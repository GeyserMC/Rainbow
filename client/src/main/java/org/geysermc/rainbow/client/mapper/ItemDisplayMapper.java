package org.geysermc.rainbow.client.mapper;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ItemDisplayMapper implements CustomItemProvider {
    public static final ItemDisplayMapper INSTANCE = new ItemDisplayMapper();

    private ItemDisplayMapper() {}

    @Override
    public Collection<ItemStackTemplate> nextItems(LocalPlayer player, ClientPacketListener connection) {
        List<ItemStackTemplate> items = new ArrayList<>();
        ((ClientLevel) player.level()).entitiesForRendering().forEach(entity -> {
            if (entity instanceof Display.ItemDisplay itemDisplay) {
                ItemStack stack = itemDisplay.getItemStack();
                if (!stack.isEmpty()) {
                    items.add(ItemStackTemplate.fromNonEmptyStack(stack));
                }
            }
        });
        return items;
    }

    @Override
    public boolean isDone() {
        return true;
    }
}
