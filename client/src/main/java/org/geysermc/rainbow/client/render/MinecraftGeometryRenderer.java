/*
 * Copyright (c) 2026 GeyserMC. https://geysermc.org
 *
 * This file is part of Rainbow.
 *
 * Rainbow is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * Rainbow is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * Rainbow. If not, see <https://www.gnu.org/licenses/>.
 */

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
