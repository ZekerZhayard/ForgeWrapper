package io.github.zekerzhayard.forgewrapper.installer;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cpw.mods.modlauncher.Launcher;
import io.github.zekerzhayard.forgewrapper.installer.detector.DetectorLoader;
import io.github.zekerzhayard.forgewrapper.installer.detector.IFileDetector;
import io.github.zekerzhayard.forgewrapper.installer.util.ModuleUtil;

public class Main {
    public static void main(String[] args) throws Throwable {
        List<String> argsList = Stream.of(args).collect(Collectors.toList());
        String mcVersion = argsList.get(argsList.indexOf("--fml.mcVersion") + 1);
        String forgeGroup = argsList.contains("--fml.forgeGroup") ? argsList.get(argsList.indexOf("--fml.forgeGroup") + 1) : "net.neoforged";
        String forgeVersion = argsList.get(argsList.indexOf("--fml.forgeVersion") + 1);
        String forgeFullVersion = mcVersion + "-" + forgeVersion;

        IFileDetector detector = DetectorLoader.loadDetector();
        try {
            Bootstrap.bootstrap(detector.getJvmArgs(forgeGroup, forgeFullVersion), detector.getMinecraftJar(mcVersion).getFileName().toString(), detector.getLibraryDir().toAbsolutePath().toString());
        } catch (Throwable ignored) {
            // Avoid this bunch of hacks that nuke the whole wrapper.
        }
        if (!detector.checkExtraFiles(forgeGroup, forgeFullVersion)) {
            System.out.println("Some extra libraries are missing! Running the installer to generate them now.");

            // Check installer jar.
            Path installerJar = detector.getInstallerJar(forgeGroup, forgeFullVersion);
            if (!IFileDetector.isFile(installerJar)) {
                throw new RuntimeException("Unable to detect the forge installer!");
            }

            // Check vanilla Minecraft jar.
            Path minecraftJar = detector.getMinecraftJar(mcVersion);
            if (!IFileDetector.isFile(minecraftJar)) {
                throw new RuntimeException("Unable to detect the Minecraft jar!");
            }

            try (URLClassLoader ucl = URLClassLoader.newInstance(new URL[] {
                Main.class.getProtectionDomain().getCodeSource().getLocation(),
                Launcher.class.getProtectionDomain().getCodeSource().getLocation(),
                installerJar.toUri().toURL()
            }, ModuleUtil.getPlatformClassLoader())) {
                Class<?> installer = ucl.loadClass("io.github.zekerzhayard.forgewrapper.installer.Installer");
                if (!(boolean) installer.getMethod("install", File.class, File.class, File.class).invoke(null, detector.getLibraryDir().toFile(), minecraftJar.toFile(), installerJar.toFile())) {
                    return;
                }
            }
        }

        Class<?> mainClass = ModuleUtil.setupBootstrapLauncher(Class.forName(detector.getMainClass(forgeGroup, forgeFullVersion)));
        mainClass.getMethod("main", String[].class).invoke(null, new Object[] { args });
    }
}
