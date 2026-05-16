package org.geysermc.rainbow.pack;

import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;
import org.geysermc.rainbow.mapping.geometry.MappedGeometry;
import org.geysermc.rainbow.mapping.texture.TextureHolder;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record BedrockBlock(Map<String, TextureHolder> textures, Optional<MappedGeometry> geometry) implements PackSerializer.Serializable {

    @Override
    public CompletableFuture<?> save(PackSerializingContext context) {
        return PackSerializer.Serializable.wrapOptional(geometry)
                .with(textures.values())
                .save(context);
    }
}
