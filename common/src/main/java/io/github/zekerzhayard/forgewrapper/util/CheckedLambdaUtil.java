package io.github.zekerzhayard.forgewrapper.util;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class CheckedLambdaUtil {
    public static <T> Consumer<T> wrapConsumer(CheckedConsumer<T> consumer) {
        return consumer;
    }

    public static <T, U> BiConsumer<T, U> wrapBiConsumer(CheckedBiConsumer<T, U> biconsumer) {
        return biconsumer;
    }

    public interface CheckedConsumer<T> extends Consumer<T> {
        void checkedAccept(T t) throws Throwable;

        @Override
        default void accept(T t) {
            try {
                this.checkedAccept(t);
            } catch (Throwable th) {
                throw new RuntimeException(th);
            }
        }
    }

    public interface CheckedBiConsumer<T, U> extends BiConsumer<T, U> {
        void checkedAccept(T t, U u) throws Throwable;

        @Override
        default void accept(T t, U u) {
            try {
                this.checkedAccept(t, u);
            } catch (Throwable th) {
                throw new RuntimeException(th);
            }
        }
    }
}
