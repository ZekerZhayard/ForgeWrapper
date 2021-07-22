package io.github.zekerzhayard.forgewrapper.installer.util;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.zekerzhayard.forgewrapper.installer.ClientInstall4MultiMC;
import io.github.zekerzhayard.forgewrapper.installer.LegacyClientInstall4MultiMC;
import net.minecraftforge.installer.actions.ProgressCallback;
import net.minecraftforge.installer.json.Install;
import net.minecraftforge.installer.json.Util;

public class InstallerUtil {
    public static Install loadInstallProfile(String forgeVersion) {
        if (isLegacyForge(forgeVersion, "36.1.65")) {
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

    public static boolean runClientInstall(String forgeVersion, Install profile, ProgressCallback monitor, File libraryDir, File minecraftJar, File installerJar) {
        if (isLegacyForge(forgeVersion, "36.1.65")) {
            return new LegacyClientInstall4MultiMC(profile, monitor, libraryDir, minecraftJar).run(null, input -> true);
        } else {
            return new ClientInstall4MultiMC(profile, monitor, libraryDir, minecraftJar).run(null, input -> true, installerJar);
        }
    }

    private final static Pattern FORGE_VERSION_PATTERN = Pattern.compile("(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<build>\\d+)");
    private static boolean isLegacyForge(String forgeVersion, String legacyForgeVersion) {
        Matcher m0 = FORGE_VERSION_PATTERN.matcher(forgeVersion);
        Matcher m1 = FORGE_VERSION_PATTERN.matcher(legacyForgeVersion);
        if (m0.find() && m1.find()) {
            return compareVersion(m0, m1, 0, "major", "minor", "build");
        }
        throw new RuntimeException("Missing forge version!");
    }

    private static boolean compareVersion(Matcher m0, Matcher m1, int index, String... groups) {
        if (index == groups.length) return true; // the same as the legacy version
        int result = Integer.compare(Integer.parseInt(m0.group(groups[index])), Integer.parseInt(m1.group(groups[index])));
        if (result < 0) return true; // less than the legacy version
        if (result > 0) return false; // greater than the legacy version
        return compareVersion(m0, m1, index + 1, groups);
    }
}
