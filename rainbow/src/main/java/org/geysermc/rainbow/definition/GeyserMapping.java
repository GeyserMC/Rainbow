package org.geysermc.rainbow.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import net.minecraft.util.StringRepresentable;

public interface GeyserMapping extends Comparable<GeyserMapping> {

    Codec<GeyserMapping> CODEC = Codec.lazyInitialized(() -> Type.CODEC.dispatch(GeyserMapping::type, Type::codec));
    // Not perfect since we're not checking single definitions in groups without a model... but good enough
    Codec<GeyserMapping> MODEL_SAFE_CODEC = CODEC.validate(mapping -> {
        if (mapping instanceof GeyserSingleDefinition single && single.model().isEmpty()) {
            return DataResult.error(() -> "Top level single definition must have a model");
        }
        return DataResult.success(mapping);
    });

    Type type();

    enum Type implements StringRepresentable {
        SINGLE("definition", GeyserSingleDefinition.CODEC),
        LEGACY("legacy", GeyserLegacyDefinition.CODEC),
        GROUP("group", GeyserGroupDefinition.CODEC);

        public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

        private final String name;
        private final MapCodec<? extends GeyserMapping> codec;

        Type(String name, MapCodec<? extends GeyserMapping> codec) {
            this.name = name;
            this.codec = codec;
        }

        public MapCodec<? extends GeyserMapping> codec() {
            return codec;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
