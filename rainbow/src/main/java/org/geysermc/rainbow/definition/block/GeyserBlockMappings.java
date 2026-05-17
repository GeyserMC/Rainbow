package org.geysermc.rainbow.definition.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.geysermc.rainbow.CodecUtil;

import java.util.Collections;
import java.util.Map;

public class GeyserBlockMappings {
    // Inspired by Item.CODEC
    private static final Codec<Holder<Block>> BLOCK_CODEC = BuiltInRegistries.BLOCK
            .holderByNameCodec()
            .validate(block -> block.is(Blocks.AIR.builtInRegistryHolder()) ? DataResult.error(() -> "Block must not be minecraft:air") : DataResult.success(block));
    public static final Codec<GeyserBlockMappings> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CodecUtil.unitVerifyCodec(Codec.INT, "format_version", 1),
                    Codec.unboundedMap(BLOCK_CODEC, GeyserBlockMapping.CODEC).fieldOf("blocks").forGetter(GeyserBlockMappings::mappings)
            ).apply(instance, (_, mappings) -> new GeyserBlockMappings(mappings))
    );

    private final Map<Holder<Block>, GeyserBlockMapping> mappings = new Object2ObjectOpenHashMap<>();

    public GeyserBlockMappings() {}

    private GeyserBlockMappings(Map<Holder<Block>, GeyserBlockMapping> mappings) {
        this.mappings.putAll(mappings);
    }

    public void map(Holder<Block> block, GeyserBlockMapping.Builder mapping) {
        map(block, mapping.build());
    }

    public void map(Holder<Block> block, GeyserBlockMapping mapping) {
        if (block.value().getStateDefinition().isSingletonState() && (mapping.base().isEmpty() || !mapping.stateOverrides().isEmpty())) {
            throw new IllegalArgumentException("mapping must have a base and must not have state overrides because the base block only has a single state");
        } else if (mapping.base().isEmpty() && !mapping.onlyOverrideStates()) {
            throw new IllegalArgumentException("mapping must have a base or only override states");
        } else if (mapping.onlyOverrideStates() && mapping.stateOverrides().isEmpty()) {
            throw new IllegalArgumentException("mapping must have at least a single state override, as onlyOverrideStates is set to true");
        }

        GeyserBlockMapping existing = mappings.get(block);
        if (existing != null) {
            if (existing.onlyOverrideStates() && mapping.onlyOverrideStates()) {
                mappings.put(block, existing.mergeStateOverrides(mapping));
            } else {
                throw new IllegalStateException("tried to register existing mapping for block " + block + ", and was unable to merge");
            }
        } else {
            mappings.put(block, mapping);
        }
    }

    public int size() {
        return mappings.values().stream()
                .mapToInt(mapping -> (mapping.base().isPresent() ? 1 : 0) + mapping.stateOverrides().size())
                .sum();
    }

    public Map<Holder<Block>, GeyserBlockMapping> mappings() {
        return Collections.unmodifiableMap(mappings);
    }
}
