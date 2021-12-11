package io.github.zekerzhayard.forgewrapper.installer.util;

import java.io.File;

import net.minecraftforge.installer.actions.ProgressCallback;
import net.minecraftforge.installer.json.Install;

public abstract class AbstractInstaller {
    public abstract Install loadInstallProfile();

    public abstract boolean runClientInstall(Install profile, ProgressCallback monitor, File libraryDir, File minecraftJar, File installerJar);
}
