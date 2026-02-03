package org.geysermc.rainbow.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.geysermc.rainbow.pack.BedrockPack;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// These methods are only ever called if player isn't null, so please stop warning me IntelliJ qwq
@SuppressWarnings("DataFlowIssue")
public class PackManagerUtils {
    public static boolean startPack(String name, PackManager manager, Minecraft minecraft) {
        try {
            manager.startPack(name);
            minecraft.player.displayClientMessage(
                    Component.translatable("feedback.rainbow.pack_create_success", name)
                            .withStyle(style -> style
                                    .withColor(ChatFormatting.GREEN)
                            ),
                    false
            );
            return true;
        } catch (IOException e) {
            RainbowClient.LOGGER.error("IOException when creating pack.", e);
            minecraft.player.displayClientMessage(
                    Component.translatable("feedback.rainbow.pack_create_error")
                            .withStyle(style -> style
                                    .withColor(ChatFormatting.RED)
                                    .withClickEvent(RainbowClient.LOG_CLICK_EVENT)
                            ),
                    false
            );
            return false;
        }
    }

    public static boolean mapItemInHand(PackManager manager, Minecraft minecraft) {
        if (!manager.isInProgress()) return false;

        manager.run(pack -> {
            ItemStack heldItem = minecraft.player.getMainHandItem();
            switch (pack.map(heldItem)) {
                case NONE_MAPPED -> minecraft.player.displayClientMessage(Component.translatable("feedback.rainbow.no_item_mapped").withStyle(ChatFormatting.RED), false);
                case PROBLEMS_OCCURRED -> minecraft.player.displayClientMessage(Component.translatable("feedback.rainbow.mapped_item_problems").withStyle(ChatFormatting.YELLOW), false);
                case MAPPED_SUCCESSFULLY -> minecraft.player.displayClientMessage(Component.translatable("feedback.rainbow.mapped_item").withStyle(ChatFormatting.GREEN), false);
            }
        });

        return true;
    }

    public static boolean mapItemsInInventory(PackManager manager, Minecraft minecraft) {
        if (!manager.isInProgress()) return false;

        manager.run(pack -> {
            int mapped = 0;
            boolean errors = false;

            List<ItemStack> items;

            if (minecraft.screen instanceof AbstractContainerScreen<?> abstractContainerScreen) {
                items = abstractContainerScreen.getMenu().getItems();
            } else {
                items = new ArrayList<>(minecraft.player.getInventory().getNonEquipmentItems());
                items.add(minecraft.player.getInventory().getItem(36)); // Boots
                items.add(minecraft.player.getInventory().getItem(37)); // Leggings
                items.add(minecraft.player.getInventory().getItem(38)); // Chestplate
                items.add(minecraft.player.getInventory().getItem(39)); // Helmet
                items.add(minecraft.player.getInventory().getItem(40)); // Offhand
            }

            for (ItemStack stack : items) {
                BedrockPack.MappingResult result = pack.map(stack);
                if (result != BedrockPack.MappingResult.NONE_MAPPED) {
                    mapped++;
                    if (result == BedrockPack.MappingResult.PROBLEMS_OCCURRED) {
                        errors = true;
                    }
                }
            }

            if (mapped > 0) {
                minecraft.player.displayClientMessage(Component.translatable("feedback.rainbow.mapped_items", mapped).withStyle(ChatFormatting.GREEN), false);
                if (errors) {
                    minecraft.player.displayClientMessage(Component.translatable("feedback.rainbow.mapped_items_problems").withStyle(ChatFormatting.YELLOW), false);
                }
            } else {
                minecraft.player.displayClientMessage(Component.translatable("feedback.rainbow.no_items_mapped").withStyle(ChatFormatting.RED), false);
            }
        });

        return true;
    }

    public static boolean finishPack(PackManager manager, Minecraft minecraft) {
        if (!manager.isInProgress()) return false;

        Optional<Path> exportPath = manager.getExportPath();
        // Ignore the result of the method, we check above
        manager.finish(() -> minecraft.player.displayClientMessage(
                Component.translatable("feedback.rainbow.pack_finished_success")
                        .withStyle(style -> style
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent.OpenFile(exportPath.orElseThrow()))
                        ),
                false
        ));

        return true;
    }
}
