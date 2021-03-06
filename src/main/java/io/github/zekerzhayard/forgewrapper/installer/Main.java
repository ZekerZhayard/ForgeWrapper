package io.github.zekerzhayard.forgewrapper.installer;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cpw.mods.modlauncher.Launcher;
import io.github.zekerzhayard.forgewrapper.installer.detector.DetectorLoader;
import io.github.zekerzhayard.forgewrapper.installer.detector.IFileDetector;

public class Main {
    public static void main(String[] args) throws Exception {
        List<String> argsList = Stream.of(args).collect(Collectors.toList());
        String mcVersion = argsList.get(argsList.indexOf("--fml.mcVersion") + 1);
        String forgeFullVersion = mcVersion + "-" + argsList.get(argsList.indexOf("--fml.forgeVersion") + 1);

        IFileDetector detector = DetectorLoader.loadDetector();
        if (!detector.checkExtraFiles(forgeFullVersion)) {
            System.out.println("Some extra libraries are missing! Run the installer to generate them now.");

            // Check installer jar.
            Path installerJar = detector.getInstallerJar(forgeFullVersion);
            if (!IFileDetector.isFile(installerJar)) {
                throw new RuntimeException("Can't detect the forge installer!");
            }

            // Check vanilla Minecraft jar.
            Path minecraftJar = detector.getMinecraftJar(mcVersion);
            if (!IFileDetector.isFile(minecraftJar)) {
                throw new RuntimeException("Can't detect the Minecraft jar!");
            }

            try (URLClassLoader ucl = URLClassLoader.newInstance(new URL[] {
                Main.class.getProtectionDomain().getCodeSource().getLocation(),
                Launcher.class.getProtectionDomain().getCodeSource().getLocation(),
                installerJar.toUri().toURL()
            }, getParentClassLoader())) {
                Class<?> installer = ucl.loadClass("io.github.zekerzhayard.forgewrapper.installer.Installer");
                if (!(boolean) installer.getMethod("install", File.class, File.class).invoke(null, detector.getLibraryDir().toFile(), minecraftJar.toFile())) {
                    return;
                }
            }
        }

        Launcher.main(args);
    }

    // https://github.com/MinecraftForge/Installer/blob/fe18a164b5ebb15b5f8f33f6a149cc224f446dc2/src/main/java/net/minecraftforge/installer/actions/PostProcessors.java#L287-L303
    private static ClassLoader getParentClassLoader() {
        if (!System.getProperty("java.version").startsWith("1.")) {
            try {
                return (ClassLoader) ClassLoader.class.getDeclaredMethod("getPlatformClassLoader").invoke(null);
            } catch (Exception e) {
                System.out.println("No platform classloader: " + System.getProperty("java.version"));
            }
        }
        return null;
    }
}
