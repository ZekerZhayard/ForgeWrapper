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

public class Main {
    public static void main(String[] args) throws Exception {
        List<String> argsList = Stream.of(args).collect(Collectors.toList());
        String mcVersion = argsList.get(argsList.indexOf("--fml.mcVersion") + 1);
        String mcpFullVersion = mcVersion + "-" + argsList.get(argsList.indexOf("--fml.mcpVersion") + 1);
        String forgeFullVersion = mcVersion + "-" + argsList.get(argsList.indexOf("--fml.forgeVersion") + 1);

        Path librariesDir = getLibrariesDir().toPath();
        Path minecraftDir = librariesDir.resolve("net").resolve("minecraft").resolve("client");
        Path forgeDir = librariesDir.resolve("net").resolve("minecraftforge").resolve("forge").resolve(forgeFullVersion);
        if (getAdditionalLibraries(minecraftDir, forgeDir, mcVersion, forgeFullVersion, mcpFullVersion).anyMatch(path -> !Files.exists(path))) {
            System.out.println("Some extra libraries are missing! Run the installer to generate them now.");
            URLClassLoader ucl = URLClassLoader.newInstance(new URL[] {
                Main.class.getProtectionDomain().getCodeSource().getLocation(),
                Launcher.class.getProtectionDomain().getCodeSource().getLocation(),
                forgeDir.resolve("forge-" + forgeFullVersion + "-installer.jar").toUri().toURL()
            }, getParentClassLoader());

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
            //              /<version>      /modlauncher    /mods           /cpw            /libraries
            return laucnher.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public static Stream<Path> getAdditionalLibraries(Path minecraftDir, Path forgeDir, String mcVersion, String forgeFullVersion, String mcpFullVersion) {
        return Stream.of(
            forgeDir.resolve("forge-" + forgeFullVersion + "-client.jar"),
            minecraftDir.resolve(mcVersion).resolve("client-" + mcVersion + "-extra.jar"),
            minecraftDir.resolve(mcpFullVersion).resolve("client-" + mcpFullVersion + "-srg.jar")
        );
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
