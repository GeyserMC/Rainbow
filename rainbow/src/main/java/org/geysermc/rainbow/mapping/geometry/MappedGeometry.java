package org.geysermc.rainbow.mapping.geometry;

import org.geysermc.rainbow.mapping.PackAssetCache;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;

import java.util.concurrent.CompletableFuture;

public interface MappedGeometry extends PackAssetCache.Cacheable<MappedGeometry>, PackSerializer.Serializable {

    String identifier();

    @Override
    default CachedGeometry cachedCopy() {
        if (this instanceof CachedGeometry cached) {
            return cached;
        }
        return new CachedGeometry(identifier());
    }

    record CachedGeometry(String identifier) implements MappedGeometry {

        @Override
        public CompletableFuture<?> save(PackSerializingContext context) {
            return PackSerializer.noop();
        }
    }
}
