package io.github.zekerzhayard.forgewrapper.installer;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;

import org.apache.commons.codec.digest.DigestUtils;

public class Download {
    public static void download(String url, String location) throws IOException {
        File localFile = new File(location);
        localFile.getParentFile().mkdirs();
        if (localFile.isFile()) {
            try {
                Files.copy(new URL(url + ".md5").openConnection().getInputStream(), Paths.get(location + ".md5"), StandardCopyOption.REPLACE_EXISTING);
                Files.copy(new URL(url + ".sha1").openConnection().getInputStream(), Paths.get(location + ".sha1"), StandardCopyOption.REPLACE_EXISTING);
                String md5 = new String(Files.readAllBytes(Paths.get(location + ".md5")));
                String sha1 = new String(Files.readAllBytes(Paths.get(location + ".sha1")));
                if (!checkMD5(location, md5) || !checkSHA1(location, sha1)) {
                    localFile.delete();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!localFile.isFile()) {
            if (localFile.isDirectory()) {
                throw new RuntimeException(location + " must be a file!");
            }
            Files.copy(new URL(url).openConnection().getInputStream(), Paths.get(location), StandardCopyOption.REPLACE_EXISTING);
            download(url, location);
        }
    }

    public static boolean checkMD5(String path, String hash) throws IOException {
        String md5 = DigestUtils.md5Hex(Files.readAllBytes(Paths.get(path)));
        return md5.toLowerCase(Locale.ENGLISH).equals(hash.toLowerCase(Locale.ENGLISH));
    }

    public static boolean checkSHA1(String path, String hash) throws IOException {
        String sha1 = DigestUtils.sha1Hex(Files.readAllBytes(Paths.get(path)));
        return sha1.toLowerCase(Locale.ENGLISH).equals(hash.toLowerCase(Locale.ENGLISH));
    }
}
