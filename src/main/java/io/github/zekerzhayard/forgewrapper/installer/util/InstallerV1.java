package io.github.zekerzhayard.forgewrapper.installer.util;

import java.io.File;
import java.lang.reflect.Method;

import net.minecraftforge.installer.actions.PostProcessors;
import net.minecraftforge.installer.actions.ProgressCallback;
import net.minecraftforge.installer.json.Install;
import net.minecraftforge.installer.json.InstallV1;
import net.minecraftforge.installer.json.Util;

public class InstallerV1 extends AbstractInstaller {
    @Override
    public Install loadInstallProfile() {
        return Util.loadInstallProfile();
    }

    @Override
    public boolean runClientInstall(Install profile, ProgressCallback monitor, File libraryDir, File minecraftJar,
            File installerJar) {
        PostProcessors processors = new PostProcessors(
                profile instanceof InstallV1 ? (InstallV1) profile : new InstallV1(profile), true, monitor);

        try {
            Method method = processors.getClass().getMethod("process", File.class, File.class, File.class, File.class);
            Object result = method.invoke(processors, libraryDir, minecraftJar, libraryDir.getParentFile(),
                    installerJar);

            if (method.getReturnType() == boolean.class)
                return (boolean) result;

            return result != null;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
