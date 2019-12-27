package io.github.zekerzhayard.forgewrapper;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cpw.mods.modlauncher.Launcher;
import io.github.zekerzhayard.forgewrapper.installer.Download;

public class Main {
    public static void main(String[] args) throws Exception {
        URL[] urls = Utils.getURLs(new ArrayList<>());
        Pattern pattern = Pattern.compile("forge\\-(?<mcVersion>[0-9\\.]+)\\-(?<forgeVersion>[0-9\\.]+)\\-launcher\\.jar");
        String version = "";
        for (URL url : urls) {
            Matcher matcher = pattern.matcher(url.getFile());
            if (matcher.find() && url.getFile().endsWith(matcher.group(0))) {
                version = matcher.group("mcVersion") + "-" + matcher.group("forgeVersion");
                String installerUrl = String.format("https://files.minecraftforge.net/maven/net/minecraftforge/forge/%s/forge-%s-installer.jar", version, version);
                Download.download(installerUrl, String.format("./.forgewrapper/forge-%s-installer.jar", version));
                break;
            }
        }

        URLClassLoader ucl = URLClassLoader.newInstance(new URL[] {
            Main.class.getProtectionDomain().getCodeSource().getLocation(),
            new File(String.format("./.forgewrapper/forge-%s-installer.jar", version)).toURI().toURL()
        }, null);

        Class<?> installer = ucl.loadClass("io.github.zekerzhayard.forgewrapper.installer.Installer");
        if (!(boolean) installer.getMethod("install").invoke(null)) {
            return;
        }
        List<String> argsList = Stream.of(args).collect(Collectors.toList());
        argsList.add("--launchTarget");
        argsList.add("fmlclient");
        argsList.add("--fml.forgeVersion");
        argsList.add((String) installer.getMethod("getForgeVersion").invoke(null));
        argsList.add("--fml.mcVersion");
        argsList.add((String) installer.getMethod("getMcVersion").invoke(null));
        argsList.add("--fml.forgeGroup");
        argsList.add("net.minecraftforge");
        argsList.add("--fml.mcpVersion");
        argsList.add((String) installer.getMethod("getMcpVersion").invoke(null));

        Launcher.main(argsList.toArray(new String[0]));
    }
}
