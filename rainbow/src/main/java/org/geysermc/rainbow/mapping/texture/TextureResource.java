package org.geysermc.rainbow.mapping.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.resources.metadata.animation.AnimationFrame;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.metadata.animation.FrameSize;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public record TextureResource(NativeImage texture, Optional<FrameSize> frameSize, List<FrameInfo> frames,
                              int frameRowCount, int totalFrameCount) implements AutoCloseable {

    public static TextureResource createAnimated(NativeImage texture, AnimationMetadataSection animation) {
        // Inspired by: SpriteContents#createAnimatedTexture

        FrameSize frameSize = animation.calculateFrameSize(texture.getWidth(), texture.getHeight());
        int frameRowCount = texture.getWidth() / frameSize.width();
        int frameColCount = texture.getHeight() / frameSize.height();
        int totalFrameCount = frameRowCount * frameColCount;
        int defaultFrameTime = animation.defaultFrameTime();

        if (totalFrameCount <= 1) {
            // Early return: 1 frame can never be animated
            return new TextureResource(texture, Optional.empty(), List.of(), 1, totalFrameCount);
        }

        List<FrameInfo> frames = new ArrayList<>();
        if (animation.frames().isEmpty()) {
            for (int i = 0; i < totalFrameCount; i++) {
                frames.add(new FrameInfo(i, defaultFrameTime));
            }
        } else {
            for (AnimationFrame frame : animation.frames().get()) {
                frames.add(new FrameInfo(frame.index(), frame.timeOr(defaultFrameTime)));
            }

            Iterator<FrameInfo> iterator = frames.iterator();
            while (iterator.hasNext()) {
                FrameInfo frame = iterator.next();
                boolean isValid = frame.time > 0 && frame.index >= 0 && frame.index < totalFrameCount;
                if (!isValid) {
                    iterator.remove();
                }
            }
        }

        return new TextureResource(texture, Optional.of(frameSize), Collections.unmodifiableList(frames), frameRowCount, totalFrameCount);
    }

    public static TextureResource createAnimated(NativeImage texture, FrameSize frameSize, List<FrameInfo> frames, int frameRowCount) {
        int frameColCount = texture.getHeight() / frameSize.height();
        return new TextureResource(texture, Optional.of(frameSize), frames, frameRowCount, frameRowCount * frameColCount);
    }

    public static TextureResource createNonAnimated(NativeImage texture) {
        return new TextureResource(texture, Optional.empty(), List.of(), 1, 1);
    }

    public static TextureResource create(NativeImage texture, Optional<AnimationMetadataSection> animationMetadata) {
        return animationMetadata
                .map(animation -> createAnimated(texture, animation))
                .orElseGet(() -> createNonAnimated(texture));
    }

    public NativeImage getFirstFrame() {
        FrameSize size = sizeOfFrame();
        NativeImage firstFrame = new NativeImage(size.width(), size.height(), false);
        firstFrame.copyFrom(texture);
        return firstFrame;
    }

    public FrameInfo getFrameInfo(int index) {
        return frames.get(index);
    }

    public NativeImage getFrame(int index) {
        if (frames.isEmpty()) {
            return getFirstFrame();
        }
        FrameInfo frame = frames.get(index);
        FrameSize size = sizeOfFrame();

        int frameX = getFrameX(frame.index) * size.width();
        int frameY = getFrameY(frame.index) * size.height();

        NativeImage image = new NativeImage(size.width(), size.height(), false);
        texture.copyRect(image, frameX, frameY, 0, 0, size.width(), size.height(), false, false);
        return image;
    }

    public int frameReferenceCount() {
        return Math.max(1, frames.size());
    }

    public FrameSize sizeOfFrame() {
        return frameSize.orElseGet(() -> new FrameSize(texture.getWidth(), texture.getHeight()));
    }

    private int getFrameX(final int index) {
        return index % frameRowCount;
    }

    private int getFrameY(final int index) {
        return index / frameRowCount;
    }

    @Override
    public void close() {
        texture.close();
    }

    public record FrameInfo(int index, int time) {}
}
