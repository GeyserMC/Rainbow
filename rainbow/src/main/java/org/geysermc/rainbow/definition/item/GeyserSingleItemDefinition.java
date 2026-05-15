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
