package org.geysermc.rainbow.mapping.geometry;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import org.geysermc.rainbow.mapping.texture.TextureHolder;

public interface GeometryRenderer {

    TextureHolder render(Identifier identifier, ItemStackTemplate stack);
}
