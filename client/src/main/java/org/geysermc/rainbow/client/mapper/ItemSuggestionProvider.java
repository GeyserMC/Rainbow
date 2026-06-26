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

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.world.item.ItemStackTemplate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

// TODO safety
public class ItemSuggestionProvider implements CustomItemProvider {
    private final List<String> remainingCommands;
    private boolean waitingOnItem = false;
    private boolean waitingOnClear = false;

    public ItemSuggestionProvider(List<String> commands) {
        remainingCommands = new ArrayList<>(commands);
    }

    public Collection<ItemStackTemplate> nextItems(LocalPlayer player, ClientPacketListener connection) {
        if (!remainingCommands.isEmpty() || waitingOnItem) {
            if (waitingOnClear && player.getInventory().isEmpty()) {
                waitingOnClear = false;
            } else if (!waitingOnItem) {
                connection.send(new ServerboundChatCommandPacket(remainingCommands.removeFirst()));
                waitingOnItem = true;
            } else {
                if (!player.getInventory().isEmpty()) {
                    Collection<ItemStackTemplate> items = player.getInventory().getNonEquipmentItems().stream()
                            .filter(stack -> !stack.isEmpty())
                            .map(ItemStackTemplate::fromNonEmptyStack)
                            .toList();
                    connection.send(new ServerboundChatCommandPacket("clear"));

                    waitingOnItem = false;
                    if (!remainingCommands.isEmpty()) {
                        waitingOnClear = true;
                    }
                    return items;
                }
            }
        }
        return List.of();
    }

    public int queueSize() {
        return remainingCommands.size();
    }

    @Override
    public boolean isDone() {
        return remainingCommands.isEmpty() && !waitingOnItem && !waitingOnClear;
    }
}
