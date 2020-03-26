package io.github.zekerzhayard.forgewrapper.installer;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cpw.mods.modlauncher.Launcher;
import io.github.zekerzhayard.forgewrapper.Utils;

public class Main {
    public static void main(String[] args) throws Exception {
        List<String> argsList = Stream.of(args).collect(Collectors.toList());
        String version = argsList.get(argsList.indexOf("--fml.mcVersion") + 1) + "-" + argsList.get(argsList.indexOf("--fml.forgeVersion") + 1);
        String installerUrl = String.format("https://files.minecraftforge.net/maven/net/minecraftforge/forge/%s/forge-%s-installer.jar", version, version);
        String installerFileStr = String.format("./.forgewrapper/forge-%s-installer.jar", version);
        Utils.download(installerUrl, installerFileStr);

        URLClassLoader ucl = URLClassLoader.newInstance(new URL[] {
            Main.class.getProtectionDomain().getCodeSource().getLocation(),
            Launcher.class.getProtectionDomain().getCodeSource().getLocation(),
            new File(installerFileStr).toURI().toURL()
        }, null);

        Class<?> installer = ucl.loadClass("io.github.zekerzhayard.forgewrapper.installer.Installer");
        if (!(boolean) installer.getMethod("install").invoke(null)) {
            return;
        }

        Launcher.main(args);
    }
}
