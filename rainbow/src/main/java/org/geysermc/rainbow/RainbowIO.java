package org.geysermc.rainbow;

import com.mojang.logging.LogUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RainbowIO {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final List<IOExceptionListener> listeners = new ArrayList<>();

    private RainbowIO() {}

    public static <T> Optional<T> safeIO(IOSupplier<T> supplier) {
        try {
            return Optional.ofNullable(supplier.get());
        } catch (IOException exception) {
            LOGGER.error("Failed to perform IO operation!", exception);
            listeners.forEach(listener -> listener.error(exception));
            return Optional.empty();
        }
    }

    public static <T> T safeIO(IOSupplier<T> supplier, T defaultValue) {
        return safeIO(supplier).orElse(defaultValue);
    }

    public static void safeIO(IORunnable runnable) {
        safeIO(() -> {
            runnable.run();
            return null;
        });
    }

    public static void registerExceptionListener(IOExceptionListener listener) {
        listeners.add(listener);
    }

    @FunctionalInterface
    public interface IOSupplier<T> {

        @Nullable T get() throws IOException;
    }

    @FunctionalInterface
    public interface IORunnable {

        void run() throws IOException;
    }

    @FunctionalInterface
    public interface IOExceptionListener {

        void error(IOException exception);
    }
}
