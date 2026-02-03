package org.geysermc.rainbow.client.mapper;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.stream.Stream;

public class NoItemProvider implements CustomItemProvider {
    public static final NoItemProvider INSTANCE = new NoItemProvider();

    private NoItemProvider() {}

    @Override
    public Stream<ItemStack> nextItems(LocalPlayer player, ClientPacketListener connection) {
        return Stream.empty();
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public Component name() {
        return Component.translatable("menu.rainbow.manage_pack.auto_mapping.none");
    }
}
