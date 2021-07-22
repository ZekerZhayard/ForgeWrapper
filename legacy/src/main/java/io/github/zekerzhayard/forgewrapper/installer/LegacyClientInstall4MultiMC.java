package io.github.zekerzhayard.forgewrapper.installer;

import java.io.File;
import java.util.function.Predicate;

import net.minecraftforge.installer.actions.ClientInstall;
import net.minecraftforge.installer.actions.ProgressCallback;
import net.minecraftforge.installer.json.Install;

public class LegacyClientInstall4MultiMC extends ClientInstall {
    protected File libraryDir;
    protected File minecraftJar;

    public LegacyClientInstall4MultiMC(Install profile, ProgressCallback monitor, File libraryDir, File minecraftJar) {
        super(profile, monitor);
        this.libraryDir = libraryDir;
        this.minecraftJar = minecraftJar;
    }

    @Override
    public boolean run(File target, Predicate<String> optionals) {
        return this.processors.process(this.libraryDir, this.minecraftJar);
    }
}
