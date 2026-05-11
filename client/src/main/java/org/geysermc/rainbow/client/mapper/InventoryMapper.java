package org.geysermc.rainbow.client.mapper;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.Collection;

public class InventoryMapper implements CustomItemProvider {
    public static final InventoryMapper INSTANCE = new InventoryMapper();

    private InventoryMapper() {}

    @Override
    public Collection<ItemStackTemplate> nextItems(LocalPlayer player, ClientPacketListener connection) {
        return player.containerMenu.getItems().stream()
                .filter(stack -> !stack.isEmpty())
                .map(ItemStackTemplate::fromNonEmptyStack)
                .toList();
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public Component name() {
        return Component.translatable("menu.rainbow.manage_pack.auto_mapping.inventory");
    }
}
