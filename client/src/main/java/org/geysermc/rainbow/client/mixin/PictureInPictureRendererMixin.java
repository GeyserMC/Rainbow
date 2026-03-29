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
