package org.geysermc.rainbow.mapping.texture;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@FunctionalInterface
public interface TextureSerializer extends Function<TextureHolder, CompletableFuture<?>> {}
