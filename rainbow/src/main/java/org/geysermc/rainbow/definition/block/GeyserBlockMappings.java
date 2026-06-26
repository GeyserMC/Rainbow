package org.geysermc.rainbow.definition.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.geysermc.rainbow.definition.AbstractGeyserMappings;

import java.util.Map;

public final class GeyserBlockMappings extends AbstractGeyserMappings<Holder<Block>, GeyserBlockMapping> {
    // Inspired by Item.CODEC
    private static final Codec<Holder<Block>> BLOCK_CODEC = BuiltInRegistries.BLOCK
            .holderByNameCodec()
            .validate(block -> block.is(Blocks.AIR.builtInRegistryHolder()) ? DataResult.error(() -> "Block must not be minecraft:air") : DataResult.success(block));
    public static final Codec<GeyserBlockMappings> CODEC = createCodec("blocks", 1, BLOCK_CODEC, GeyserBlockMapping.CODEC, GeyserBlockMappings::new);

    public GeyserBlockMappings() {}

    private GeyserBlockMappings(Map<Holder<Block>, GeyserBlockMapping> mappings) {
        super(mappings);
    }

    public void map(Holder<Block> block, GeyserBlockMapping.Builder mapping) {
        map(block, mapping.build());
    }

    @Override
    public void map(Holder<Block> block, GeyserBlockMapping mapping) {
        if (block.value().getStateDefinition().isSingletonState() && (mapping.base().isEmpty() || !mapping.stateOverrides().isEmpty())) {
            throw new IllegalArgumentException("mapping must have a base and must not have state overrides because the base block only has a single state");
        } else if (mapping.base().isEmpty() && !mapping.onlyOverrideStates()) {
            throw new IllegalArgumentException("mapping must have a base or only override states");
        } else if (mapping.onlyOverrideStates() && mapping.stateOverrides().isEmpty()) {
            throw new IllegalArgumentException("mapping must have at least a single state override, as onlyOverrideStates is set to true");
        }

        GeyserBlockMapping existing = mappings().get(block);
        if (existing != null) {
            if (existing.onlyOverrideStates() && mapping.onlyOverrideStates()) {
                super.map(block, existing.mergeStateOverrides(mapping));
            } else {
                throw new IllegalStateException("tried to register existing mapping for block " + block + ", and was unable to merge");
            }
        } else {
            super.map(block, mapping);
        }
    }

    @Override
    public int size() {
        return mappings().values().stream()
                .mapToInt(mapping -> (mapping.base().isPresent() ? 1 : 0) + mapping.stateOverrides().size())
                .sum();
    }
}
