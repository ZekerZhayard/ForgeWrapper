package io.github.zekerzhayard.forgewrapper;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cpw.mods.modlauncher.Launcher;

public class Utils {
    public static URL[] getURLs(List<String> blackList) {
        ClassLoader cl = Utils.class.getClassLoader();
        List<URL> urls = new ArrayList<>();
        if (cl instanceof URLClassLoader) {
            urls.addAll(Stream.of(((URLClassLoader) cl).getURLs()).filter(url -> blackList.stream().noneMatch(str -> url.getFile().endsWith(str))).collect(Collectors.toList()));
        } else {
            String[] elements = System.getProperty("java.class.path").split(File.pathSeparator);

            for (String ele : elements) {
                try {
                    if (blackList.stream().noneMatch(ele::endsWith)) {
                        urls.add(new File(ele).toURI().toURL());
                    }
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }
        return urls.toArray(new URL[0]);
    }

    public static File getLibrariesDir() {
        try {
            File laucnher = new File(Launcher.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            // see https://github.com/MinecraftForge/MinecraftForge/blob/863ab2ca184cf2e2dfa134d07bfc20d6a9a6a4e8/src/main/java/net/minecraftforge/fml/relauncher/libraries/LibraryManager.java#L151
            //             /<version>      /modlauncher    /mods           /cpw            /libraries
            return laucnher.getParentFile().getParentFile().getParentFile().getParentFile().getParentFile();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
