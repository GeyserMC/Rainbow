package org.geysermc.rainbow.definition.predicate;

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
