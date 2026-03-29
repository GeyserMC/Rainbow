package org.geysermc.rainbow.client.mapper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.geysermc.rainbow.client.PackManager;
import org.geysermc.rainbow.pack.BedrockPack;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class PackMapper {
    private final PackManager packManager;
    private @Nullable CustomItemProvider itemProvider;

    public PackMapper(PackManager packManager) {
        this.packManager = packManager;
    }

    public void setItemProvider(@Nullable CustomItemProvider itemProvider) {
        this.itemProvider = itemProvider;
    }

    public void tick(Minecraft minecraft) {
        if (itemProvider != null) {
            LocalPlayer player = Objects.requireNonNull(minecraft.player);
            ClientPacketListener connection = Objects.requireNonNull(minecraft.getConnection());

            packManager.runOrElse(pack -> {
                // TODO maybe report problems here... probably better to do so in pack class though
                long mapped = itemProvider.nextItems(player, connection)
                        .map(pack::map)
                        .filter(result -> result != BedrockPack.MappingResult.NONE_MAPPED)
                        .count();
                if (mapped != 0) {
                    player.sendSystemMessage(Component.translatable("chat.rainbow.mapped_items", mapped));
                }
                if (itemProvider.isDone()) {
                    player.sendSystemMessage(Component.translatable("chat.rainbow.automatic_mapping_finished"));
                    itemProvider = null;
                }
            }, () -> itemProvider = null);
        }
    }
}
