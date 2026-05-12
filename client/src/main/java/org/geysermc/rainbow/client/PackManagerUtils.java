package org.geysermc.rainbow.client;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import org.geysermc.rainbow.client.mapper.PackMapper;
import org.geysermc.rainbow.pack.BedrockPack;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// These methods are only ever called if player isn't null, so please stop warning me IntelliJ qwq
@SuppressWarnings("DataFlowIssue")
public class PackManagerUtils {
    private static final Component NO_PACK_CREATED = Component.translatable("feedback.rainbow.no_pack", Component.literal("/rainbow create <name>")
            .withStyle(style -> style.withColor(ChatFormatting.BLUE).withUnderlined(true)
                    .withClickEvent(new ClickEvent.SuggestCommand("/rainbow create ")))).withStyle(ChatFormatting.RED);

    public static boolean startPack(String name, PackManager manager, Minecraft minecraft) {
        try {
            manager.startPack(name);
            minecraft.player.sendSystemMessage(
                    Component.translatable("feedback.rainbow.pack_create_success", name)
                            .withStyle(style -> style
                                    .withColor(ChatFormatting.GREEN)
                            )
            );
            return true;
        } catch (IOException e) {
            RainbowClient.LOGGER.error("IOException when creating pack.", e);
            minecraft.player.sendSystemMessage(
                    Component.translatable("feedback.rainbow.pack_create_error")
                            .withStyle(style -> style
                                    .withColor(ChatFormatting.RED)
                                    .withClickEvent(RainbowClient.LOG_CLICK_EVENT)
                            )
            );
            return false;
        }
    }

    public static boolean mapItemInHand(PackManager manager, Minecraft minecraft) {
        if (!manager.isInProgress()) return false;

        manager.run(pack -> {
            ItemStack heldItem = minecraft.player.getMainHandItem();
            if (heldItem.isEmpty()) {
                minecraft.player.sendSystemMessage(Component.literal("Must hold an item to map").withStyle(ChatFormatting.RED));
            } else {
                switch (RainbowClient.getPackMapper().mapItems(pack, List.of(ItemStackTemplate.fromNonEmptyStack(heldItem))).toSingleResult()) {
                    case NONE_MAPPED -> minecraft.player.sendSystemMessage(Component.translatable("commands.rainbow.no_item_mapped").withStyle(ChatFormatting.RED));
                    case PROBLEMS_OCCURRED -> minecraft.player.sendSystemMessage(Component.translatable("commands.rainbow.mapped_held_item_problems"));
                    case MAPPED_SUCCESSFULLY -> minecraft.player.sendSystemMessage(Component.translatable("commands.rainbow.mapped_held_item"));
                }
            }
        });

        return true;
    }

    public static boolean mapItemsInInventory(PackManager manager, Minecraft minecraft) {
        if (!manager.isInProgress()) return false;

        manager.run(pack -> {
            List<ItemStackTemplate> inventoryStacks = new ArrayList<>(minecraft.player.inventoryMenu.getItems().stream()
                    .filter(stack -> !stack.isEmpty())
                    .map(ItemStackTemplate::fromNonEmptyStack)
                    .toList());
            if (minecraft.player.hasContainerOpen()) {
                inventoryStacks.addAll(minecraft.player.containerMenu.getItems().stream()
                        .filter(stack -> !stack.isEmpty())
                        .map(ItemStackTemplate::fromNonEmptyStack)
                        .toList());
            }
            PackMapper.MappingResults results = RainbowClient.getPackMapper().mapItems(pack, inventoryStacks);

            if (results.itemsMapped() > 0 || results.skullsMapped() > 0) {
                if (results.itemsMapped() > 0) {
                    minecraft.player.sendSystemMessage(Component.translatable("commands.rainbow.mapped_items_from_inventory", results.itemsMapped()).withStyle(ChatFormatting.GREEN));
                    if (results.problems() > 0) {
                        minecraft.player.sendSystemMessage(Component.translatable("commands.rainbow.mapped_items_problems").withStyle(ChatFormatting.YELLOW));
                    }
                }
                if (results.skullsMapped() > 0) {
                    minecraft.player.sendSystemMessage(Component.translatable("commands.rainbow.mapped_skulls_from_inventory", results.skullsMapped()).withStyle(ChatFormatting.GREEN));
                }
            } else {
                minecraft.player.sendSystemMessage(Component.translatable("commands.rainbow.no_items_mapped").withStyle(ChatFormatting.RED));
            }
        });

        return true;
    }

    public static boolean finishPack(PackManager manager, Minecraft minecraft) {
        if (!manager.isInProgress()) return false;

        minecraft.player.sendSystemMessage(Component.translatable("commands.rainbow.pack_finishing"));
        Optional<Path> exportPath = manager.getExportPath();
        Runnable onFinish = () -> minecraft.player.sendSystemMessage(Component.translatable("commands.rainbow.pack_finished_successfully").withStyle(style
                -> style.withUnderlined(true).withClickEvent(new ClickEvent.OpenFile(exportPath.orElseThrow()))));
        if (!manager.finish(onFinish)) {
            minecraft.player.sendSystemMessage(NO_PACK_CREATED);
        }

        return true;
    }
}
