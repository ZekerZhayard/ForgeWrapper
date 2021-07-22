package io.github.zekerzhayard.forgewrapper.installer.util;

import net.minecraftforge.installer.json.Install;
import net.minecraftforge.installer.json.Util;

// to compatible with forge [1.13.2-25.0.9,1.16.5-36.1.65]
public class LegacyInstallerUtil {
    public static Install loadInstallProfile() {
        return Util.loadInstallProfile();
    }
}
