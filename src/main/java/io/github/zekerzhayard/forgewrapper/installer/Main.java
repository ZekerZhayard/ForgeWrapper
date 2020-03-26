package io.github.zekerzhayard.forgewrapper.installer;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cpw.mods.modlauncher.Launcher;
import io.github.zekerzhayard.forgewrapper.Utils;

public class Main {
    public static void main(String[] args) throws Exception {
        List<String> argsList = Stream.of(args).collect(Collectors.toList());
        String version = argsList.get(argsList.indexOf("--fml.mcVersion") + 1) + "-" + argsList.get(argsList.indexOf("--fml.forgeVersion") + 1);

        Path forgeDir = getLibrariesDir().toPath().resolve("net").resolve("minecraftforge").resolve("forge").resolve(version);
        Path clientJar = forgeDir.resolve("forge-" + version + "-client.jar");
        Path extraJar = forgeDir.resolve("forge-" + version + "-extra.jar");
        if (Files.exists(clientJar) && Files.exists(extraJar)) {
            String installerUrl = String.format("https://files.minecraftforge.net/maven/net/minecraftforge/forge/%s/forge-%s-installer.jar", version, version);
            String installerFileStr = String.format("./.forgewrapper/forge-%s-installer.jar", version);
            Utils.download(installerUrl, installerFileStr);

            URLClassLoader ucl = URLClassLoader.newInstance(new URL[] {
                Main.class.getProtectionDomain().getCodeSource().getLocation(),
                Launcher.class.getProtectionDomain().getCodeSource().getLocation(),
                new File(installerFileStr).toURI().toURL()
            }, null);

            Class<?> installer = ucl.loadClass("io.github.zekerzhayard.forgewrapper.installer.Installer");
            if (!(boolean) installer.getMethod("install").invoke(null)) {
                return;
            }
        }

        Launcher.main(args);
    }

    public static File getLibrariesDir() {
        try {
            File laucnher = new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            // see https://github.com/MinecraftForge/MinecraftForge/blob/863ab2ca184cf2e2dfa134d07bfc20d6a9a6a4e8/src/main/java/net/minecraftforge/fml/relauncher/libraries/LibraryManager.java#L151
            //              /<version>      /modlauncher    /mods           /cpw            /libraries
            return laucnher.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
