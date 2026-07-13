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

package org.geysermc.rainbow.client.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import org.geysermc.rainbow.client.render.PictureInPictureCopyRenderer;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PictureInPictureRenderer.class)
public abstract class PictureInPictureRendererMixin implements AutoCloseable, PictureInPictureCopyRenderer {

    @Shadow
    private @Nullable GpuTexture texture;

    @Unique
    private boolean allowTextureCopy = false;

    @Override
    public void rainbow$allowTextureCopy() {
        if (texture != null) {
            throw new IllegalStateException("texture already created");
        }
        allowTextureCopy = true;
    }

    @ModifyExpressionValue(method = "prepareTexturesAndProjection", at = @At(value = "CONSTANT", args = "intValue=13"))
    public int allowUsageCopySrc(int usage) {
        return allowTextureCopy ? usage | GpuTexture.USAGE_COPY_SRC : usage;
    }
}
