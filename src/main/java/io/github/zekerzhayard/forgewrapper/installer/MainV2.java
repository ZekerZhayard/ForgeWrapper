package io.github.zekerzhayard.forgewrapper.installer;

import java.util.function.Consumer;

// to support forge 1.17 (bootstraplauncher)
public class MainV2 implements Consumer<String[]> {
    @Override
    public void accept(String[] args) {
        try {
            Main.main(args);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
}
