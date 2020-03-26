package io.github.zekerzhayard.forgewrapper.installer;

import java.io.File;
import java.net.URISyntaxException;
import java.util.function.Predicate;

import cpw.mods.modlauncher.Launcher;
import net.minecraftforge.installer.actions.ActionCanceledException;
import net.minecraftforge.installer.actions.ClientInstall;
import net.minecraftforge.installer.actions.ProgressCallback;
import net.minecraftforge.installer.json.Install;

public class ClientInstall4MultiMC extends ClientInstall {
    public ClientInstall4MultiMC(Install profile, ProgressCallback monitor) {
        super(profile, monitor);
    }

    @Override
    public boolean run(File target, Predicate<String> optionals) {
        File librariesDir;
        try {
            File laucnher = new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            // see https://github.com/MinecraftForge/MinecraftForge/blob/863ab2ca184cf2e2dfa134d07bfc20d6a9a6a4e8/src/main/java/net/minecraftforge/fml/relauncher/libraries/LibraryManager.java#L151
            //                     /<version>      /modlauncher    /mods           /cpw            /libraries
            librariesDir = laucnher.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
        File clientTarget = new File(String.format("%s/com/mojang/minecraft/%s/minecraft-%s-client.jar", librariesDir.getAbsolutePath(), this.profile.getMinecraft(), this.profile.getMinecraft()));

        boolean downloadLibraries = true; // Force true when without an internet connection.
        try {
            downloadLibraries = this.downloadLibraries(librariesDir, optionals);
        } catch (ActionCanceledException e) {
            e.printStackTrace();
        }
        if (!downloadLibraries) {
            return false;
        }
        return this.processors.process(librariesDir, clientTarget);
    }
}
