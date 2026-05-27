package org.geysermc.rainbow.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import org.geysermc.rainbow.RainbowIO;

import java.io.IOException;

public class RainbowClientIOHandler implements RainbowIO.IOExceptionListener {
    private static final SystemToast.SystemToastId TOAST_ID = new SystemToast.SystemToastId();

    @Override
    public void error(IOException exception) {
        Minecraft.getInstance().gui.toastManager().addToast(new SystemToast(TOAST_ID,
                Component.translatable("toast.rainbow.io_exception.title"), Component.translatable("toast.rainbow.io_exception.description")));
    }
}
