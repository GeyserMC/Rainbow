package org.geysermc.rainbow.pack.attachable;

import net.minecraft.world.entity.EquipmentSlot;
import org.jspecify.annotations.Nullable;

public class VanillaGeometries {
    public static final String ITEM_SPRITE = "geometry.item_sprite";
    public static final String HELMET = "geometry.player.armor.helmet";
    public static final String CHESTPLATE = "geometry.player.armor.chestplate";
    public static final String LEGGINGS = "geometry.player.armor.leggings";
    public static final String BOOTS = "geometry.player.armor.boots";

    public static @Nullable String fromEquipmentSlot(EquipmentSlot slot) {
        return switch (slot) {
            case FEET -> BOOTS;
            case LEGS -> LEGGINGS;
            case CHEST -> CHESTPLATE;
            case HEAD -> HELMET;
            default -> null;
        };
    }
}
