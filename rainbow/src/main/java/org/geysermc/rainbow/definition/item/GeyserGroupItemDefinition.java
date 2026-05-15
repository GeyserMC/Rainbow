package org.geysermc.rainbow.definition.item;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public record GeyserGroupItemDefinition(Optional<Identifier> model, List<GeyserItemMapping> definitions) implements GeyserItemMapping {

    public static final MapCodec<GeyserGroupItemDefinition> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Identifier.CODEC.optionalFieldOf("model").forGetter(GeyserGroupItemDefinition::model),
                    GeyserItemMapping.CODEC.listOf().fieldOf("definitions").forGetter(GeyserGroupItemDefinition::definitions)
            ).apply(instance, GeyserGroupItemDefinition::new)
    );

    public GeyserGroupItemDefinition with(GeyserItemMapping mapping) {
        return new GeyserGroupItemDefinition(model, Stream.concat(definitions.stream(), Stream.of(mapping))
                .sorted(Comparator.comparing(Function.identity()))
                .toList());
    }

    public boolean isFor(Optional<Identifier> model) {
        return this.model.isPresent() && model.isPresent() && this.model.get().equals(model.get());
    }

    public boolean conflictsWith(Optional<Identifier> parentModel, GeyserItemDefinition other) {
        Optional<Identifier> thisModel = model.or(() -> parentModel);
        for (GeyserItemMapping definition : definitions) {
            if (definition instanceof GeyserGroupItemDefinition group && group.conflictsWith(thisModel, other)) {
                return true;
            } else if (definition instanceof GeyserItemDefinition item && item.conflictsWith(thisModel, other)) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        int totalSize = 0;
        for (GeyserItemMapping definition : definitions) {
            if (definition instanceof GeyserGroupItemDefinition group) {
                totalSize += group.size();
            } else {
                totalSize++;
            }
        }
        return totalSize;
    }

    @Override
    public Type type() {
        return Type.GROUP;
    }

    @Override
    public int compareTo(GeyserItemMapping other) {
        if (other instanceof GeyserGroupItemDefinition(Optional<Identifier> otherModel, List<GeyserItemMapping> otherDefinitions)) {
            if (model.isPresent() && otherModel.isPresent()) {
                return model.get().compareTo(otherModel.get());
            } else if (model.isPresent()) {
                return 1; // Groups with models are always greater than groups without
            } else if (otherModel.isPresent()) {
                return -1;
            } else if (definitions.isEmpty() && otherDefinitions.isEmpty()) {
                return 0;
            } else if (definitions.isEmpty()) {
                return -1; // Groups with definitions are always greater than groups without
            } else if (otherDefinitions.isEmpty()) {
                return 1;
            }
            // Compare the first definition as a last resort
            return definitions.getFirst().compareTo(otherDefinitions.getFirst());
        }
        return 1; // Groups are always greater than individual mappings
    }
}
