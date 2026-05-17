package org.geysermc.rainbow.mapping;

import com.mojang.serialization.Codec;
import net.minecraft.util.ProblemReporter;
import org.geysermc.rainbow.pack.PackPaths;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

public record PackSerializingContext(AssetResolver assetResolver, PackSerializer serializer, PackPaths paths, ProblemReporter reporter) {

    public <T> CompletableFuture<?> save(Codec<T> codec, Optional<T> optional, Function<PackPaths, Path> pathResolver) {
        return optional.map(object -> save(codec, object, pathResolver)).orElseGet(PackSerializer::noop);
    }

    public <T> CompletableFuture<?> save(Codec<T> codec, T object, Function<PackPaths, Path> pathGetter) {
        return save(codec, object, (paths, _) -> pathGetter.apply(paths));
    }

    public <T> CompletableFuture<?> save(Codec<T> codec, Optional<T> optional, BiFunction<PackPaths, T, Path> pathResolver) {
        return optional.map(object -> save(codec, object, pathResolver)).orElseGet(PackSerializer::noop);
    }

    public <T> CompletableFuture<?> save(Codec<T> codec, T object, BiFunction<PackPaths, T, Path> pathResolver) {
        return serializer.saveJson(codec, object, pathResolver.apply(paths, object));
    }
}
