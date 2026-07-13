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

package org.geysermc.rainbow.codec;

import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class OptionalMapCodec<A> extends MapCodec<Optional<A>> {
    private final MapCodec<A> delegate;

    public OptionalMapCodec(MapCodec<A> delegate) {
        this.delegate = delegate;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return delegate.keys(ops);
    }

    @Override
    public <T> DataResult<Optional<A>> decode(DynamicOps<T> ops, MapLike<T> input) {
        // If no keys are present, empty optional, else try parse
        return keys(ops).map(input::get).allMatch(Objects::isNull) ? DataResult.success(Optional.empty()) : delegate.decode(ops, input).map(Optional::of);
    }

    @Override
    public <T> RecordBuilder<T> encode(Optional<A> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        return input.map(value -> delegate.encode(value, ops, prefix)).orElse(prefix);
    }

    public static <A> MapCodec<Optional<A>> of(MapCodec<A> mapCodec) {
        return new OptionalMapCodec<>(mapCodec);
    }
}
