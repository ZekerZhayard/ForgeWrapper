package io.github.zekerzhayard.forgewrapper.installer.detector;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import cpw.mods.modlauncher.Launcher;

public interface IFileDetector {
    /**
     * @return The name of the detector.
     */
    String name();

    /**
     * If there are two or more detectors are enabled, an exception will be thrown. Removing anything from the map is in vain.
     * @param others Other detectors.
     * @return True represents enabled.
     */
    boolean enabled(HashMap<String, IFileDetector> others);

    /**
     * @return The ".minecraft/libraries" folder for normal. It can also be defined by JVM argument "-Dforgewrapper.librariesDir=&lt;libraries-path&gt;".
     */
    default Path getLibraryDir() {
        String libraryDir = System.getProperty("forgewrapper.librariesDir");
        if (libraryDir != null) {
            return Paths.get(libraryDir).toAbsolutePath();
        }
        try {
            Path launcher = Paths.get(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI()).toAbsolutePath();
            //              /<version>  /modlauncher/mods       /cpw        /libraries
            return launcher.getParent().getParent().getParent().getParent().getParent().toAbsolutePath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @param forgeFullVersion Forge full version (e.g. 1.14.4-28.2.0).
     * @return The forge installer jar path. It can also be defined by JVM argument "-Dforgewrapper.installer=&lt;installer-path&gt;".
     */
    default Path getInstallerJar(String forgeFullVersion) {
        String installer = System.getProperty("forgewrapper.installer");
        if (installer != null) {
            return Paths.get(installer).toAbsolutePath();
        }
        return null;
    }

    /**
     * @param mcVersion Minecraft version (e.g. 1.14.4).
     * @return The minecraft client jar path. It can also be defined by JVM argument "-Dforgewrapper.minecraft=&lt;minecraft-path&gt;".
     */
    default Path getMinecraftJar(String mcVersion) {
        String minecraft = System.getProperty("forgewrapper.minecraft");
        if (minecraft != null) {
            return Paths.get(minecraft).toAbsolutePath();
        }
        return null;
    }

    /**
     * @param forgeFullVersion Forge full version (e.g. 1.14.4-28.2.0).
     * @return The json object in the-installer-jar-->install_profile.json-->data-->xxx-->client.
     */
    default JsonObject getInstallProfileExtraData(String forgeFullVersion) {
        Path installer = this.getInstallerJar(forgeFullVersion);
        if (isFile(installer)) {
            try (ZipFile zf = new ZipFile(installer.toFile())) {
                ZipEntry ze = zf.getEntry("install_profile.json");
                if (ze != null) {
                    try (
                        InputStream is = zf.getInputStream(ze);
                        InputStreamReader isr = new InputStreamReader(is, StandardCharsets.UTF_8)
                    ) {
                        for (Map.Entry<String, JsonElement> entry : new JsonParser().parse(isr).getAsJsonObject().entrySet()) {
                            if (entry.getKey().equals("data")) {
                                return entry.getValue().getAsJsonObject();
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            throw new RuntimeException("Can't detect the forge installer!");
        }
        return null;
    }

    /**
     * Check all cached files.
     * @param forgeFullVersion Forge full version (e.g. 1.14.4-28.2.0).
     * @return True represents all files are ready.
     */
    default boolean checkExtraFiles(String forgeFullVersion) {
        JsonObject jo = this.getInstallProfileExtraData(forgeFullVersion);
        if (jo != null) {
            Map<String, Path> libsMap = new HashMap<>();
            Map<String, String> hashMap = new HashMap<>();

            // Get all "data/<name>/client" elements.
            for (Map.Entry<String, JsonElement> entry : jo.entrySet()) {
                String clientStr = getElement(entry.getValue().getAsJsonObject(), "client").getAsString();
                if (entry.getKey().endsWith("_SHA")) {
                    Pattern p = Pattern.compile("^'(?<sha1>[A-Za-z0-9]{40})'$");
                    Matcher m = p.matcher(clientStr);
                    if (m.find()) {
                        hashMap.put(entry.getKey(), m.group("sha1"));
                    }
                } else {
                    Pattern p = Pattern.compile("^\\[(?<groupId>[^:]*):(?<artifactId>[^:]*):(?<version>[^:@]*)(:(?<prefix>[^@]*))?(@(?<type>[^]]*))?]$");
                    Matcher m = p.matcher(clientStr);
                    if (m.find()) {
                        String groupId = nullToDefault(m.group("groupId"), "");
                        String artifactId = nullToDefault(m.group("artifactId"), "");
                        String version = nullToDefault(m.group("version"), "");
                        String prefix = nullToDefault(m.group("prefix"), "");
                        String type = nullToDefault(m.group("type"), "jar");
                        libsMap.put(entry.getKey(), this.getLibraryDir()
                            .resolve(groupId.replace('.', File.separatorChar))
                            .resolve(artifactId)
                            .resolve(version)
                            .resolve(artifactId + "-" + version + (prefix.equals("") ? "" : "-") + prefix + "." + type).toAbsolutePath());
                    }
                }
            }

            // Check all cached libraries.
            boolean checked = true;
            for (Map.Entry<String, Path> entry : libsMap.entrySet()) {
                checked = checked && this.checkExtraFile(entry.getValue(), hashMap.get(entry.getKey() + "_SHA"));
            }
            return checked;
        }
        // Skip installing process if installer profile doesn't exist.
        return true;
    }

    /**
     * Check the exact file.
     * @param path The path of the file to check.
     * @param sha1 The sha1 defined in installer.
     * @return True represents the file is ready.
     */
    default boolean checkExtraFile(Path path, String sha1) {
        return Files.isRegularFile(path) && (sha1 == null || sha1.equals("") || sha1.toLowerCase(Locale.ENGLISH).equals(getFileSHA1(path)));
    }

    static boolean isFile(Path path) {
        return path != null && Files.isRegularFile(path);
    }

    static JsonElement getElement(JsonObject object, String property) {
        Optional<Map.Entry<String, JsonElement>> first = object.entrySet().stream().filter(e -> e.getKey().equals(property)).findFirst();
        if (first.isPresent()) {
            return first.get().getValue();
        }
        return JsonNull.INSTANCE;
    }

    static String getFileSHA1(Path path) {
        try {
            StringBuilder sha1 = new StringBuilder(new BigInteger(1, MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(path))).toString(16));
            while (sha1.length() < 40) {
                sha1.insert(0, "0");
            }
            return sha1.toString().toLowerCase(Locale.ENGLISH);
        } catch (IOException | NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }

    static String nullToDefault(String string, String defaultValue) {
        return string == null ? defaultValue : string;
    }
}
