package org.geysermc.rainbow.definition;

import org.geysermc.rainbow.definition.block.GeyserBlockMappings;
import org.geysermc.rainbow.definition.item.GeyserItemMappings;

public record GeyserMappings(GeyserBlockMappings blocks, GeyserItemMappings items) {

    public GeyserMappings() {
        this(new GeyserBlockMappings(), new GeyserItemMappings());
    }
}
