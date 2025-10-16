package org.geysermc.rainbow.mixin;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.SpriteLoader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.concurrent.Executor;

@Mixin(SpriteLoader.class)
public interface SpriteLoaderAccessor {

    @Invoker
    SpriteLoader.Preparations invokeStitch(List<SpriteContents> contents, int mipLevel, Executor executor);
}
