package io.github.zekerzhayard.forgewrapper.installer;

import java.io.File;
import java.util.function.Predicate;

import net.minecraftforge.installer.actions.ClientInstall;
import net.minecraftforge.installer.actions.ProgressCallback;
import net.minecraftforge.installer.json.Install;

public class ClientInstall4MultiMC extends ClientInstall {
    public ClientInstall4MultiMC(Install profile, ProgressCallback monitor) {
        super(profile, monitor);
    }

    @Override
    public boolean run(File target, Predicate<String> optionals) {
        File librariesDir = Main.getLibrariesDir();
        File clientTarget = new File(String.format("%s/com/mojang/minecraft/%s/minecraft-%s-client.jar", librariesDir.getAbsolutePath(), this.profile.getMinecraft(), this.profile.getMinecraft()));
        return this.processors.process(librariesDir, clientTarget);
    }
}
