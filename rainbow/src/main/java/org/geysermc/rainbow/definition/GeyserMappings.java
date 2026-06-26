package org.geysermc.rainbow.definition;

import org.geysermc.rainbow.definition.block.GeyserBlockMappings;
import org.geysermc.rainbow.definition.item.GeyserItemMappings;
import org.geysermc.rainbow.definition.skull.GeyserSkullMappings;

public record GeyserMappings(GeyserBlockMappings blocks, GeyserItemMappings items,
                             GeyserSkullMappings skulls) {

    public GeyserMappings() {
        this(new GeyserBlockMappings(), new GeyserItemMappings(), new GeyserSkullMappings());
    }
}
