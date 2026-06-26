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
