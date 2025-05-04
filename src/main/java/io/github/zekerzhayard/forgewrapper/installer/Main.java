package io.github.zekerzhayard.forgewrapper.installer;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cpw.mods.modlauncher.Launcher;
import io.github.zekerzhayard.forgewrapper.installer.detector.DetectorLoader;
import io.github.zekerzhayard.forgewrapper.installer.detector.IFileDetector;
import io.github.zekerzhayard.forgewrapper.installer.util.ModuleUtil;

public class Main {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Throwable {
        // --fml.neoForgeVersion 20.2.20-beta --fml.fmlVersion 1.0.2 --fml.mcVersion 1.20.2 --fml.neoFormVersion 20231019.002635 --launchTarget forgeclient

        List<String> argsList = Stream.of(args).collect(Collectors.toList());
        // NOTE: this is only true for NeoForge versions past 20.2.x
        // early versions of NeoForge (for 1.20.1) are not supposed to be covered here
        boolean isNeoForge = argsList.contains("--fml.neoForgeVersion");
        boolean skipHashCheck = argsList.contains("--skipHashCheck");

        String mcVersion = argsList.get(argsList.indexOf("--fml.mcVersion") + 1);
        String forgeGroup = argsList.contains("--fml.forgeGroup") ? argsList.get(argsList.indexOf("--fml.forgeGroup") + 1) : "net.neoforged";
        String forgeArtifact = isNeoForge ? "neoforge" : "forge";
        String forgeVersionKey = isNeoForge ? "--fml.neoForgeVersion" : "--fml.forgeVersion";
        String forgeVersion = argsList.get(argsList.indexOf(forgeVersionKey) + 1);
        String forgeFullVersion = isNeoForge ? forgeVersion : mcVersion + "-" + forgeVersion;

        IFileDetector detector = DetectorLoader.loadDetector();
        // Check installer jar.
        Path installerJar = detector.getInstallerJar(forgeGroup, forgeArtifact, forgeFullVersion);
        if (!isFile(installerJar)) {
            throw new RuntimeException("Unable to detect the forge installer!");
        }

        // Check vanilla Minecraft jar.
        Path minecraftJar = detector.getMinecraftJar(mcVersion);
        if (!isFile(minecraftJar)) {
            throw new RuntimeException("Unable to detect the Minecraft jar!");
        }

        try (URLClassLoader ucl = URLClassLoader.newInstance(new URL[] {
            Main.class.getProtectionDomain().getCodeSource().getLocation(),
            Launcher.class.getProtectionDomain().getCodeSource().getLocation(),
            installerJar.toUri().toURL()
        }, ModuleUtil.getPlatformClassLoader())) {
            Class<?> installer = ucl.loadClass("io.github.zekerzhayard.forgewrapper.installer.Installer");

            Map<String, Object> data = (Map<String, Object>) installer.getMethod("getData", File.class, boolean.class).invoke(null, detector.getLibraryDir().toFile(), skipHashCheck);
            try {
                Bootstrap.bootstrap((String[]) data.get("jvmArgs"), detector.getMinecraftJar(mcVersion).getFileName().toString(), detector.getLibraryDir().toAbsolutePath().toString());
            } catch (Throwable t) {
                // Avoid this bunch of hacks that nuke the whole wrapper.
                t.printStackTrace();
            }

            if (!((boolean) installer.getMethod("install", File.class, File.class, File.class).invoke(null, detector.getLibraryDir().toFile(), minecraftJar.toFile(), installerJar.toFile()))) {
                return;
            }

            ModuleUtil.setupClassPath(detector.getLibraryDir(), (List<String>) data.get("extraLibraries"));
            Class<?> mainClass = ModuleUtil.setupBootstrapLauncher(Class.forName((String) data.get("mainClass")));
            mainClass.getMethod("main", String[].class).invoke(null, new Object[] {args});
        }
    }

    private static boolean isFile(Path path) {
        return path != null && Files.isRegularFile(path);
    }
}
