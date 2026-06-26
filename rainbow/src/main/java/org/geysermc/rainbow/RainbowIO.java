/*
 * Copyright (c) 2026 GeyserMC. https://geysermc.org
 *
 * This file is part of Rainbow.
 *
 * Rainbow is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * Rainbow is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * Rainbow. If not, see <https://www.gnu.org/licenses/>.
 */

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
