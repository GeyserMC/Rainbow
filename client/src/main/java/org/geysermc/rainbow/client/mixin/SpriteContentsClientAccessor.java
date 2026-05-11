package org.geysermc.rainbow.client.mixin;

import net.minecraft.client.renderer.texture.SpriteContents;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SpriteContents.class)
public interface SpriteContentsClientAccessor {

    @Accessor
    SpriteContents.@Nullable AnimatedTexture getAnimatedTexture();
}
