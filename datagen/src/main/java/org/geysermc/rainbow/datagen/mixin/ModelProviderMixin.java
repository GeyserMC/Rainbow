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

package org.geysermc.rainbow.datagen.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.blockstates.BlockModelDefinitionGenerator;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import org.geysermc.rainbow.datagen.accessor.ModelProviderDataAccessor;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Mixin(ModelProvider.class)
public abstract class ModelProviderMixin implements DataProvider, ModelProviderDataAccessor {
    @Unique
    private @Nullable Map<Item, ClientItem> itemInfos = null;
    @Unique
    private @Nullable Map<Block, BlockModelDefinitionGenerator> blockDefinitionGenerators = null;
    @Unique
    private @Nullable Map<Identifier, ModelInstance> models = null;

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/data/models/BlockModelGenerators;run()V"))
    public void setItemInfosAndModels(CachedOutput output, CallbackInfoReturnable<CompletableFuture<?>> callbackInfoReturnable,
                                      @Local(name = "itemModels") ModelProvider.ItemInfoCollector itemModels,
                                      @Local(name = "blockStateGenerators") ModelProvider.BlockStateGeneratorCollector blockStateGenerators,
                                      @Local(name = "simpleModels") ModelProvider.SimpleModelCollector simpleModels) {
        itemInfos = ((ItemInfoCollectorAccessor) itemModels).getItemInfos();
        blockDefinitionGenerators = ((BlockStateGeneratorCollectorAccessor) blockStateGenerators).getGenerators();
        models = ((SimpleModelCollectorAccessor) simpleModels).getModels();
    }

    @Override
    public Map<Item, ClientItem> rainbow$getItemInfos() {
        if (itemInfos == null) {
            throw new IllegalStateException("ModelProvider has not run yet");
        }
        return itemInfos;
    }

    @Override
    public Map<Block, BlockModelDefinitionGenerator> rainbow$getBlockDefinitionGenerators() {
        if (blockDefinitionGenerators == null) {
            throw new IllegalStateException("ModelProvider has not run yet");
        }
        return blockDefinitionGenerators;
    }

    @Override
    public Map<Identifier, ModelInstance> rainbow$getModels() {
        if (models == null) {
            throw new IllegalStateException("ModelProvider has not run yet");
        }
        return models;
    }
}
