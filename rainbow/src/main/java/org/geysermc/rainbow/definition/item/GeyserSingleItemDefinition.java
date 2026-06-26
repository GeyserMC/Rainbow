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

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public record GeyserSingleItemDefinition(GeyserBaseItemDefinition base, Optional<Identifier> model) implements GeyserItemDefinition {
    public static final MapCodec<GeyserSingleItemDefinition> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    GeyserBaseItemDefinition.MAP_CODEC.forGetter(GeyserSingleItemDefinition::base),
                    Identifier.CODEC.optionalFieldOf("model").forGetter(GeyserSingleItemDefinition::model)
            ).apply(instance, GeyserSingleItemDefinition::new)
    );

    @Override
    public boolean conflictsWith(Optional<Identifier> parentModel, GeyserItemDefinition other) {
        if (other instanceof GeyserSingleItemDefinition otherSingle) {
            Identifier thisModel = model.or(() -> parentModel).orElseThrow();
            Identifier otherModel = otherSingle.model.or(() -> parentModel).orElseThrow();
            return thisModel.equals(otherModel) && base.conflictsWith(other.base());
        }
        return false;
    }

    public GeyserSingleItemDefinition withoutModel() {
        return new GeyserSingleItemDefinition(base, Optional.empty());
    }

    @Override
    public Type type() {
        return Type.SINGLE;
    }
}
