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

package org.geysermc.rainbow.definition.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public record GeyserLegacyItemDefinition(GeyserBaseItemDefinition base, int customModelData) implements GeyserItemDefinition {

    public static final MapCodec<GeyserLegacyItemDefinition> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    GeyserBaseItemDefinition.MAP_CODEC.forGetter(GeyserLegacyItemDefinition::base),
                    Codec.INT.fieldOf("custom_model_data").forGetter(GeyserLegacyItemDefinition::customModelData)
            ).apply(instance, GeyserLegacyItemDefinition::new)
    );

    @Override
    public boolean conflictsWith(Optional<Identifier> parentModel, GeyserItemDefinition other) {
        if (other instanceof GeyserLegacyItemDefinition(GeyserBaseItemDefinition otherBase, int otherModelData)) {
            return customModelData == otherModelData && base.conflictsWith(otherBase);
        }
        return false;
    }

    @Override
    public Type type() {
        return Type.LEGACY;
    }
}
