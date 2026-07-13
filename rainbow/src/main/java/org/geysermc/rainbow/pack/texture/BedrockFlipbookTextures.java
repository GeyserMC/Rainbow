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

package org.geysermc.rainbow.pack.texture;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.IntList;
import org.geysermc.rainbow.stats.PackStats;

import java.util.ArrayList;
import java.util.List;

public record BedrockFlipbookTextures(List<FlipbookTexture> textures) implements PackStats.Holder {
    public static final Codec<BedrockFlipbookTextures> CODEC = FlipbookTexture.CODEC.listOf().xmap(BedrockFlipbookTextures::new, BedrockFlipbookTextures::textures);

    public int size() {
        return textures.size();
    }

    @Override
    public int stat() {
        return size();
    }

    public static Builder builder() {
        return new Builder();
    }

    public record FlipbookTexture(String name, String path, int ticksPerFrame, IntList frames, int replicate, boolean interpolate) {
        private static final Codec<IntList> INT_LIST_CODEC = Codec.INT.listOf().xmap(
                list -> IntList.of(list.stream().mapToInt(Integer::intValue).toArray()),
                ints -> ints.intStream().boxed().toList());
        public static final Codec<FlipbookTexture> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("atlas_tile").forGetter(FlipbookTexture::name),
                        Codec.STRING.fieldOf("flipbook_texture").forGetter(FlipbookTexture::path),
                        Codec.INT.optionalFieldOf("ticks_per_frame", 1).forGetter(FlipbookTexture::ticksPerFrame),
                        INT_LIST_CODEC.fieldOf("frames").forGetter(FlipbookTexture::frames),
                        Codec.INT.optionalFieldOf("replicate", 1).forGetter(FlipbookTexture::replicate),
                        Codec.BOOL.optionalFieldOf("blend_frames", true).forGetter(FlipbookTexture::interpolate)
                ).apply(instance, FlipbookTexture::new)
        );
    }

    public static class Builder {
        private final List<FlipbookTexture> textures = new ArrayList<>();

        public Builder with(FlipbookTexture texture) {
            textures.add(texture);
            return this;
        }

        public BedrockFlipbookTextures build() {
            return new BedrockFlipbookTextures(List.copyOf(textures));
        }
    }
}
