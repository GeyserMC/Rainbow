package org.geysermc.rainbow.definition.predicate;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;

import java.util.function.Supplier;

public record GeyserConditionPredicate(Property property, boolean expected) implements GeyserPredicate {

    public static final MapCodec<GeyserConditionPredicate> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Property.CODEC.forGetter(GeyserConditionPredicate::property),
                    Codec.BOOL.optionalFieldOf("expected", true).forGetter(GeyserConditionPredicate::expected)
            ).apply(instance, GeyserConditionPredicate::new)
    );

    public static final Property BROKEN = unit(Property.Type.BROKEN);
    public static final Property DAMAGED = unit(Property.Type.DAMAGED);
    public static final Property FISHING_ROD_CAST = unit(Property.Type.FISHING_ROD_CAST);

    @Override
    public Type type() {
        return Type.CONDITION;
    }

    public interface Property {

        MapCodec<Property> CODEC = Type.CODEC.dispatchMap("property", Property::type, Type::codec);

        Type type();

        enum Type implements StringRepresentable {
            BROKEN("broken", () -> MapCodec.unit(GeyserConditionPredicate.BROKEN)),
            DAMAGED("damaged", () -> MapCodec.unit(GeyserConditionPredicate.DAMAGED)),
            CUSTOM_MODEL_DATA("custom_model_data", () -> CustomModelData.CODEC),
            HAS_COMPONENT("has_component", () -> HasComponent.CODEC),
            FISHING_ROD_CAST("fishing_rod_cast", () -> MapCodec.unit(GeyserConditionPredicate.FISHING_ROD_CAST));

            public static final Codec<Type> CODEC = StringRepresentable.fromEnum(Type::values);

            private final String name;
            private final Supplier<MapCodec<? extends Property>> codec;

            Type(String name, Supplier<MapCodec<? extends Property>> codec) {
                this.name = name;
                this.codec = Suppliers.memoize(codec::get);
            }

            public MapCodec<? extends Property> codec() {
                return codec.get();
            }

            @Override
            public String getSerializedName() {
                return name;
            }
        }
    }

    public record CustomModelData(int index) implements Property {
        public static final MapCodec<CustomModelData> CODEC = ExtraCodecs.NON_NEGATIVE_INT.optionalFieldOf("index", 0).xmap(CustomModelData::new, CustomModelData::index);

        @Override
        public Type type() {
            return Type.CUSTOM_MODEL_DATA;
        }
    }

    public record HasComponent(DataComponentType<?> component) implements Property {
        public static final MapCodec<HasComponent> CODEC = DataComponentType.CODEC.fieldOf("component").xmap(HasComponent::new, HasComponent::component);

        @Override
        public Type type() {
            return Type.HAS_COMPONENT;
        }
    }

    private static Property unit(Property.Type type) {
        return () -> type;
    }
}
