package org.geysermc.rainbow.mapping;

import com.mojang.serialization.Codec;
import org.geysermc.rainbow.pack.PackPaths;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface PackSerializer {

    <T> CompletableFuture<?> saveJson(Codec<T> codec, T object, Path path);

    CompletableFuture<?> saveTexture(byte[] texture, Path path);

    @FunctionalInterface
    interface Serializable {

        CompletableFuture<?> save(PackSerializingContext context);

        default Serializable with(Serializable other) {
            return context -> CompletableFuture.allOf(save(context), other.save(context));
        }

        default Serializable with(Optional<? extends Serializable> other) {
            return other.map(this::with).orElse(this);
        }

        default Serializable with(Collection<? extends Serializable> others) {
            return with(context -> CompletableFuture.allOf(others.stream().map(serializable -> serializable.save(context)).toArray(CompletableFuture[]::new)));
        }

        static Serializable wrapOptional(Optional<? extends Serializable> optional) {
            return context -> optional.map(serializable -> serializable.save(context)).orElseGet(PackSerializer::noop);
        }

        static <T> Serializable wrapCodec(Codec<T> codec, T object, Function<PackPaths, Path> pathGetter) {
            return wrapCodec(codec, object, (paths, _) -> pathGetter.apply(paths));
        }

        static <T> Serializable wrapCodec(Codec<T> codec, T object, BiFunction<PackPaths, T, Path> pathResolver) {
            return context -> context.serializer().saveJson(codec, object, pathResolver.apply(context.paths(), object));
        }

        static <T> Serializable wrapOptionalCodec(Codec<T> codec, Optional<T> optional, Function<PackPaths, Path> pathGetter) {
            return wrapOptionalCodec(codec, optional, (paths, _) -> pathGetter.apply(paths));
        }

        static <T> Serializable wrapOptionalCodec(Codec<T> codec, Optional<T> optional, BiFunction<PackPaths, T, Path> pathResolver) {
            return optional.map(object -> wrapCodec(codec, object, pathResolver)).orElseGet(Serializable::noop);
        }

        static Serializable noop() {
            return _ -> PackSerializer.noop();
        }
    }

    static <T> CompletableFuture<@Nullable T> noop() {
        return CompletableFuture.completedFuture(null);
    }
}
