package org.geysermc.rainbow.definition.item;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.geysermc.rainbow.CodecUtil;
import org.geysermc.rainbow.stats.PackStats;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public final class GeyserItemMappings implements PackStats.Holder {
    private static final Codec<Map<Holder<Item>, Collection<GeyserItemMapping>>> MAPPINGS_CODEC = Codec.unboundedMap(Item.CODEC, GeyserItemMapping.MODEL_SAFE_CODEC.listOf().xmap(Function.identity(), ArrayList::new));

    public static final Codec<GeyserItemMappings> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CodecUtil.unitVerifyCodec(Codec.INT, "format_version", 2),
                    MAPPINGS_CODEC.fieldOf("items").forGetter(GeyserItemMappings::mappings)
            ).apply(instance, (_, mappings) -> new GeyserItemMappings(mappings))
    );

    private final Multimap<Holder<Item>, GeyserItemMapping> mappings = MultimapBuilder
            .hashKeys()
            .<GeyserItemMapping>treeSetValues(Comparator.comparing(mapping -> mapping))
            .build();

    public GeyserItemMappings() {}

    private GeyserItemMappings(Map<Holder<Item>, Collection<GeyserItemMapping>> mappings) {
        for (Holder<Item> item : mappings.keySet()) {
            this.mappings.putAll(item, mappings.get(item));
        }
    }

    public void map(Holder<Item> item, GeyserItemDefinition mapping) {
        Optional<Identifier> model = mapping instanceof GeyserSingleItemDefinition single ? Optional.of(single.model().orElseThrow()) : Optional.empty();
        Optional<GeyserGroupItemDefinition> modelGroup = Optional.empty();

        Collection<GeyserItemMapping> existingMappings = new ArrayList<>(mappings.get(item));
        for (GeyserItemMapping existing : existingMappings) {
            if (existing instanceof GeyserGroupItemDefinition existingGroup && existingGroup.isFor(model)) {
                if (existingGroup.conflictsWith(Optional.empty(), mapping)) {
                    throw new IllegalArgumentException("Mapping conflicts with existing group mapping");
                }
                modelGroup = Optional.of(existingGroup);
                break;
            } else if (existing instanceof GeyserItemDefinition itemDefinition) {
                if (itemDefinition.conflictsWith(Optional.empty(), mapping)) {
                    throw new IllegalArgumentException("Mapping conflicts with existing item mapping");
                } else if (model.isPresent() && itemDefinition instanceof GeyserSingleItemDefinition single && model.get().equals(single.model().orElseThrow())) {
                    mappings.remove(item, itemDefinition);
                    modelGroup = Optional.of(new GeyserGroupItemDefinition(model, List.of(single.withoutModel())));
                }
            }
        }

        if (modelGroup.isPresent()) {
            mappings.remove(item, modelGroup.get());

            // We're only putting mappings in groups when they're single definitions - legacy mappings always go ungrouped
            assert mapping instanceof GeyserSingleItemDefinition;
            mappings.put(item, modelGroup.get().with(((GeyserSingleItemDefinition) mapping).withoutModel()));
        } else {
            mappings.put(item, mapping);
        }
    }

    public Map<Holder<Item>, Collection<GeyserItemMapping>> mappings() {
        return mappings.asMap();
    }

    public int size() {
        int totalSize = 0;
        for (GeyserItemMapping mapping : mappings.values()) {
            if (mapping instanceof GeyserGroupItemDefinition group) {
                totalSize += group.size();
            } else {
                totalSize++;
            }
        }
        return totalSize;
    }

    @Override
    public int stat() {
        return size();
    }
}
