package org.geysermc.rainbow.definition;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.Optional;

public record GeyserLegacyDefinition(GeyserBaseDefinition base, int customModelData) implements GeyserItemDefinition {

    public static final MapCodec<GeyserLegacyDefinition> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    GeyserBaseDefinition.MAP_CODEC.forGetter(GeyserLegacyDefinition::base),
                    Codec.INT.fieldOf("custom_model_data").forGetter(GeyserLegacyDefinition::customModelData)
            ).apply(instance, GeyserLegacyDefinition::new)
    );

    @Override
    public boolean conflictsWith(Optional<Identifier> parentModel, GeyserItemDefinition other) {
        if (other instanceof GeyserLegacyDefinition otherLegacy) {
            return customModelData == otherLegacy.customModelData && base.conflictsWith(otherLegacy.base);
        }
        return false;
    }

    @Override
    public Type type() {
        return Type.LEGACY;
    }
}
