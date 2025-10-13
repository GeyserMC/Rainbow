package org.geysermc.rainbow.packconverter;

import net.minecraft.client.Minecraft;
import org.geysermc.pack.converter.pipeline.ConversionContext;
import org.geysermc.pack.converter.util.LogListener;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.creative.MinecraftCreativeResourcePack;
import org.geysermc.rainbow.pack.BedrockPack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record PackConverterContext(String packName, LogListener logListener, MinecraftCreativeResourcePack pack) {

    public ConversionContext createConversionContext() {
        return new ConversionContext(packName, logListener);
    }

    public static PackConverterContext create(BedrockPack pack) {
        return new PackConverterContext(pack.name(), RainbowLogListener.INSTANCE, new MinecraftCreativeResourcePack(Minecraft.getInstance().getResourceManager()));
    }

    private static class RainbowLogListener implements LogListener {
        private static final LogListener INSTANCE = new RainbowLogListener();

        @Override
        public void debug(@NotNull String message) {
            Rainbow.LOGGER.debug(message);
        }

        @Override
        public void info(@NotNull String message) {
            Rainbow.LOGGER.info(message);
        }

        @Override
        public void warn(@NotNull String message) {
            Rainbow.LOGGER.warn(message);
        }

        @Override
        public void error(@NotNull String message) {
            Rainbow.LOGGER.error(message);
        }

        @Override
        public void error(@NotNull String message, @Nullable Throwable exception) {
            Rainbow.LOGGER.error(message, exception);
        }
    }
}
