package io.github.zekerzhayard.forgewrapper.installer;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.regex.Pattern;

import net.minecraftforge.installer.actions.ActionCanceledException;
import net.minecraftforge.installer.actions.ProgressCallback;
import net.minecraftforge.installer.json.Install;
import net.minecraftforge.installer.json.Util;

public class Installer {
    private static Install install;

    public static boolean install() throws ActionCanceledException {
        ProgressCallback monitor = ProgressCallback.withOutputs(System.out);
        install = Util.loadInstallProfile();
        if (System.getProperty("java.net.preferIPv4Stack") == null) {
            System.setProperty("java.net.preferIPv4Stack", "true");
        }
        String vendor = System.getProperty("java.vendor", "missing vendor");
        String javaVersion = System.getProperty("java.version", "missing java version");
        String jvmVersion = System.getProperty("java.vm.version", "missing jvm version");
        monitor.message(String.format("JVM info: %s - %s - %s", vendor, javaVersion, jvmVersion));
        monitor.message("java.net.preferIPv4Stack=" + System.getProperty("java.net.preferIPv4Stack"));
        return new ClientInstall4MultiMC(install, monitor).run(null, input -> true);
    }

    public static String getForgeVersion() {
        return install.getVersion().substring(install.getVersion().lastIndexOf("-") + 1);
    }

    public static String getMcVersion() {
        return install.getMinecraft();
    }

    public static String getMcpVersion() {
        return install.getData(true).get("MCP_VERSION").replace("'", "");
    }

    static void hookStdOut(ProgressCallback monitor) {
        final Pattern endingWhitespace = Pattern.compile("\\r?\\n$");
        final OutputStream monitorStream = new OutputStream() {

            @Override
            public void write(byte[] buf, int off, int len) {
                byte[] toWrite = new byte[len];
                System.arraycopy(buf, off, toWrite, 0, len);
                write(toWrite);
            }

            @Override
            public void write(byte[] b) {
                String toWrite = new String(b);
                toWrite = endingWhitespace.matcher(toWrite).replaceAll("");
                if (!toWrite.isEmpty()) {
                    monitor.message(toWrite);
                }
            }

            @Override
            public void write(int b) {
                write(new byte[] {(byte) b});
            }
        };

        System.setOut(new PrintStream(monitorStream));
        System.setErr(new PrintStream(monitorStream));
    }
}
