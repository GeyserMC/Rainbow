package org.geysermc.rainbow.pack;

import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;
import org.geysermc.rainbow.mapping.geometry.MappedGeometry;
import org.geysermc.rainbow.mapping.texture.BlockModelTextures;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record BedrockBlock(BlockModelTextures textures, Optional<MappedGeometry> geometry) implements PackSerializer.Serializable {

    @Override
    public CompletableFuture<?> save(PackSerializingContext context) {
        return textures
                .with(geometry)
                .save(context);
    }
}
