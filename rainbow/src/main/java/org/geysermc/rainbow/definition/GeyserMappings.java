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

import org.geysermc.rainbow.definition.block.GeyserBlockMappings;
import org.geysermc.rainbow.definition.item.GeyserItemMappings;
import org.geysermc.rainbow.definition.waypoint.GeyserWaypointStyleMappings;
import org.geysermc.rainbow.stats.PackStatKeys;
import org.geysermc.rainbow.stats.PackStats;

public record GeyserMappings(GeyserBlockMappings blocks, GeyserItemMappings items,
                             GeyserWaypointStyleMappings waypointStyles) implements PackStats.Aggregator {

    public GeyserMappings() {
        this(new GeyserBlockMappings(), new GeyserItemMappings(), new GeyserWaypointStyleMappings());
    }

    @Override
    public void collectStats(PackStats.Collector collector) {
        collector.collect(PackStatKeys.BLOCK_MAPPINGS, blocks)
                .collect(PackStatKeys.ITEM_MAPPINGS, items)
                .collect(PackStatKeys.WAYPOINT_MAPPINGS, waypointStyles);
    }
}
