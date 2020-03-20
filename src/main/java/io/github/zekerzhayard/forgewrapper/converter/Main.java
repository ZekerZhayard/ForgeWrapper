package io.github.zekerzhayard.forgewrapper.converter;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import io.github.zekerzhayard.forgewrapper.installer.Download;

public class Main {
    public static void main(String[] args) {
        Path installer = null, instance = Paths.get(".");
        String cursepack = null;
        try {
            HashMap<String, String> argsMap = parseArgs(args);
            if (argsMap.containsKey("--installer")) {
                installer = Paths.get(argsMap.get("--installer"));
            } else {
                installer = Paths.get(System.getProperty("java.io.tmpdir", "."), "gson-2.8.6.jar");
                Download.download("https://repo1.maven.org/maven2/com/google/code/gson/gson/2.8.6/gson-2.8.6.jar", installer.toString());
            }
            if (argsMap.containsKey("--instance")) {
                instance = Paths.get(argsMap.get("--instance"));
            }
            if (argsMap.containsKey("--cursepack")) {
                cursepack = argsMap.get("--cursepack");
            }
        } catch (Exception e) {
            System.out.println("Invalid arguments! Use: java -jar <ForgeWrapper.jar> [--installer=<forge-installer.jar> | --cursepack=<curseforge-modpack.zip>] [--instance=<instance-path>]");
            throw new RuntimeException(e);
        }

        try {
            URLClassLoader ucl = URLClassLoader.newInstance(new URL[] {
                Converter.class.getProtectionDomain().getCodeSource().getLocation(),
                installer.toUri().toURL()
            }, null);
            ucl.loadClass("io.github.zekerzhayard.forgewrapper.converter.Converter").getMethod("convert", Path.class, Path.class, String.class).invoke(null, installer, instance, cursepack);
            System.out.println("Successfully install Forge for MultiMC!");
        } catch (Exception e) {
            System.out.println("Failed to install Forge!");
            throw new RuntimeException(e);
        }
    }

    /**
     * @return installer -- The path of forge installer.<br/>
     * instance -- The instance folder of MultiMC.<br/>
     * cursepack -- The version of cursepacklocator.<br/>
     */
    private static HashMap<String, String> parseArgs(String[] args) {
        HashMap<String, String> map = new HashMap<>();
        for (String arg : args) {
            String[] params = arg.split("=", 2);
            map.put(params[0], params[1]);
        }
        if (!map.containsKey("--installer") && !map.containsKey("--cursepack")) {
            throw new IllegalArgumentException();
        }
        return map;
    }
}
