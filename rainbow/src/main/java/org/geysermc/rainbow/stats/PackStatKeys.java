package org.geysermc.rainbow.stats;

public final class PackStatKeys {
    public static final PackStatKey BLOCK_MAPPINGS = geyserMappingsKey("block");
    public static final PackStatKey ITEM_MAPPINGS = geyserMappingsKey("item");
    public static final PackStatKey SKULL_MAPPINGS = geyserMappingsKey("skull");
    public static final PackStatKey WAYPOINT_MAPPINGS = geyserMappingsKey("waypoint style");

    public static final PackStatKey ITEM_TEXTURE_ATLAS_SIZE = simple("item texture atlas size");
    public static final PackStatKey TERRAIN_TEXTURE_ATLAS_SIZE = simple("terrain texture atlas size");
    public static final PackStatKey FLIPBOOK_TEXTURE_ATLAS_SIZE = simple("flipbook texture atlas size");
    public static final PackStatKey SOUND_DEFINITIONS = simple("sound definitions");

    public static final PackStatKey ATTACHABLES = exported("attachables");

    public static final PackStatKey USERNAME_SKULLS = exported("username skulls");
    public static final PackStatKey UUID_SKULLS = exported("UUID skulls");
    public static final PackStatKey STATIC_PROFILE_SKULLS = exported("static profile skulls");

    public static final PackStatKey.AssetCacheStatKey GEOMETRY_CACHE = assetCache("geometry");
    public static final PackStatKey.AssetCacheStatKey BLOCK_TEXTURE_CACHE = assetCache("block texture");
    public static final PackStatKey.AssetCacheStatKey ITEM_TEXTURE_CACHE = assetCache("item texture");

    private static PackStatKey geyserMappingsKey(String type) {
        return written(type + " mappings");
    }

    private static PackStatKey written(String humanName) {
        return new PackStatKey.Single(humanName, PackStatKey.Single.TaskType.WRITTEN);
    }

    private static PackStatKey exported(String humanName) {
        return new PackStatKey.Single(humanName, PackStatKey.Single.TaskType.EXPORTED);
    }

    private static PackStatKey simple(String humanName) {
        return new PackStatKey.Single(humanName, PackStatKey.Single.TaskType.NONE);
    }

    private static PackStatKey.AssetCacheStatKey assetCache(String humanName) {
        return new PackStatKey.AssetCacheStatKey(humanName);
    }

    private PackStatKeys() {}
}
