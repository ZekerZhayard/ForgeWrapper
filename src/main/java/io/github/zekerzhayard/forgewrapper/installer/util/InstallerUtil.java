package io.github.zekerzhayard.forgewrapper.installer.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.lang.reflect.Method;

import io.github.zekerzhayard.forgewrapper.installer.ClientInstall4MultiMC;
import io.github.zekerzhayard.forgewrapper.installer.LegacyClientInstall4MultiMC;
import net.minecraftforge.installer.actions.ProgressCallback;
import net.minecraftforge.installer.json.Install;
import net.minecraftforge.installer.json.Util;

public class InstallerUtil {
    public static Install loadInstallProfile(String _forgeVersion) {
        if (isLegacyForge()) {
            return LegacyInstallerUtil.loadInstallProfile();
        } else {
            // to prevent ClassNotFoundException
            return new Object() {
                public Install get() {
                    return Util.loadInstallProfile();
                }
            }.get();
        }
    }

    public static boolean runClientInstall(String _forgeVersion, Install profile, ProgressCallback monitor, File libraryDir, File minecraftJar, File installerJar) {
        if (isLegacyForge()) {
            return new LegacyClientInstall4MultiMC(profile, monitor, libraryDir, minecraftJar).run(null, input -> true);
        } else {
            return new ClientInstall4MultiMC(profile, monitor, libraryDir, minecraftJar).run(null, input -> true, installerJar);
        }
    }

    private final static String V0Method = "public static net.minecraftforge.installer.json.Install net.minecraftforge.installer.json.Util.loadInstallProfile()";
    private final static String V1Method = "public static net.minecraftforge.installer.json.InstallV1 net.minecraftforge.installer.json.Util.loadInstallProfile()";
    private static boolean isLegacyForge() {
        Method m[] = net.minecraftforge.installer.json.Util.class.getMethods();
        int found = 0;
        boolean isLegacy = false;
        List<String> methodList = new ArrayList<>();
        for(int i = 0; i < m.length; i++) {
            String classString = m[i].toString();
            if(classString.equals(V1Method)) {
                found++;
            }
            else if(classString.equals(V0Method)) {
                isLegacy = true;
                found++;
            }
            methodList.add("Method " + i + " " + classString);
        }
        if(found != 1) {
            String result = String.join("\n", methodList);
            System.err.println("Seen following methods:");
            System.err.println(result);
            throw new RuntimeException("Unhandled Forge installer type!");
        }
        return isLegacy;
    }

    private static boolean compareVersion(Matcher m0, Matcher m1, int index, String... groups) {
        if (index == groups.length) return true; // the same as the legacy version
        int result = Integer.compare(Integer.parseInt(m0.group(groups[index])), Integer.parseInt(m1.group(groups[index])));
        if (result < 0) return true; // less than the legacy version
        if (result > 0) return false; // greater than the legacy version
        return compareVersion(m0, m1, index + 1, groups);
    }
}
