package org.geysermc.rainbow.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.geysermc.rainbow.RainbowIO;

import java.io.IOException;

public class RainbowClientIOHandler implements RainbowIO.IOExceptionListener {
    @Override
    public void error(IOException exception) {
        Minecraft.getInstance().player.displayClientMessage(
                Component.translatable("feedback.rainbow.io_exception")
                        .withStyle(style -> style
                                .withColor(ChatFormatting.RED)
                                .withClickEvent(RainbowClient.LOG_CLICK_EVENT)
                        ),
                false
        );
    }
}
