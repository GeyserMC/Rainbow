package org.geysermc.rainbow.client.mapper;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.stream.Stream;

public interface CustomItemProvider {

    Stream<ItemStackTemplate> nextItems(LocalPlayer player, ClientPacketListener connection);

    boolean isDone();
}
