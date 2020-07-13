package io.github.zekerzhayard.forgewrapper.installer.detector;

import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public class DetectorLoader {
    public static IFileDetector loadDetector() {
        ServiceLoader<IFileDetector> sl = ServiceLoader.load(IFileDetector.class);
        HashMap<String, IFileDetector> detectors = new HashMap<>();
        for (IFileDetector detector : sl) {
            detectors.put(detector.name(), detector);
        }

        boolean enabled = false;
        IFileDetector temp = null;
        for (Map.Entry<String, IFileDetector> detector : detectors.entrySet()) {
            HashMap<String, IFileDetector> others = new HashMap<>(detectors);
            others.remove(detector.getKey());
            if (!enabled) {
                enabled = detector.getValue().enabled(others);
                if (enabled) {
                    temp = detector.getValue();
                }
            } else if (detector.getValue().enabled(others)) {
                throw new RuntimeException("There are two or more file detectors are enabled! (" + temp.toString() + ", " + detector.toString() + ")");
            }
        }

        if (temp == null) {
            throw new RuntimeException("No file detector is enabled!");
        }
        return temp;
    }
}
