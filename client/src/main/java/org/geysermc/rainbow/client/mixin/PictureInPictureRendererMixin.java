package org.geysermc.rainbow.client.mixin;

import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer;
import org.geysermc.rainbow.client.render.PictureInPictureCopyRenderer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

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

    @ModifyConstant(method = "prepareTexturesAndProjection", constant = @Constant(intValue = 13))
    public int allowUsageCopySrc(int usage) {
        return allowTextureCopy ? usage | GpuTexture.USAGE_COPY_SRC : usage;
    }
}
