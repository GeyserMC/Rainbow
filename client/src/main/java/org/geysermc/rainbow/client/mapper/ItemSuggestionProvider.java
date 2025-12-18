package org.geysermc.rainbow.client.mapper;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

// TODO safety
public class ItemSuggestionProvider implements CustomItemProvider {
    private final List<String> remainingCommands;
    private boolean waitingOnItem = false;
    private boolean waitingOnClear = false;

    public ItemSuggestionProvider(List<String> commands) {
        remainingCommands = new ArrayList<>(commands);
    }

    public Stream<ItemStack> nextItems(LocalPlayer player, ClientPacketListener connection) {
        if (!remainingCommands.isEmpty() || waitingOnItem) {
            if (waitingOnClear && player.getInventory().isEmpty()) {
                waitingOnClear = false;
            } else if (!waitingOnItem) {
                connection.send(new ServerboundChatCommandPacket(remainingCommands.removeFirst()));
                waitingOnItem = true;
            } else {
                if (!player.getInventory().isEmpty()) {
                    Stream<ItemStack> items = player.getInventory().getNonEquipmentItems().stream();
                    connection.send(new ServerboundChatCommandPacket("clear"));

                    waitingOnItem = false;
                    if (!remainingCommands.isEmpty()) {
                        waitingOnClear = true;
                    }
                    return items;
                }
            }
        }
        return Stream.empty();
    }

    public int queueSize() {
        return remainingCommands.size();
    }

    @Override
    public boolean isDone() {
        return remainingCommands.isEmpty() && !waitingOnItem && !waitingOnClear;
    }

    @Override
    public Component name() {
        return Component.translatable("menu.rainbow.manage_pack.auto_mapping.commands");
    }
}
