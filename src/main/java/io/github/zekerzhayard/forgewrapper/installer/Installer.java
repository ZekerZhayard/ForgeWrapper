package io.github.zekerzhayard.forgewrapper.installer;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraftforge.installer.DownloadUtils;
import net.minecraftforge.installer.actions.PostProcessors;
import net.minecraftforge.installer.actions.ProgressCallback;
import net.minecraftforge.installer.json.Artifact;
import net.minecraftforge.installer.json.Install;
import net.minecraftforge.installer.json.InstallV1;
import net.minecraftforge.installer.json.Util;
import net.minecraftforge.installer.json.Version;

public class Installer {
    private static InstallV1Wrapper wrapper;
    private static InstallV1Wrapper getWrapper(File librariesDir) {
        if (wrapper == null) {
            wrapper = new InstallV1Wrapper(Util.loadInstallProfile(), librariesDir);
        }
        return wrapper;
    }

    public static Map<String, Object> getData(File librariesDir) {
        Map<String, Object> data = new HashMap<>();
        Version0 version = Version0.loadVersion(getWrapper(librariesDir));
        data.put("mainClass", version.getMainClass());
        data.put("jvmArgs", version.getArguments().getJvm());
        data.put("extraLibraries", getExtraLibraries(version));
        return data;
    }

    public static boolean install(File libraryDir, File minecraftJar, File installerJar) throws Throwable {
        ProgressCallback monitor = ProgressCallback.withOutputs(System.out);
        if (System.getProperty("java.net.preferIPv4Stack") == null) {
            System.setProperty("java.net.preferIPv4Stack", "true");
        }
        String vendor = System.getProperty("java.vendor", "missing vendor");
        String javaVersion = System.getProperty("java.version", "missing java version");
        String jvmVersion = System.getProperty("java.vm.version", "missing jvm version");
        monitor.message(String.format("JVM info: %s - %s - %s", vendor, javaVersion, jvmVersion));
        monitor.message("java.net.preferIPv4Stack=" + System.getProperty("java.net.preferIPv4Stack"));
        monitor.message("Current Time: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()));

        // MinecraftForge has removed all old installers since 2024/2/27, but they still exist in NeoForge.
        PostProcessors processors = new PostProcessors(wrapper, true, monitor);
        Method processMethod = PostProcessors.class.getMethod("process", File.class, File.class, File.class, File.class);
        if (boolean.class.equals(processMethod.getReturnType())) {
            return (boolean) processMethod.invoke(processors, libraryDir, minecraftJar, libraryDir.getParentFile(), installerJar);
        } else {
            return processMethod.invoke(processors, libraryDir, minecraftJar, libraryDir.getParentFile(), installerJar) != null;
        }
    }

    // Some libraries in the version json are not available via direct download,
    // so they are not available in the MultiMC meta json,
    // so wee need to get them manually.
    private static List<String> getExtraLibraries(Version0 version) {
        List<String> paths = new ArrayList<>();
        for (Version.Library library : version.getLibraries()) {
            Version.LibraryDownload artifact = library.getDownloads().getArtifact();
            if (artifact.getUrl().isEmpty()) {
                paths.add(artifact.getPath());
            }
        }
        return paths;
    }

    public static class InstallV1Wrapper extends InstallV1 {
        protected Map<String, List<Processor>> processors = new HashMap<>();
        protected File librariesDir;

        public InstallV1Wrapper(InstallV1 v1, File librariesDir) {
            super(v1);
            this.serverJarPath = v1.getServerJarPath();
            this.librariesDir = librariesDir;
        }

        @Override
        public List<Processor> getProcessors(String side) {
            List<Processor> processor = this.processors.get(side);
            if (processor == null) {
                checkProcessorFiles(processor = super.getProcessors(side), super.getData("client".equals(side)), this.librariesDir);
                this.processors.put(side, processor);
            }
            return processor;
        }

        private static void checkProcessorFiles(List<Processor> processors, Map<String, String> data, File base) {
            Map<String, File> artifactData = new HashMap<>();
            for (Map.Entry<String, String> entry : data.entrySet()) {
                String value = entry.getValue();
                if (value.charAt(0) == '[' && value.charAt(value.length() - 1) == ']') {
                    artifactData.put("{" + entry.getKey() + "}", Artifact.from(value.substring(1, value.length() - 1)).getLocalPath(base));
                }
            }

            Map<Processor, Map<String, String>> outputsMap = new HashMap<>();
            label:
            for (Processor processor : processors) {
                Map<String, String> outputs = new HashMap<>();
                if (processor.getOutputs().isEmpty()) {
                    String[] args = processor.getArgs();
                    for (int i = 0; i < args.length; i++) {
                        for (Map.Entry<String, File> entry : artifactData.entrySet()) {
                            if (args[i].contains(entry.getKey())) {
                                // We assume that all files that exist but don't have the sha1 checksum are valid.
                                if (entry.getValue().exists()) {
                                    outputs.put(entry.getKey(), DownloadUtils.getSha1(entry.getValue()));
                                } else {
                                    outputsMap.clear();
                                    break label;
                                }
                            }
                        }
                    }
                    outputsMap.put(processor, outputs);
                }
            }
            for (Map.Entry<Processor, Map<String, String>> entry : outputsMap.entrySet()) {
                setOutputs(entry.getKey(), entry.getValue());
            }
        }

        private static Field outputsField;
        private static void setOutputs(Processor processor, Map<String, String> outputs) {
            try {
                if (outputsField == null) {
                    outputsField = Processor.class.getDeclaredField("outputs");
                    outputsField.setAccessible(true);
                }
                outputsField.set(processor, outputs);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }
    }

    public static class Version0 extends Version {

        public static Version0 loadVersion(Install profile) {
            try (InputStream stream = Util.class.getResourceAsStream(profile.getJson())) {
                return Util.GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), Version0.class);
            } catch (Throwable t) {
                throw new RuntimeException(t);
            }
        }

        protected String mainClass;
        protected Version0.Arguments arguments;

        public String getMainClass() {
            return mainClass;
        }

        public Version0.Arguments getArguments() {
            return arguments;
        }

        public static class Arguments {
            protected String[] jvm;

            public String[] getJvm() {
                return jvm;
            }
        }
    }
}
