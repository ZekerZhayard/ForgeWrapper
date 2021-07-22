package io.github.zekerzhayard.forgewrapper.installer;

import java.io.File;

import io.github.zekerzhayard.forgewrapper.installer.util.InstallerUtil;
import net.minecraftforge.installer.actions.ProgressCallback;
import net.minecraftforge.installer.json.Install;

public class Installer {
    public static boolean install(File libraryDir, File minecraftJar, File installerJar, String forgeVersion) {
        ProgressCallback monitor = ProgressCallback.withOutputs(System.out);
        Install install = InstallerUtil.loadInstallProfile(forgeVersion);
        if (System.getProperty("java.net.preferIPv4Stack") == null) {
            System.setProperty("java.net.preferIPv4Stack", "true");
        }
        String vendor = System.getProperty("java.vendor", "missing vendor");
        String javaVersion = System.getProperty("java.version", "missing java version");
        String jvmVersion = System.getProperty("java.vm.version", "missing jvm version");
        monitor.message(String.format("JVM info: %s - %s - %s", vendor, javaVersion, jvmVersion));
        monitor.message("java.net.preferIPv4Stack=" + System.getProperty("java.net.preferIPv4Stack"));
        return InstallerUtil.runClientInstall(forgeVersion, install, monitor, libraryDir, minecraftJar, installerJar);
    }
}
