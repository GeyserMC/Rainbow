package org.geysermc.rainbow.client.stats;

import org.geysermc.rainbow.stats.PackStatKey;
import org.geysermc.rainbow.stats.PackStatKeys;

public final class PackStatGroups {
    public static final PackStatGroup MAPPINGS = create(PackStatKeys.BLOCK_MAPPINGS, PackStatKeys.ITEM_MAPPINGS,
            PackStatKeys.SKULL_MAPPINGS, PackStatKeys.WAYPOINT_MAPPINGS);
    public static final PackStatGroup ATLASES = create(PackStatKeys.ITEM_TEXTURE_ATLAS_SIZE, PackStatKeys.TERRAIN_TEXTURE_ATLAS_SIZE,
            PackStatKeys.FLIPBOOK_TEXTURE_ATLAS_SIZE, PackStatKeys.SOUND_DEFINITIONS);
    public static final PackStatGroup SKULLS = create(PackStatKeys.USERNAME_SKULLS, PackStatKeys.UUID_SKULLS, PackStatKeys.STATIC_PROFILE_SKULLS);
    public static final PackStatGroup ASSET_CACHE = create(PackStatKeys.GEOMETRY_CACHE, PackStatKeys.BLOCK_TEXTURE_CACHE, PackStatKeys.ITEM_TEXTURE_CACHE);

    private static PackStatGroup create(PackStatKey... keys) {
        return new PackStatGroup(keys);
    }

    private PackStatGroups() {}
}
