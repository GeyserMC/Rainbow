package org.geysermc.rainbow.client.mapper;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.stream.Stream;

public interface CustomItemProvider {

    Stream<ItemStack> nextItems(LocalPlayer player, ClientPacketListener connection);

    boolean isDone();

    Component name();
}
