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
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import org.geysermc.rainbow.datagen.accessor.LanguageProviderDataAccessor;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;

@Mixin(FabricLanguageProvider.class)
public abstract class FabricLanguageProviderMixin implements DataProvider, LanguageProviderDataAccessor {
    @Unique
    private @Nullable Map<String, String> translationEntries = null;

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;thenCompose(Ljava/util/function/Function;)Ljava/util/concurrent/CompletableFuture;"))
    public void setTranslationEntries(CachedOutput output, CallbackInfoReturnable<CompletableFuture<?>> callbackInfoReturnable,
                                      @Local(name = "translationEntries") TreeMap<String, String> translationEntries) {
        this.translationEntries = translationEntries;
    }

    @Override
    public Map<String, String> rainbow$getTranslationEntries() {
        if (translationEntries == null) {
            throw new IllegalStateException("FabricLanguageProvider has not run yet");
        }
        return translationEntries;
    }
}
