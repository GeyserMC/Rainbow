/*
 * Copyright (c) 2026 GeyserMC. https://geysermc.org
 *
 * This file is part of Rainbow.
 *
 * Rainbow is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * Rainbow is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * Rainbow. If not, see <https://www.gnu.org/licenses/>.
 */

package org.geysermc.rainbow.mapping;

import com.mojang.serialization.Codec;
import net.minecraft.server.packs.resources.Resource;
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

    CompletableFuture<?> saveResource(Resource resource, Path path);

    @FunctionalInterface
    interface Serializable {

        CompletableFuture<?> save(PackSerializingContext context);

        default Serializable with(Serializable other) {
            return context -> CompletableFuture.allOf(save(context), other.save(context));
        }

        default <T> Serializable with(Codec<T> codec, T object, Function<PackPaths, Path> pathGetter) {
            return with(wrapCodec(codec, object, pathGetter));
        }

        default <T> Serializable with(Codec<T> codec, T object, BiFunction<PackPaths, T, Path> pathResolver) {
            return with(wrapCodec(codec, object, pathResolver));
        }

        default Serializable with(Optional<? extends Serializable> other) {
            return other.map(this::with).orElse(this);
        }

        default <T> Serializable with(Codec<T> codec, Optional<T> optional, Function<PackPaths, Path> pathGetter) {
            return with(wrapOptionalCodec(codec, optional, pathGetter));
        }

        default <T> Serializable with(Codec<T> codec, Optional<T> optional, BiFunction<PackPaths, T, Path> pathResolver) {
            return with(wrapOptionalCodec(codec, optional, pathResolver));
        }

        default Serializable with(Collection<? extends Serializable> others) {
            return with(allOf(others));
        }

        static Serializable wrapOptional(Optional<? extends Serializable> optional) {
            return context -> optional.map(serializable -> serializable.save(context)).orElseGet(PackSerializer::noop);
        }

        static <T> Serializable wrapCodec(Codec<T> codec, T object, Function<PackPaths, Path> pathGetter) {
            return context -> context.save(codec, object, pathGetter);
        }

        static <T> Serializable wrapCodec(Codec<T> codec, T object, BiFunction<PackPaths, T, Path> pathResolver) {
            return context -> context.save(codec, object, pathResolver);
        }

        static <T> Serializable wrapOptionalCodec(Codec<T> codec, Optional<T> optional, Function<PackPaths, Path> pathGetter) {
            return context -> context.save(codec, optional, pathGetter);
        }

        static <T> Serializable wrapOptionalCodec(Codec<T> codec, Optional<T> optional, BiFunction<PackPaths, T, Path> pathResolver) {
            return context -> context.save(codec, optional, pathResolver);
        }

        static Serializable allOf(Collection<? extends Serializable> serializables) {
            return context -> CompletableFuture.allOf(serializables.stream().map(serializable -> serializable.save(context)).toArray(CompletableFuture[]::new));
        }

        static Serializable noop() {
            return _ -> PackSerializer.noop();
        }
    }

    static <T> CompletableFuture<@Nullable T> noop() {
        return CompletableFuture.completedFuture(null);
    }
}
