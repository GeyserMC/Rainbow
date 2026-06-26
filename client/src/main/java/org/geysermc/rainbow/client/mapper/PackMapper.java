/*
 * Copyright (c) 2026 GeyserMC. https://geysermc.org
 *
 * This file is part of Rainbow.
 *
 * Rainbow is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * Rainbow is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * Rainbow. If not, see <https://www.gnu.org/licenses/>.
 */

package org.geysermc.rainbow.client.mapper;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.ResolvableProfile;
import org.geysermc.rainbow.client.PackManager;
import org.geysermc.rainbow.pack.BedrockPack;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
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
                MappingResults results = mapItems(pack, itemProvider.nextItems(player, connection));
                if (results.itemsMapped > 0) {
                    player.sendSystemMessage(Component.translatable("chat.rainbow.mapped_items", results.itemsMapped));
                }
                if (results.skullsMapped > 0) {
                    player.sendSystemMessage(Component.translatable("chat.rainbow.mapped_skulls", results.skullsMapped));
                }
                if (results.problems > 0) {
                    player.sendSystemMessage(Component.translatable("chat.rainbow.mapped_problems").withStyle(ChatFormatting.RED));
                }
                if (itemProvider.isDone()) {
                    player.sendSystemMessage(Component.translatable("chat.rainbow.automatic_mapping_finished"));
                    itemProvider = null;
                }
            }, () -> itemProvider = null);
        }
    }

    public MappingResults mapItems(BedrockPack pack, Collection<ItemStackTemplate> items) {
        long skullsMapped = items.stream()
                .filter(stack -> {
                    ResolvableProfile profile = stack.get(DataComponents.PROFILE);
                    return profile != null && pack.mapSkull(profile) == BedrockPack.MappingResult.MAPPED_SUCCESSFULLY;
                })
                .count();

        int itemsMapped = 0;
        int problems = 0;
        for (ItemStackTemplate stack : items) {
            switch (pack.mapItem(stack)) {
                case MAPPED_SUCCESSFULLY -> itemsMapped++;
                case PROBLEMS_OCCURRED -> {
                    itemsMapped++;
                    problems++;
                }
            }
        }
        return new MappingResults(itemsMapped, (int) skullsMapped, problems);
    }

    public record MappingResults(int itemsMapped, int skullsMapped, int problems) {

        public BedrockPack.MappingResult toSingleResult() {
            return problems > 0 ? BedrockPack.MappingResult.PROBLEMS_OCCURRED : itemsMapped > 0 || skullsMapped > 0 ? BedrockPack.MappingResult.MAPPED_SUCCESSFULLY : BedrockPack.MappingResult.NONE_MAPPED;
        }
    }
}
