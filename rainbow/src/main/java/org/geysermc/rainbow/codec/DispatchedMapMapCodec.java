package org.geysermc.rainbow.codec;

import com.google.common.collect.ImmutableMap;
import com.mojang.datafixers.util.Pair;
import com.mojang.datafixers.util.Unit;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

// Inspired by DFU's DispatchedMapCodec, but for MapCodec
public final class DispatchedMapMapCodec<K, V> extends MapCodec<Map<K, V>> {
    private final Codec<K> keyCodec;
    private final Function<K, Codec<? extends V>> valueCodecFunction;
    private final Keyable keyable;

    public DispatchedMapMapCodec(Codec<K> keyCodec, Function<K, Codec<? extends V>> valueCodecFunction, Keyable keyable) {
        this.keyCodec = keyCodec;
        this.valueCodecFunction = valueCodecFunction;
        this.keyable = keyable;
    }

    public Codec<K> keyCodec() {
        return keyCodec;
    }

    public Function<K, Codec<? extends V>> valueCodecFunction() {
        return valueCodecFunction;
    }

    public Keyable keyable() {
        return keyable;
    }

    @Override
    public <T> Stream<T> keys(DynamicOps<T> ops) {
        return Stream.empty();
    }

    @Override
    public <T> DataResult<Map<K, V>> decode(DynamicOps<T> ops, MapLike<T> input) {
        // Inspired by BaseMapCodec

        Object2ObjectMap<K, V> read = new Object2ObjectArrayMap<>();
        Stream.Builder<Pair<T, T>> failed = Stream.builder();

        DataResult<Unit> result = input.entries().reduce(
                DataResult.success(Unit.INSTANCE, Lifecycle.stable()),
                (r, pair) -> {
                    DataResult<K> key = keyCodec.parse(ops, pair.getFirst());
                    DataResult<? extends V> value = key.map(valueCodecFunction).flatMap(codec -> codec.parse(ops, pair.getSecond()));

                    DataResult<Pair<K, V>> entryResult = key.apply2stable(Pair::of, value);
                    Optional<Pair<K, V>> entry = entryResult.resultOrPartial();
                    if (entry.isPresent()) {
                        V existingValue = read.putIfAbsent(entry.get().getFirst(), entry.get().getSecond());
                        if (existingValue != null) {
                            failed.add(pair);
                            return r.apply2stable((u, _) -> u, DataResult.error(() -> "Duplicate entry for key: '" + entry.get().getFirst() + "'"));
                        }
                    }
                    if (entryResult.isError()) {
                        failed.add(pair);
                    }

                    return r.apply2stable((u, _) -> u, entryResult);
                },
                (r1, r2) -> r1.apply2stable((u1, _) -> u1, r2)
        );

        Map<K, V> elements = ImmutableMap.copyOf(read);
        T errors = ops.createMap(failed.build());

        return result.map(_ -> elements).setPartial(elements).mapError(e -> e + " missed input: " + errors);
    }

    @Override
    public <T> RecordBuilder<T> encode(Map<K, V> input, DynamicOps<T> ops, RecordBuilder<T> prefix) {
        // Inspired by BaseMapCodec

        for (Map.Entry<K, V> entry : input.entrySet()) {
            prefix.add(keyCodec.encodeStart(ops, entry.getKey()), ((Codec) valueCodecFunction.apply(entry.getKey())).encodeStart(ops, entry.getValue()));
        }
        return prefix;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (DispatchedMapMapCodec<?, ?>) obj;
        return Objects.equals(this.keyCodec, that.keyCodec) &&
                Objects.equals(this.valueCodecFunction, that.valueCodecFunction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyCodec, valueCodecFunction);
    }

    @Override
    public String toString() {
        return "DispatchedMapMapCodec[" +
                "keyCodec=" + keyCodec + ", " +
                "valueCodecFunction=" + valueCodecFunction + ']';
    }
}
