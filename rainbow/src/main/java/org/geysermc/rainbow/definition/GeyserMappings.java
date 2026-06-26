package org.geysermc.rainbow.definition;

import org.geysermc.rainbow.definition.block.GeyserBlockMappings;
import org.geysermc.rainbow.definition.item.GeyserItemMappings;
import org.geysermc.rainbow.definition.skull.GeyserSkullMappings;
import org.geysermc.rainbow.definition.waypoint.GeyserWaypointStyleMappings;
import org.geysermc.rainbow.stats.PackStatKeys;
import org.geysermc.rainbow.stats.PackStats;

public record GeyserMappings(GeyserBlockMappings blocks, GeyserItemMappings items,
                             GeyserSkullMappings skulls, GeyserWaypointStyleMappings waypointStyles) implements PackStats.Aggregator {

    public GeyserMappings() {
        this(new GeyserBlockMappings(), new GeyserItemMappings(),
                new GeyserSkullMappings(), new GeyserWaypointStyleMappings());
    }

    @Override
    public void collectStats(PackStats.Collector collector) {
        collector.collect(PackStatKeys.BLOCK_MAPPINGS, blocks)
                .collect(PackStatKeys.ITEM_MAPPINGS, items)
                .collect(PackStatKeys.SKULL_MAPPINGS, skulls)
                .collect(PackStatKeys.WAYPOINT_MAPPINGS, waypointStyles);
    }
}
