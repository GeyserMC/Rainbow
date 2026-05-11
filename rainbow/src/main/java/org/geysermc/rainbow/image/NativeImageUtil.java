package org.geysermc.rainbow.image;

import com.mojang.blaze3d.platform.NativeImage;
import org.geysermc.rainbow.mixin.NativeImageAccessor;
import org.lwjgl.stb.STBImage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;

public class NativeImageUtil {

    // Adjusted NativeImage#writeToFile
    @SuppressWarnings("DataFlowIssue")
    public static byte[] writeToByteArray(NativeImage image) throws IOException {
        if (!image.format().supportedByStb()) {
            throw new UnsupportedOperationException("Don't know how to write format " + image.format());
        } else {
            ((NativeImageAccessor) (Object) image).invokeCheckAllocated();
            try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                if (!((NativeImageAccessor) (Object) image).invokeWriteToChannel(Channels.newChannel(output))) {
                    throw new IOException("Could not write image to pipe: " + STBImage.stbi_failure_reason());
                }
                return output.toByteArray();
            }
        }
    }
}
