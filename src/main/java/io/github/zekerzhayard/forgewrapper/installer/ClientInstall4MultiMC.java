package io.github.zekerzhayard.forgewrapper.installer;

import java.io.File;
import java.net.URISyntaxException;
import java.util.function.Predicate;

import io.github.zekerzhayard.forgewrapper.Utils;
import net.minecraftforge.installer.actions.ActionCanceledException;
import net.minecraftforge.installer.actions.ClientInstall;
import net.minecraftforge.installer.actions.ProgressCallback;
import net.minecraftforge.installer.json.Install;

public class ClientInstall4MultiMC extends ClientInstall {
    public ClientInstall4MultiMC(Install profile, ProgressCallback monitor) {
        super(profile, monitor);
    }

    @Override
    public boolean run(File target, Predicate<String> optionals) throws ActionCanceledException {
        try {
            File librariesDir = Utils.getLibrariesDir();
            File clientTarget = new File(String.format("%s/com/mojang/minecraft/%s/minecraft-%s-client.jar", librariesDir.getAbsolutePath(), this.profile.getMinecraft(), this.profile.getMinecraft()));

            if (!this.downloadLibraries(librariesDir, optionals)) {
                return false;
            }
            if (!this.processors.process(librariesDir, clientTarget)) {
                return false;
            }
        } catch (URISyntaxException e) {
            return false;
        }
        return true;
    }
}
