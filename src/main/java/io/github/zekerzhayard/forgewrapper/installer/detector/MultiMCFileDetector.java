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
    public Path getInstallerJar(String forgeFullVersion) {
        Path path = IFileDetector.super.getInstallerJar(forgeFullVersion);
        if (path == null) {
            return this.installerJar != null ? this.installerJar : (this.installerJar = this.getLibraryDir().resolve("net").resolve("minecraftforge").resolve("forge").resolve(forgeFullVersion).resolve("forge-" + forgeFullVersion + "-installer.jar").toAbsolutePath());
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
