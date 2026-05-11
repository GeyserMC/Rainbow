package org.geysermc.rainbow.definition.predicate;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.level.Level;

import java.util.function.Function;

public record GeyserMatchPredicate(MatchPredicateData data) implements GeyserPredicate {

    public static final MapCodec<GeyserMatchPredicate> CODEC = MatchPredicateData.CODEC.xmap(GeyserMatchPredicate::new, GeyserMatchPredicate::data);

    @Override
    public Type type() {
        return Type.MATCH;
    }

    public interface MatchPredicateData {

        MapCodec<MatchPredicateData> CODEC = Type.CODEC.dispatchMap("property", MatchPredicateData::type, Type::codec);

        Type type();

        enum Type implements StringRepresentable {
            CHARGE_TYPE("charge_type", ChargeType.CODEC),
            TRIM_MATERIAL("trim_material", TrimMaterialData.CODEC),
            CONTEXT_DIMENSION("context_dimension", ContextDimension.CODEC),
            CUSTOM_MODEL_DATA("custom_model_data", CustomModelData.CODEC);

            public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

            private final String name;
            private final MapCodec<? extends MatchPredicateData> codec;

            Type(String name, MapCodec<? extends MatchPredicateData> codec) {
                this.name = name;
                this.codec = codec;
            }

            public MapCodec<? extends MatchPredicateData> codec() {
                return codec;
            }

            @Override
            public String getSerializedName() {
                return name;
            }
        }
    }

    public record ChargeType(CrossbowItem.ChargeType chargeType) implements MatchPredicateData {
        public static final MapCodec<ChargeType> CODEC = simpleCodec(CrossbowItem.ChargeType.CODEC, ChargeType::chargeType, ChargeType::new);

        @Override
        public Type type() {
            return Type.CHARGE_TYPE;
        }
    }

    public record TrimMaterialData(ResourceKey<TrimMaterial> material) implements MatchPredicateData {
        public static final MapCodec<TrimMaterialData> CODEC = simpleCodec(ResourceKey.codec(Registries.TRIM_MATERIAL), TrimMaterialData::material, TrimMaterialData::new);

        @Override
        public Type type() {
            return Type.TRIM_MATERIAL;
        }
    }

    public record ContextDimension(ResourceKey<Level> level) implements MatchPredicateData {
        public static final MapCodec<ContextDimension> CODEC = simpleCodec(ResourceKey.codec(Registries.DIMENSION), ContextDimension::level, ContextDimension::new);

        @Override
        public Type type() {
            return Type.CONTEXT_DIMENSION;
        }
    }

    public record CustomModelData(String string, int index) implements MatchPredicateData {
        public static final MapCodec<CustomModelData> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.STRING.fieldOf("value").forGetter(CustomModelData::string),
                        ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("index", 0).forGetter(CustomModelData::index)
                ).apply(instance, CustomModelData::new)
        );

        @Override
        public Type type() {
            return Type.CUSTOM_MODEL_DATA;
        }
    }

    private static <P, T> MapCodec<P> simpleCodec(Codec<T> codec, Function<P, T> getter, Function<T, P> constructor) {
        return codec.fieldOf("value").xmap(constructor, getter);
    }
}
