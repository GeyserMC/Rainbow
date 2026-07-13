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

package org.geysermc.rainbow.pack.geometry;

import net.minecraft.world.entity.EquipmentSlot;
import org.jspecify.annotations.Nullable;

public class VanillaGeometries {
    public static final String ITEM_SPRITE = "geometry.item_sprite";
    public static final String HELMET = "geometry.player.armor.helmet";
    public static final String CHESTPLATE = "geometry.player.armor.chestplate";
    public static final String ELYTRA = "geometry.elytra";
    public static final String LEGGINGS = "geometry.player.armor.leggings";
    public static final String BOOTS = "geometry.player.armor.boots";

    public static @Nullable String fromEquipmentSlot(EquipmentSlot slot, boolean glider) {
        if (glider) {
            return ELYTRA;
        }
        return switch (slot) {
            case FEET -> BOOTS;
            case LEGS -> LEGGINGS;
            case CHEST -> CHESTPLATE;
            case HEAD -> HELMET;
            default -> null;
        };
    }
}
