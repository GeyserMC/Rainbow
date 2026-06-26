package org.geysermc.rainbow.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.geysermc.rainbow.CodecUtil;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

public abstract class AbstractGeyserMappings<K, V> {
    private final Map<K, V> mappings = new Object2ObjectOpenHashMap<>();

    protected AbstractGeyserMappings() {}

    protected AbstractGeyserMappings(Map<K, V> mappings) {
        this.mappings.putAll(mappings);
    }

    protected void map(K key, V value) {
        mappings.put(key, value);
    }

    protected static <K, V, T extends AbstractGeyserMappings<K, V>> Codec<T> createCodec(String type, int formatVersion,
                                                                                         Codec<K> keyCodec, Codec<V> valueCodec,
                                                                                         Function<Map<K, V>, T> constructor) {
        return RecordCodecBuilder.create(instance ->
                instance.group(
                        CodecUtil.unitVerifyCodec(Codec.INT, "format_version", formatVersion),
                        Codec.unboundedMap(keyCodec, valueCodec).fieldOf(type).forGetter(T::mappings)
                ).apply(instance, (_, mappings) -> constructor.apply(mappings))
        );
    }

    public Map<K, V> mappings() {
        return Collections.unmodifiableMap(mappings);
    }

    public int size() {
        return mappings.size();
    }
}
