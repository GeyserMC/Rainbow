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

    private GeyserBlockMappings(Map<Holder<Block>, GeyserBlockMapping> mappings) {
        this.mappings.putAll(mappings);
    }

    public Map<Holder<Block>, GeyserBlockMapping> mappings() {
        return Collections.unmodifiableMap(mappings);
    }
}
