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

public record BedrockTextureAtlas(String resourcePackName, String atlasName, BedrockTextures textures) {
    public static final String ITEM_ATLAS = "atlas.items";
    public static final String TERRAIN_ATLAS = "atlas.terrain";
    public static final Codec<BedrockTextureAtlas> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("resource_pack_name").forGetter(BedrockTextureAtlas::resourcePackName),
                    Codec.STRING.fieldOf("texture_name").forGetter(BedrockTextureAtlas::atlasName),
                    BedrockTextures.CODEC.fieldOf("texture_data").forGetter(BedrockTextureAtlas::textures)
            ).apply(instance, BedrockTextureAtlas::new)
    );

    public static BedrockTextureAtlas itemAtlas(String resourcePackName, BedrockTextures.Builder textures) {
        return new BedrockTextureAtlas(resourcePackName, ITEM_ATLAS, textures.build());
    }

    public static BedrockTextureAtlas terrainAtlas(String resourcePackName, BedrockTextures.Builder textures) {
        return new BedrockTextureAtlas(resourcePackName, TERRAIN_ATLAS, textures.build());
    }
}
