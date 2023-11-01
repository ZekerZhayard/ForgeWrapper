package io.github.zekerzhayard.forgewrapper.installer.detector;

import java.nio.file.Path;
import java.util.HashMap;

public class MultiMCFileDetector implements IFileDetector {
    protected Path libraryDir = null;
    protected Path installerJar = null;
    protected Path minecraftJar = null;

    @Override
    public String name() {
        return "MultiMC";
    }

    @Override
    public boolean enabled(HashMap<String, IFileDetector> others) {
        return others.size() == 0;
    }

    @Override
    public Path getLibraryDir() {
        if (this.libraryDir == null) {
            this.libraryDir = IFileDetector.super.getLibraryDir();
        }
        return this.libraryDir;
    }

    @Override
    public Path getInstallerJar(String forgeGroup, String forgeArtifact, String forgeFullVersion) {
        Path path = IFileDetector.super.getInstallerJar(forgeGroup, forgeArtifact, forgeFullVersion);
        if (path == null) {
            if (this.installerJar == null) {
                Path installerBase = this.getLibraryDir();
                for (String dir : forgeGroup.split("\\."))
                    installerBase = installerBase.resolve(dir);
                this.installerJar = installerBase.resolve(forgeArtifact).resolve(forgeFullVersion).resolve(forgeArtifact + "-" + forgeFullVersion + "-installer.jar").toAbsolutePath();
            }
            return this.installerJar;
        }
        return path;
    }

    @Override
    public Path getMinecraftJar(String mcVersion) {
        Path path = IFileDetector.super.getMinecraftJar(mcVersion);
        if (path == null) {
            return this.minecraftJar != null ? this.minecraftJar : (this.minecraftJar = this.getLibraryDir().resolve("com").resolve("mojang").resolve("minecraft").resolve(mcVersion).resolve("minecraft-" + mcVersion + "-client.jar").toAbsolutePath());
        }
        return path;
    }
}
