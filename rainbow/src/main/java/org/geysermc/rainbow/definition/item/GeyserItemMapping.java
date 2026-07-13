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
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.StringRepresentable;

public interface GeyserItemMapping extends Comparable<GeyserItemMapping> {

    Codec<GeyserItemMapping> CODEC = Codec.lazyInitialized(() -> Type.CODEC.dispatch(GeyserItemMapping::type, Type::codec));
    // Not perfect since we're not checking single definitions in groups without a model... but good enough
    Codec<GeyserItemMapping> MODEL_SAFE_CODEC = CODEC.validate(mapping -> {
        if (mapping instanceof GeyserSingleItemDefinition single && single.model().isEmpty()) {
            return DataResult.error(() -> "Top level single definition must have a model");
        }
        return DataResult.success(mapping);
    });

    Type type();

    enum Type implements StringRepresentable {
        SINGLE("definition", GeyserSingleItemDefinition.CODEC),
        LEGACY("legacy", GeyserLegacyItemDefinition.CODEC),
        GROUP("group", GeyserGroupItemDefinition.CODEC);

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final String name;
        private final MapCodec<? extends GeyserItemMapping> codec;

        Type(String name, MapCodec<? extends GeyserItemMapping> codec) {
            this.name = name;
            this.codec = codec;
        }

        public MapCodec<? extends GeyserItemMapping> codec() {
            return codec;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
