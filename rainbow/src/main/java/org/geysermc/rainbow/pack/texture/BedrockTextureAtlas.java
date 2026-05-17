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
