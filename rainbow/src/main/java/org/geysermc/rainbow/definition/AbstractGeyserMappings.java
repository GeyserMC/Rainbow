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

package org.geysermc.rainbow.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import org.geysermc.rainbow.CodecUtil;
import org.geysermc.rainbow.stats.PackStats;

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

public abstract class AbstractGeyserMappings<K, V> implements PackStats.Holder {
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

    @Override
    public int stat() {
        return size();
    }
}
