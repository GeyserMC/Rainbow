package org.geysermc.rainbow.mapping.geometry;

import org.geysermc.rainbow.mapping.PackSerializingContext;
import org.geysermc.rainbow.pack.geometry.BedrockGeometry;

import java.util.concurrent.CompletableFuture;

public record MappedGeometryInstance(BedrockGeometry geometry) implements MappedGeometry {

    @Override
    public String identifier() {
        return geometry.definitions().getFirst().info().identifier();
    }

    @Override
    public CompletableFuture<?> save(PackSerializingContext context) {
        return context.serializer().saveJson(BedrockGeometry.CODEC, geometry, context.paths().geometryPath(identifier()));
    }
}
