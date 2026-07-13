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

package org.geysermc.rainbow.client.accessor;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

// Implemented on ModelManager, since this class doesn't keep the resolved models or unbaked client items after baking, we have to store them manually.
// This comes with some extra memory usage, but Rainbow should only be used to convert packs, so it should be fine
public interface ResolvedModelAccessor {

    Optional<BlockStateModel.UnbakedRoot> rainbow$getBlockStateModel(BlockState state);

    Optional<ResolvedModel> rainbow$getResolvedModel(Identifier identifier);

    Optional<ClientItem> rainbow$getClientItem(Identifier identifier);
}
