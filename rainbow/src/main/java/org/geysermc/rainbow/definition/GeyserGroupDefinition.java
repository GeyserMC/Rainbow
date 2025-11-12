package org.geysermc.rainbow.definition;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

public record GeyserGroupDefinition(Optional<Identifier> model, List<GeyserMapping> definitions) implements GeyserMapping {

    public static final MapCodec<GeyserGroupDefinition> CODEC = RecordCodecBuilder.mapCodec(instance ->
            instance.group(
                    Identifier.CODEC.optionalFieldOf("model").forGetter(GeyserGroupDefinition::model),
                    GeyserMapping.CODEC.listOf().fieldOf("definitions").forGetter(GeyserGroupDefinition::definitions)
            ).apply(instance, GeyserGroupDefinition::new)
    );

    public GeyserGroupDefinition with(GeyserMapping mapping) {
        return new GeyserGroupDefinition(model, Stream.concat(definitions.stream(), Stream.of(mapping))
                .sorted(Comparator.comparing(Function.identity()))
                .toList());
    }

    public boolean isFor(Optional<Identifier> model) {
        return this.model.isPresent() && model.isPresent() && this.model.get().equals(model.get());
    }

    public boolean conflictsWith(Optional<Identifier> parentModel, GeyserItemDefinition other) {
        Optional<Identifier> thisModel = model.or(() -> parentModel);
        for (GeyserMapping definition : definitions) {
            if (definition instanceof GeyserGroupDefinition group && group.conflictsWith(thisModel, other)) {
                return true;
            } else if (definition instanceof GeyserItemDefinition item && item.conflictsWith(thisModel, other)) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        int totalSize = 0;
        for (GeyserMapping definition : definitions) {
            if (definition instanceof GeyserGroupDefinition group) {
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
    public int compareTo(@NotNull GeyserMapping other) {
        if (other instanceof GeyserGroupDefinition(Optional<Identifier> otherModel, List<GeyserMapping> otherDefinitions)) {
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
