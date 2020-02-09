package io.github.zekerzhayard.forgewrapper.converter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class Converter {
    public static void convert(Path installerPath, Path targetDir) throws Exception {
        JsonObject installer = getInstallerJson(installerPath);

        ArrayList<String> arguments = new ArrayList<>();

        getElement(installer.getAsJsonObject("arguments"), "game").getAsJsonArray().iterator().forEachRemaining(je -> arguments.add(je.getAsString()));
        String mcVersion = arguments.get(arguments.indexOf("--fml.mcVersion") + 1);
        String forgeVersion = arguments.get(arguments.indexOf("--fml.forgeVersion") + 1);
        String forgeFullVersion = "forge-" + mcVersion + "-" + forgeVersion;
        StringBuilder wrapperVersion = new StringBuilder();

        JsonObject pack = convertPackJson(mcVersion);
        JsonObject patches = convertPatchesJson(installer, mcVersion, forgeVersion, wrapperVersion);

        Files.createDirectories(targetDir);

        // Copy mmc-pack.json and instance.cfg to <instance> folder.
        Path instancePath = targetDir.resolve(forgeFullVersion);
        Files.createDirectories(instancePath);
        Files.copy(new ByteArrayInputStream(pack.toString().getBytes(StandardCharsets.UTF_8)), instancePath.resolve("mmc-pack.json"), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(new ByteArrayInputStream(("InstanceType=OneSix\nname=" + forgeFullVersion).getBytes(StandardCharsets.UTF_8)), instancePath.resolve("instance.cfg"), StandardCopyOption.REPLACE_EXISTING);

        // Copy ForgeWrapper to <instance>/libraries folder.
        Path librariesPath = instancePath.resolve("libraries");
        Files.createDirectories(librariesPath);
        Files.copy(Paths.get(Converter.class.getProtectionDomain().getCodeSource().getLocation().toURI()), librariesPath.resolve(wrapperVersion.toString()), StandardCopyOption.REPLACE_EXISTING);

        // Copy net.minecraftforge.json to <instance>/patches folder.
        Path patchesPath = instancePath.resolve("patches");
        Files.createDirectories(patchesPath);
        Files.copy(new ByteArrayInputStream(patches.toString().getBytes(StandardCharsets.UTF_8)), patchesPath.resolve("net.minecraftforge.json"), StandardCopyOption.REPLACE_EXISTING);

        // Copy forge installer to <instance>/.minecraft/.forgewrapper folder.
        Path forgeWrapperPath = instancePath.resolve(".minecraft").resolve(".forgewrapper");
        Files.createDirectories(forgeWrapperPath);
        Files.copy(installerPath, forgeWrapperPath.resolve(forgeFullVersion + "-installer.jar"), StandardCopyOption.REPLACE_EXISTING);
    }

    private static JsonObject getInstallerJson(Path installerPath) {
        try {
            ZipFile zf = new ZipFile(installerPath.toFile());
            ZipEntry versionFile = zf.getEntry("version.json");
            if (versionFile == null) {
                throw new RuntimeException("The forge installer is invalid!");
            }
            InputStreamReader isr = new InputStreamReader(zf.getInputStream(versionFile), StandardCharsets.UTF_8);
            return new JsonParser().parse(isr).getAsJsonObject();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    // Convert mmc-pack.json:
    //   - Replace Minecraft version
    private static JsonObject convertPackJson(String mcVersion) {
        JsonObject pack = new JsonParser().parse(new InputStreamReader(Converter.class.getResourceAsStream("/mmc-pack.json"))).getAsJsonObject();

        for (JsonElement component : getElement(pack, "components").getAsJsonArray()) {
            JsonObject componentObject = component.getAsJsonObject();
            JsonElement version = getElement(componentObject, "version");
            if (!version.isJsonNull() && getElement(componentObject, "uid").getAsString().equals("net.minecraft")) {
                componentObject.addProperty("version", mcVersion);
            }
        }
        return pack;
    }

    // Convert patches/net.minecraftforge.json:
    //   - Add libraries
    //   - Add forge-launcher url
    //   - Replace Minecraft & Forge versions
    private static JsonObject convertPatchesJson(JsonObject installer, String mcVersion, String forgeVersion, StringBuilder wrapperVersion) {
        JsonObject patches = new JsonParser().parse(new InputStreamReader(Converter.class.getResourceAsStream("/patches/net.minecraftforge.json"))).getAsJsonObject();
        JsonArray libraries = getElement(patches, "libraries").getAsJsonArray();

        for (JsonElement lib : libraries) {
            String name = getElement(lib.getAsJsonObject(), "name").getAsString();
            if (name.startsWith("io.github.zekerzhayard:ForgeWrapper:")) {
                wrapperVersion.append(getElement(lib.getAsJsonObject(), "MMC-filename").getAsString());
            }
        }
        for (JsonElement lib : getElement(installer ,"libraries").getAsJsonArray()) {
            JsonObject artifact = getElement(getElement(lib.getAsJsonObject(), "downloads").getAsJsonObject(), "artifact").getAsJsonObject();
            String path = getElement(artifact, "path").getAsString();
            if (path.startsWith("net/minecraftforge/forge/")) {
                artifact.getAsJsonObject().addProperty("url", "https://files.minecraftforge.net/maven/" + path.replace(".jar", "-launcher.jar"));
            }
            libraries.add(lib);
        }

        patches.addProperty("version", forgeVersion);
        for (JsonElement require : getElement(patches, "requires").getAsJsonArray()) {
            JsonObject requireObject = require.getAsJsonObject();
            if (getElement(requireObject, "uid").getAsString().equals("net.minecraft")) {
                requireObject.addProperty("equals", mcVersion);
            }
        }
        return patches;
    }

    private static JsonElement getElement(JsonObject object, String property) {
        Optional<Map.Entry<String, JsonElement>> first = object.entrySet().stream().filter(e -> e.getKey().equals(property)).findFirst();
        if (first.isPresent()) {
            return first.get().getValue();
        }
        return JsonNull.INSTANCE;
    }
}
