package io.github.zekerzhayard.forgewrapper.installer;

import java.io.File;
import java.lang.reflect.Method;

import io.github.zekerzhayard.forgewrapper.installer.util.AbstractInstaller;
import io.github.zekerzhayard.forgewrapper.installer.util.InstallerV0;
import io.github.zekerzhayard.forgewrapper.installer.util.InstallerV1;
import net.minecraftforge.installer.actions.ProgressCallback;
import net.minecraftforge.installer.json.Install;
import net.minecraftforge.installer.json.Util;

public class Installer {
    public static boolean install(File libraryDir, File minecraftJar, File installerJar, int installerSpec) {
        AbstractInstaller installer = getInstaller(installerSpec);
        ProgressCallback monitor = ProgressCallback.withOutputs(System.out);
        Install profile = installer.loadInstallProfile();
        if (System.getProperty("java.net.preferIPv4Stack") == null) {
            System.setProperty("java.net.preferIPv4Stack", "true");
        }
        String vendor = System.getProperty("java.vendor", "missing vendor");
        String javaVersion = System.getProperty("java.version", "missing java version");
        String jvmVersion = System.getProperty("java.vm.version", "missing jvm version");
        monitor.message(String.format("JVM info: %s - %s - %s", vendor, javaVersion, jvmVersion));
        monitor.message("java.net.preferIPv4Stack=" + System.getProperty("java.net.preferIPv4Stack"));
        return installer.runClientInstall(profile, monitor, libraryDir, minecraftJar, installerJar);
    }

    private static AbstractInstaller getInstaller(int installerSpec) {
        switch (installerSpec) {
            case 0: {
                Boolean isV1 = false;
                Method[] methods = Util.class.getDeclaredMethods();
                for (Method method: methods) {
                    String methodName = method.toString();
                    if (methodName.contains("InstallV1") && methodName.contains("loadInstallProfile")) {
                        isV1 = true;
                        break;
                    }
                }
                return isV1 ? new InstallerV1() : new InstallerV0();
            }
            case 1: return new InstallerV1();
            default: throw new IllegalArgumentException("Invalid installer profile spec: " + installerSpec);
        }
    }
}
