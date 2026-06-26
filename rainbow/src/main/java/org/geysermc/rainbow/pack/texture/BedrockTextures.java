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

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import org.geysermc.rainbow.mapping.texture.BlockModelTextures;
import org.geysermc.rainbow.pack.BedrockBlock;
import org.geysermc.rainbow.pack.BedrockItem;
import org.geysermc.rainbow.stats.PackStats;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record BedrockTextures(Map<String, String> textures) implements PackStats.Holder {
    public static final Codec<BedrockTextures> CODEC =
            Codec.compoundList(Codec.STRING, Codec.compoundList(Codec.STRING, Codec.STRING))
                    .xmap(pairs -> pairs.stream().map(pair -> Pair.of(pair.getFirst(), pair.getSecond().getFirst().getSecond())).collect(Pair.toMap()),
                            map -> map.entrySet().stream().map(entry -> Pair.of(entry.getKey(), List.of(Pair.of("textures", entry.getValue())))).toList())
                    .xmap(BedrockTextures::new, BedrockTextures::textures);
    public static final String TEXTURES_FOLDER = "textures/";

    public Builder toBuilder() {
        Builder builder = builder();
        builder.textures.putAll(textures);
        return builder;
    }

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

    public static class Builder {
        private final Map<String, String> textures = new HashMap<>();

        public Builder withBlockTextures(BedrockBlock block) {
            if (!block.textures().cached()) {
                block.textures().materials().ifLeft(this::withBlockMaterial);
                block.textures().materials().ifRight(infos -> infos.values().forEach(this::withBlockMaterial));
            }
            return this;
        }

        private void withBlockMaterial(BlockModelTextures.MaterialInfo info) {
            withTexture(info.texture().bedrockSafeName(), info.texture().bedrockSafeDestination());
        }

        public Builder withItemTexture(BedrockItem item) {
            String iconName = item.icon().bedrockSafeName();
            if (!textures.containsKey(iconName)) {
                return withTexture(iconName, item.icon().bedrockSafeDestination());
            }
            return this;
        }

        public Builder withTexture(String name, String texture) {
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Name cannot be empty");
            }
            String currentTexture = textures.get(name);
            if (currentTexture != null) {
                if (!texture.equals(currentTexture)) {
                    throw new IllegalArgumentException("Texture conflict (name=" + name + ", existing=" + currentTexture + ", new=" + texture +  ")");
                }
            } else {
                textures.put(name, texture);
            }
            return this;
        }

        public BedrockTextures build() {
            return new BedrockTextures(Map.copyOf(textures));
        }
    }
}
