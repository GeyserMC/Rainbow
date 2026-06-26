package org.geysermc.rainbow.definition.waypoint;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.resources.WaypointStyle;
import net.minecraft.resources.Identifier;
import org.geysermc.rainbow.CodecUtil;

import java.util.Collections;
import java.util.Map;

public final class GeyserWaypointStyleMappings {
    public static final Codec<GeyserWaypointStyleMappings> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CodecUtil.unitVerifyCodec(Codec.INT, "format_version", 1),
                    Codec.unboundedMap(Identifier.CODEC, WaypointStyle.CODEC).fieldOf("waypoint_styles").forGetter(GeyserWaypointStyleMappings::mappings)
            ).apply(instance, (_, mappings) -> new GeyserWaypointStyleMappings(mappings))
    );

    private final Map<Identifier, WaypointStyle> mappings = new Object2ObjectOpenHashMap<>();

    public GeyserWaypointStyleMappings() {}

    private GeyserWaypointStyleMappings(Map<Identifier, WaypointStyle> mappings) {
        this.mappings.putAll(mappings);
    }

    public void map(Identifier identifier, WaypointStyle style) {
        if (mappings.containsKey(identifier)) {
            throw new IllegalStateException("tried to map waypoint style for " + identifier + " twice");
        }
        mappings.put(identifier, style);
    }

    public Map<Identifier, WaypointStyle> mappings() {
        return Collections.unmodifiableMap(mappings);
    }

    public int size() {
        return mappings.size();
    }
}
