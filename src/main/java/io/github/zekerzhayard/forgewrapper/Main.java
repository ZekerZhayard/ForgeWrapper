package io.github.zekerzhayard.forgewrapper;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cpw.mods.modlauncher.Launcher;
import io.github.zekerzhayard.forgewrapper.converter.Converter;
import io.github.zekerzhayard.forgewrapper.installer.Download;

public class Main {
    public static void main(String[] args) throws Exception {
        URL[] urls = Utils.getURLs(new ArrayList<>());
        Pattern pattern = Pattern.compile("forge-(?<mcVersion>[0-9.]+)-(?<forgeVersion>[0-9.]+)\\.jar");
        String version = "";
        String installerFileStr = "";
        for (URL url : urls) {
            Matcher matcher = pattern.matcher(url.getFile());
            if (matcher.find() && url.getFile().endsWith(matcher.group(0))) {
                version = matcher.group("mcVersion") + "-" + matcher.group("forgeVersion");
                String installerUrl = String.format("https://files.minecraftforge.net/maven/net/minecraftforge/forge/%s/forge-%s-installer.jar", version, version);
                installerFileStr = String.format("./.forgewrapper/forge-%s-installer.jar", version);
                Download.download(installerUrl, installerFileStr);
                break;
            }
        }

        URLClassLoader ucl = URLClassLoader.newInstance(new URL[] {
            Main.class.getProtectionDomain().getCodeSource().getLocation(),
            Launcher.class.getProtectionDomain().getCodeSource().getLocation(),
            new File(installerFileStr).toURI().toURL()
        }, null);

        Class<?> installer = ucl.loadClass("io.github.zekerzhayard.forgewrapper.installer.Installer");
        if (!(boolean) installer.getMethod("install").invoke(null)) {
            return;
        }
        List<String> argsList = Stream.of(args).collect(Collectors.toList());
        argsList.addAll(Converter.getAdditionalArgs(Paths.get(installerFileStr)));

        Launcher.main(argsList.toArray(new String[0]));
    }
}
