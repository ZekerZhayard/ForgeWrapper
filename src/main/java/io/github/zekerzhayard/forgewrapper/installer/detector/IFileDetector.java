package io.github.zekerzhayard.forgewrapper.installer.detector;

import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

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
     * @param forgeGroup Forge package group (e.g. net.minecraftforge).
     * @param forgeArtifact Forge package artifact (e.g. forge).
     * @param forgeFullVersion Forge full version (e.g. 1.14.4-28.2.0).
     * @return The forge installer jar path. It can also be defined by JVM argument "-Dforgewrapper.installer=&lt;installer-path&gt;".
     */
    default Path getInstallerJar(String forgeGroup, String forgeArtifact, String forgeFullVersion) {
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
}
