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

package org.geysermc.rainbow.definition.item.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

import java.util.List;

public interface GeyserPredicate {

    Codec<GeyserPredicate> CODEC = Type.CODEC.dispatch(GeyserPredicate::type, Type::codec);
    Codec<List<GeyserPredicate>> LIST_CODEC = ExtraCodecs.compactListCodec(CODEC);

    Type type();

    enum Type implements StringRepresentable {
        CONDITION("condition", GeyserConditionPredicate.CODEC),
        MATCH("match", GeyserMatchPredicate.CODEC),
        RANGE_DISPATCH("range_dispatch", GeyserRangeDispatchPredicate.CODEC);

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final String name;
        private final MapCodec<? extends GeyserPredicate> codec;

        Type(String name, MapCodec<? extends GeyserPredicate> codec) {
            this.name = name;
            this.codec = codec;
        }

        public MapCodec<? extends GeyserPredicate> codec() {
            return codec;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
