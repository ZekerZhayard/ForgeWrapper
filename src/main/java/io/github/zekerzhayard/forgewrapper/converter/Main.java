package io.github.zekerzhayard.forgewrapper.converter;

import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) {
        ArrayList<String> argsList = new ArrayList<>(Arrays.asList(args));
        Path installer, instance;
        try {
            installer = Paths.get(argsList.get(argsList.indexOf("--installer") + 1));
            instance = Paths.get(".");
            if (argsList.contains("--instance")) {
                instance = Paths.get(argsList.get(argsList.indexOf("--instance") + 1));
            }
        } catch (Exception e) {
            System.out.println("Invalid arguments! Use: java -jar <ForgeWrapper.jar> [--installer] <forge-installer.jar> [--instance <instance-path>]");
            throw new RuntimeException(e);
        }

        try {
            URLClassLoader ucl = URLClassLoader.newInstance(new URL[] {
                Converter.class.getProtectionDomain().getCodeSource().getLocation(),
                installer.toUri().toURL()
            }, null);
            ucl.loadClass("io.github.zekerzhayard.forgewrapper.converter.Converter").getMethod("convert", Path.class, Path.class).invoke(null, installer, instance);
            System.out.println("Successfully install Forge for MultiMC!");
        } catch (Exception e) {
            System.out.println("Failed to install Forge!");
            throw new RuntimeException(e);
        }
    }
}
