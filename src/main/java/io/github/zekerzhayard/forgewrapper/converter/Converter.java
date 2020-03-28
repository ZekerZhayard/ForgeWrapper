package io.github.zekerzhayard.forgewrapper.converter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
    public static void convert(Path installerPath, Path targetDir, Path multimcDir) throws Exception {
        JsonObject installer = getJsonFromZip(installerPath, "version.json");
        JsonObject installProfile = getJsonFromZip(installerPath, "install_profile.json");
        List<String> arguments = getAdditionalArgs(installer);
        String mcVersion = arguments.get(arguments.indexOf("--fml.mcVersion") + 1);
        String forgeVersion = arguments.get(arguments.indexOf("--fml.forgeVersion") + 1);
        String forgeFullVersion = "forge-" + mcVersion + "-" + forgeVersion;
        StringBuilder wrapperVersion = new StringBuilder();

        JsonObject pack = convertPackJson(mcVersion);
        JsonObject patches = convertPatchesJson(installer, installProfile, mcVersion, forgeVersion, wrapperVersion);

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

        // Copy forge installer to MultiMC/libraries/net/minecraftforge/forge/<mcVersion>-<forgeVersion> folder.
        if (multimcDir != null) {
            Path targetInstallerPath = multimcDir.resolve("libraries").resolve("net").resolve("minecraftforge").resolve("forge").resolve(forgeVersion);
            Files.createDirectories(targetInstallerPath);
            Files.copy(installerPath, targetInstallerPath.resolve(forgeFullVersion + "-installer.jar"), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static List<String> getAdditionalArgs(JsonObject installer) {
        List<String> args = new ArrayList<>();
        getElement(installer.getAsJsonObject("arguments"), "game").getAsJsonArray().iterator().forEachRemaining(je -> args.add(je.getAsString()));
        return args;
    }

    public static JsonObject getJsonFromZip(Path path, String json) {
        try {
            ZipFile zf = new ZipFile(path.toFile());
            ZipEntry versionFile = zf.getEntry(json);
            if (versionFile == null) {
                throw new RuntimeException("The zip file is invalid!");
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
    private static JsonObject convertPatchesJson(JsonObject installer, JsonObject installProfile, String mcVersion, String forgeVersion, StringBuilder wrapperVersion) {
        JsonObject patches = new JsonParser().parse(new InputStreamReader(Converter.class.getResourceAsStream("/patches/net.minecraftforge.json"))).getAsJsonObject();
        JsonArray mavenFiles = getElement(patches, "mavenFiles").getAsJsonArray();
        JsonArray libraries = getElement(patches, "libraries").getAsJsonArray();

        String minecraftArguments = String.join(" ", getElement(patches, "minecraftArguments").getAsString(), "--username ${auth_player_name} --version ${version_name} --gameDir ${game_directory} --assetsDir ${assets_root} --assetIndex ${assets_index_name} --uuid ${auth_uuid} --accessToken ${auth_access_token} --userType ${user_type} --versionType ${version_type}", String.join(" ", getAdditionalArgs(installer))).trim();
        patches.addProperty("minecraftArguments", minecraftArguments);

        for (JsonElement mavenFile : mavenFiles) {
            String name = getElement(mavenFile.getAsJsonObject(), "name").getAsString();
            mavenFile.getAsJsonObject().addProperty("name", name.replace("{VERSION}", mcVersion).replace("{FORGE_VERSION}", forgeVersion));
        }
        for (JsonElement lib : libraries) {
            String name = getElement(lib.getAsJsonObject(), "name").getAsString();
            if (name.startsWith("io.github.zekerzhayard:ForgeWrapper:")) {
                wrapperVersion.append(getElement(lib.getAsJsonObject(), "MMC-filename").getAsString());
            }
        }
        Map<String, String> additionalUrls = new HashMap<>();
        String path = String.format("net/minecraftforge/forge/%s-%s/forge-%s-%s", mcVersion, forgeVersion, mcVersion, forgeVersion);
        additionalUrls.put(path + "-universal.jar", "https://files.minecraftforge.net/maven/" + path + "-universal.jar");
        transformLibraries(getElement(installProfile, "libraries").getAsJsonArray(), mavenFiles, additionalUrls);
        additionalUrls.clear();
        additionalUrls.put(path + ".jar", "https://files.minecraftforge.net/maven/" + path + "-launcher.jar");
        transformLibraries(getElement(installer ,"libraries").getAsJsonArray(), libraries, additionalUrls);

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

    private static void transformLibraries(JsonArray source, JsonArray target, Map<String, String> additionalUrls) {
        for (JsonElement lib : source) {
            JsonObject artifact = getElement(getElement(lib.getAsJsonObject(), "downloads").getAsJsonObject(), "artifact").getAsJsonObject();
            String path = getElement(artifact, "path").getAsString();
            if (additionalUrls.containsKey(path)) {
                artifact.getAsJsonObject().addProperty("url", additionalUrls.get(path));
            }
            target.add(lib);
        }
    }
}
