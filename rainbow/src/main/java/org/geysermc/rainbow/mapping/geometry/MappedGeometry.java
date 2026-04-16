package org.geysermc.rainbow.mapping.geometry;

import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;
import org.geysermc.rainbow.mapping.texture.TextureHolder;

import java.util.concurrent.CompletableFuture;

public interface MappedGeometry extends PackSerializer.Serializable {

    String identifier();

    TextureHolder stitchedTextures();

    TextureHolder icon();

    default CachedGeometry cachedCopy() {
        if (this instanceof CachedGeometry cached) {
            return cached;
        }
        return new CachedGeometry(identifier(), TextureHolder.createCopy(stitchedTextures()), TextureHolder.createCopy(icon()));
    }

    record CachedGeometry(String identifier, TextureHolder stitchedTextures, TextureHolder icon) implements MappedGeometry {

        @Override
        public CompletableFuture<?> save(PackSerializingContext context) {
            return PackSerializer.noop();
        }
    }
}
