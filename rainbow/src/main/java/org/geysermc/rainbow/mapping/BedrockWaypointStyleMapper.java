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

package org.geysermc.rainbow.mapping;

import net.minecraft.client.resources.WaypointStyle;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.waypoints.WaypointStyleAsset;
import org.geysermc.rainbow.ProblemSuccessReporter;
import org.geysermc.rainbow.mapping.texture.TextureHolder;
import org.geysermc.rainbow.pack.BedrockWaypointStyle;

import java.util.Optional;

public class BedrockWaypointStyleMapper {

    public static void tryMapWaypointStyle(ResourceKey<WaypointStyleAsset> key, ProblemSuccessReporter reporter, PackContext context) {
        Optional<WaypointStyle> optional = context.assetResolver().getWaypointStyle(key);
        optional.ifPresentOrElse(style -> {
            context.mappings().waypointStyles().map(key.identifier(), style);
            context.assetConsumer().acceptWaypointStyle(new BedrockWaypointStyle(style.sprites().stream()
                    .map(sprite -> TextureHolder.createBuiltIn(javaSpriteToBedrockSprite(sprite), sprite.withPrefix("gui/sprites/hud/locator_bar_dot/")))
                    .toList()));
            reporter.reportSuccess(() -> "mapped waypoint style " + key.identifier() + " with " + style.sprites().size() + " sprites");
        }, () -> reporter.report(() -> "not mapping missing waypoint style " + key.identifier()));
    }

    private static Identifier javaSpriteToBedrockSprite(Identifier sprite) {
        return Identifier.withDefaultNamespace("ui/" + sprite.getNamespace() + "/locator_bar_dot/" + sprite.getPath());
    }
}
