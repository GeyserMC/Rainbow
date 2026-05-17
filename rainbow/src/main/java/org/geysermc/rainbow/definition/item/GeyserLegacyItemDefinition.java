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
