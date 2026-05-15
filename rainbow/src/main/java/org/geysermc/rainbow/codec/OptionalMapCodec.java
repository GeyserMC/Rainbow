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
