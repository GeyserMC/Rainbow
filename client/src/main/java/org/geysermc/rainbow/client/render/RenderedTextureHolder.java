package org.geysermc.rainbow.client.render;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.pip.OversizedItemRenderer;
import net.minecraft.client.gui.render.state.GuiItemRenderState;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.render.state.pip.OversizedItemRenderState;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.client.mixin.PictureInPictureRendererAccessor;
import org.geysermc.rainbow.image.NativeImageUtil;
import org.geysermc.rainbow.mapping.AssetResolver;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.texture.TextureHolder;
import org.joml.Matrix3x2fStack;

import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RenderedTextureHolder extends TextureHolder {
    private final ItemStack stackToRender;

    public RenderedTextureHolder(Identifier identifier, ItemStack stackToRender) {
        super(identifier);
        this.stackToRender = stackToRender;
    }

    @Override
    public Optional<byte[]> load(AssetResolver assetResolver, ProblemReporter reporter) {
        throw new UnsupportedOperationException("Rendered texture does not support loading");
    }

    @Override
    public CompletableFuture<?> save(AssetResolver assetResolver, PackSerializer serializer, Path path, ProblemReporter reporter) {
        TrackingItemStackRenderState itemRenderState = new TrackingItemStackRenderState();
        Minecraft.getInstance().getItemModelResolver().updateForTopItem(itemRenderState, stackToRender, ItemDisplayContext.GUI, null, null, 0);
        itemRenderState.setOversizedInGui(true);

        GuiItemRenderState guiItemRenderState = new GuiItemRenderState("geometry_render", new Matrix3x2fStack(16), itemRenderState, 0, 0, null);
        ScreenRectangle sizeBounds = guiItemRenderState.oversizedItemBounds();
        Objects.requireNonNull(sizeBounds);
        OversizedItemRenderState oversizedRenderState = new OversizedItemRenderState(guiItemRenderState, sizeBounds.left(), sizeBounds.top(), sizeBounds.right() + 4, sizeBounds.bottom() + 4);

        Lock lock = new ReentrantLock();
        Condition condition = lock.newCondition();

        try (OversizedItemRenderer itemRenderer = new OversizedItemRenderer(Minecraft.getInstance().renderBuffers().bufferSource())) {
            //noinspection DataFlowIssue
            ((PictureInPictureCopyRenderer) itemRenderer).rainbow$allowTextureCopy();
            itemRenderer.prepare(oversizedRenderState, new GuiRenderState(), 4);
            writeAsPNG(serializer, path, ((PictureInPictureRendererAccessor) itemRenderer).getTexture(), lock, condition);
        }

        return CompletableFuture.runAsync(() -> {
            lock.lock();
            try {
                condition.await();
            } catch (InterruptedException ignored) {
            } finally {
                lock.unlock();
            }
        });
    }

    // Simplified TextureUtil#writeAsPNG with some modifications to flip the image and just generate it at full size
    private static void writeAsPNG(PackSerializer serializer, Path path, GpuTexture texture, Lock lock, Condition condition) {
        RenderSystem.assertOnRenderThread();

        int width = texture.getWidth(0);
        int height = texture.getHeight(0);
        int bufferSize = texture.getFormat().pixelSize() * width * height;

        GpuBuffer buffer = RenderSystem.getDevice().createBuffer(() -> "Texture output buffer", GpuBuffer.USAGE_COPY_DST | GpuBuffer.USAGE_MAP_READ, bufferSize);
        CommandEncoder commandEncoder = RenderSystem.getDevice().createCommandEncoder();

        Runnable writer = () -> {
            try (GpuBuffer.MappedView mappedView = commandEncoder.mapBuffer(buffer, true, false)) {
                RainbowIO.safeIO(() -> {
                    try (NativeImage image = new NativeImage(width, height, false)) {
                        for (int y = 0; y < height; y++) {
                            for (int x = 0; x < width; x++) {
                                int colour = mappedView.data().getInt((x + y * width) * texture.getFormat().pixelSize());
                                image.setPixelABGR(x, height - y - 1, colour);
                            }
                        }

                        serializer.saveTexture(NativeImageUtil.writeToByteArray(image), path).join();
                        lock.lock();
                        try {
                            condition.signalAll();
                        } finally {
                            lock.unlock();
                        }
                    }
                });
            } finally {
                buffer.close();
            }
        };
        commandEncoder.copyTextureToBuffer(texture, buffer, 0, writer, 0);
    }
}
