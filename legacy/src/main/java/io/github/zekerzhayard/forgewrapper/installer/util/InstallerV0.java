package io.github.zekerzhayard.forgewrapper.installer.util;

import java.io.File;
import java.util.function.Predicate;

import net.minecraftforge.installer.actions.ClientInstall;
import net.minecraftforge.installer.actions.ProgressCallback;
import net.minecraftforge.installer.json.Install;
import net.minecraftforge.installer.json.Util;

public class InstallerV0 extends AbstractInstaller {
    @Override
    public Install loadInstallProfile() {
        return Util.loadInstallProfile();
    }

    @Override
    public boolean runClientInstall(Install profile, ProgressCallback monitor, File libraryDir, File minecraftJar, File installerJar) {
        return new ClientInstall4MultiMC(profile, monitor, libraryDir, minecraftJar).run(null, input -> true);
    }

    public static class ClientInstall4MultiMC extends ClientInstall {
        protected File libraryDir;
        protected File minecraftJar;

        public ClientInstall4MultiMC(Install profile, ProgressCallback monitor, File libraryDir, File minecraftJar) {
            super(profile, monitor);
            this.libraryDir = libraryDir;
            this.minecraftJar = minecraftJar;
        }

        @Override
        public boolean run(File target, Predicate<String> optionals) {
            return this.processors.process(this.libraryDir, this.minecraftJar);
        }
    }
}
