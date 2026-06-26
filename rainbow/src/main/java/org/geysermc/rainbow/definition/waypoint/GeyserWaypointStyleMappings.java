package org.geysermc.rainbow.definition.waypoint;

import com.mojang.serialization.Codec;
import net.minecraft.client.resources.WaypointStyle;
import net.minecraft.resources.Identifier;
import org.geysermc.rainbow.definition.AbstractGeyserMappings;

import java.util.Map;

public final class GeyserWaypointStyleMappings extends AbstractGeyserMappings<Identifier, WaypointStyle> {
    public static final Codec<GeyserWaypointStyleMappings> CODEC = AbstractGeyserMappings.createCodec("waypoint_styles", 1,
            Identifier.CODEC, WaypointStyle.CODEC, GeyserWaypointStyleMappings::new);

    public GeyserWaypointStyleMappings() {}

    private GeyserWaypointStyleMappings(Map<Identifier, WaypointStyle> mappings) {
        super(mappings);
    }

    @Override
    public void map(Identifier key, WaypointStyle value) {
        super.map(key, value);
    }
}
