package org.geysermc.rainbow.definition;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.geysermc.rainbow.CodecUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class GeyserMappings {
    private static final Codec<Map<Holder<Item>, Collection<GeyserMapping>>> MAPPINGS_CODEC = Codec.unboundedMap(Item.CODEC, GeyserMapping.MODEL_SAFE_CODEC.listOf().xmap(Function.identity(), ArrayList::new));

    public static final Codec<GeyserMappings> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CodecUtil.unitVerifyCodec(Codec.INT, "format_version", 2),
                    MAPPINGS_CODEC.fieldOf("items").forGetter(GeyserMappings::mappings)
            ).apply(instance, (format, mappings) -> new GeyserMappings(mappings))
    );

    private final Multimap<Holder<Item>, GeyserMapping> mappings = MultimapBuilder
            .hashKeys()
            .<GeyserMapping>treeSetValues(Comparator.comparing(mapping -> mapping))
            .build();

    public GeyserMappings() {}

    private GeyserMappings(Map<Holder<Item>, Collection<GeyserMapping>> mappings) {
        for (Holder<Item> item : mappings.keySet()) {
            this.mappings.putAll(item, mappings.get(item));
        }
    }

    public void map(Holder<Item> item, GeyserItemDefinition mapping) {
        Optional<Identifier> model = mapping instanceof GeyserSingleDefinition single ? Optional.of(single.model().orElseThrow()) : Optional.empty();
        Optional<GeyserGroupDefinition> modelGroup = Optional.empty();

        Collection<GeyserMapping> existingMappings = new ArrayList<>(mappings.get(item));
        for (GeyserMapping existing : existingMappings) {
            if (existing instanceof GeyserGroupDefinition existingGroup && existingGroup.isFor(model)) {
                if (existingGroup.conflictsWith(Optional.empty(), mapping)) {
                    throw new IllegalArgumentException("Mapping conflicts with existing group mapping");
                }
                modelGroup = Optional.of(existingGroup);
                break;
            } else if (existing instanceof GeyserItemDefinition itemDefinition) {
                if (itemDefinition.conflictsWith(Optional.empty(), mapping)) {
                    throw new IllegalArgumentException("Mapping conflicts with existing item mapping");
                } else if (model.isPresent() && itemDefinition instanceof GeyserSingleDefinition single && model.get().equals(single.model().orElseThrow())) {
                    mappings.remove(item, itemDefinition);
                    modelGroup = Optional.of(new GeyserGroupDefinition(model, List.of(single.withoutModel())));
                }
            }
        }

        if (modelGroup.isPresent()) {
            mappings.remove(item, modelGroup.get());

            // We're only putting mappings in groups when they're single definitions - legacy mappings always go ungrouped
            assert mapping instanceof GeyserSingleDefinition;
            mappings.put(item, modelGroup.get().with(((GeyserSingleDefinition) mapping).withoutModel()));
        } else {
            mappings.put(item, mapping);
        }
    }

    public int size() {
        int totalSize = 0;
        for (GeyserMapping mapping : mappings.values()) {
            if (mapping instanceof GeyserGroupDefinition group) {
                totalSize += group.size();
            } else {
                totalSize++;
            }
        }
        return totalSize;
    }

    public Map<Holder<Item>, Collection<GeyserMapping>> mappings() {
        return mappings.asMap();
    }
}
