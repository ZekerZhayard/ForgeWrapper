package io.github.zekerzhayard.forgewrapper.installer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.github.zekerzhayard.forgewrapper.installer.util.ModuleUtil;

public class Bootstrap {
    public static void bootstrap(List<String> jvmArgs, String minecraftJar, String libraryDir) throws Throwable {
        // Replace all placeholders
        List<String> replacedJvmArgs = new ArrayList<>();
        for (String arg : jvmArgs) {
            replacedJvmArgs.add(arg.replace("${classpath}", System.getProperty("java.class.path").replace(File.separator, "/")).replace("${classpath_separator}", File.pathSeparator).replace("${library_directory}", libraryDir).replace("${version_name}", minecraftJar.substring(0, minecraftJar.lastIndexOf('.'))));
        }
        jvmArgs = replacedJvmArgs;

        String modulePath = null;
        List<String> addExports = new ArrayList<>();
        List<String> addOpens = new ArrayList<>();
        for (int i = 0; i < jvmArgs.size(); i++) {
            String arg = jvmArgs.get(i);

            if (arg.equals("-p") || arg.equals("--module-path")) {
                modulePath = jvmArgs.get(i + 1);
            } else if (arg.startsWith("--module-path=")) {
                modulePath = arg.split("=", 2)[1];
            }

            if (arg.equals("--add-exports")) {
                addExports.add(jvmArgs.get(i + 1));
            } else if (arg.startsWith("--add-exports=")) {
                addExports.add(arg.split("=", 2)[1]);
            }

            if (arg.equals("--add-opens")) {
                addOpens.add(jvmArgs.get(i + 1));
            } else if (arg.startsWith("--add-opens=")) {
                addOpens.add(arg.split("=", 2)[1]);
            }

            // Java properties
            if (arg.startsWith("-D")) {
                String[] prop = arg.substring(2).split("=", 2);

                if (prop[0].equals("ignoreList")) {
                    // The default ignoreList may cause some problems, so we define it more precisely.
                    System.setProperty(prop[0], prop[1] + ",NewLaunch.jar,ForgeWrapper-," + minecraftJar);
                } else {
                    System.setProperty(prop[0], prop[1]);
                }
            }
        }

        if (modulePath != null) {
            ModuleUtil.addModules(modulePath);
        }
        ModuleUtil.addExports(addExports);
        ModuleUtil.addOpens(addOpens);
    }
}
