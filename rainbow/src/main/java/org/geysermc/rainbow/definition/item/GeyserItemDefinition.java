package org.geysermc.rainbow.definition.item;

import net.minecraft.resources.Identifier;

import java.util.Optional;

public interface GeyserItemDefinition extends GeyserItemMapping {

    GeyserBaseItemDefinition base();

    boolean conflictsWith(Optional<Identifier> parentModel, GeyserItemDefinition other);

    @Override
    default int compareTo(GeyserItemMapping other) {
        if (other instanceof GeyserItemDefinition itemDefinition) {
            return base().bedrockIdentifier().compareTo(itemDefinition.base().bedrockIdentifier());
        }
        return -1; // Groups are always greater than individual mappings
    }
}
