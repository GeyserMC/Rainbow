package org.geysermc.rainbow.client.render;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.geysermc.rainbow.mapping.geometry.GeometryRenderer;
import org.geysermc.rainbow.mapping.texture.TextureHolder;

// TODO maybe just use this even for normal 2D items, not sure, could be useful for composite models and stuff
// TODO output in a size bedrock likes
public class MinecraftGeometryRenderer implements GeometryRenderer {
    public static final MinecraftGeometryRenderer INSTANCE = new MinecraftGeometryRenderer();

    @Override
    public TextureHolder render(Identifier identifier, ItemStack stack) {
        return new RenderedTextureHolder(identifier, stack);
    }
}
