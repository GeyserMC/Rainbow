package org.geysermc.rainbow.definition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public record GeyserSingleDefinition(GeyserBaseDefinition base, Optional<Identifier> model) implements GeyserItemDefinition {
    public static final MapCodec<GeyserSingleDefinition> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    GeyserBaseDefinition.MAP_CODEC.forGetter(GeyserSingleDefinition::base),
                    Identifier.CODEC.optionalFieldOf("model").forGetter(GeyserSingleDefinition::model)
            ).apply(instance, GeyserSingleDefinition::new)
    );

    @Override
    public boolean conflictsWith(Optional<Identifier> parentModel, GeyserItemDefinition other) {
        if (other instanceof GeyserSingleDefinition otherSingle) {
            Identifier thisModel = model.or(() -> parentModel).orElseThrow();
            Identifier otherModel = otherSingle.model.or(() -> parentModel).orElseThrow();
            return thisModel.equals(otherModel) && base.conflictsWith(other.base());
        }
        return false;
    }

    public GeyserSingleDefinition withoutModel() {
        return new GeyserSingleDefinition(base, Optional.empty());
    }

    @Override
    public Type type() {
        return Type.SINGLE;
    }
}
