package org.geysermc.rainbow.client.render;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import org.geysermc.rainbow.mapping.geometry.GeometryRenderer;
import org.geysermc.rainbow.mapping.texture.TextureHolder;

// TODO maybe just use this even for normal 2D items, not sure, could be useful for composite models and stuff
// TODO output in a size bedrock likes
public class MinecraftGeometryRenderer implements GeometryRenderer {
    public static final MinecraftGeometryRenderer INSTANCE = new MinecraftGeometryRenderer();

    @Override
    public TextureHolder render(Identifier identifier, ItemStackTemplate stack) {
        // Don't render enchantment glint
        DataComponentPatch.Builder renderedComponents = DataComponentPatch.builder();
        stack.components().split().added().forEach(renderedComponents::set);
        stack.components().split().removed().forEach(renderedComponents::remove);
        renderedComponents.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
        return new RenderedTextureHolder(identifier, new ItemStackTemplate(stack.item().value(), renderedComponents.build()));
    }
}
