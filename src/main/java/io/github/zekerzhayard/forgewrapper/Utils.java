package io.github.zekerzhayard.forgewrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public class Utils {
    private static final char[] DIGITS = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    public static void download(String url, String location) throws Exception {
        File localFile = new File(location);
        localFile.getParentFile().mkdirs();
        if (localFile.isFile()) {
            try {
                System.out.println("Checking Fingerprints of installer...");
                String md5 = new BufferedReader(new InputStreamReader(new URL(url + ".md5").openConnection().getInputStream())).readLine();
                String sha1 = new BufferedReader(new InputStreamReader(new URL(url + ".sha1").openConnection().getInputStream())).readLine();
                if (!checkMD5(location, md5) || !checkSHA1(location, sha1)) {
                    System.out.println("Fingerprints do not match!");
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
            System.out.println("Downloading forge installer... (" + url + " ---> " + location + ")");
            Files.copy(new URL(url).openConnection().getInputStream(), Paths.get(location), StandardCopyOption.REPLACE_EXISTING);
            download(url, location);
        }
    }

    public static boolean checkMD5(String path, String hash) throws IOException, NoSuchAlgorithmException {
        String md5 = new String(encodeHex(MessageDigest.getInstance("MD5").digest(Files.readAllBytes(Paths.get(path)))));
        System.out.println("MD5: " + hash + " ---> " + md5);
        return md5.toLowerCase(Locale.ENGLISH).equals(hash.toLowerCase(Locale.ENGLISH));
    }

    public static boolean checkSHA1(String path, String hash) throws IOException, NoSuchAlgorithmException {
        String sha1 = new String(encodeHex(MessageDigest.getInstance("SHA-1").digest(Files.readAllBytes(Paths.get(path)))));
        System.out.println("SHA-1: " + hash + " ---> " + sha1);
        return sha1.toLowerCase(Locale.ENGLISH).equals(hash.toLowerCase(Locale.ENGLISH));
    }

    private static char[] encodeHex(final byte[] data) {
        final int l = data.length;
        final char[] out = new char[l << 1];
        for (int i = 0, j = 0; i < l; i++) {
            out[j++] = DIGITS[(0xF0 & data[i]) >>> 4];
            out[j++] = DIGITS[0x0F & data[i]];
        }
        return out;
    }
}
