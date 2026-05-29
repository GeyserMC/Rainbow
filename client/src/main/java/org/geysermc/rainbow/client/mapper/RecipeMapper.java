package org.geysermc.rainbow.client.mapper;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.display.SlotDisplay;

import java.util.Collection;

public final class RecipeMapper implements CustomItemProvider {
    public static final RecipeMapper INSTANCE = new RecipeMapper();

    private RecipeMapper() {}

    @Override
    public Collection<ItemStackTemplate> nextItems(LocalPlayer player, ClientPacketListener connection) {
        return player.getRecipeBook().getCollections().stream()
                .flatMap(collection -> collection.getRecipes().stream())
                .map(entry -> entry.display().result())
                .filter(result -> result instanceof SlotDisplay.ItemStackSlotDisplay)
                .map(result -> ((SlotDisplay.ItemStackSlotDisplay) result).stack())
                .toList();
    }

    @Override
    public boolean isDone() {
        return true;
    }
}
