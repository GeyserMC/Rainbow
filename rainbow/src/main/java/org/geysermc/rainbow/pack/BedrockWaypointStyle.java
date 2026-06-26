package org.geysermc.rainbow.pack;

import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;
import org.geysermc.rainbow.mapping.texture.TextureHolder;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public record BedrockWaypointStyle(List<TextureHolder> sprites) implements PackSerializer.Serializable {

    @Override
    public CompletableFuture<?> save(PackSerializingContext context) {
        return PackSerializer.Serializable.allOf(sprites).save(context);
    }
}
