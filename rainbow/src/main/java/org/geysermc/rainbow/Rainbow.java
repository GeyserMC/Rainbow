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

package org.geysermc.rainbow;

import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import org.slf4j.Logger;

public class Rainbow {

    public static final String MOD_ID = "rainbow";
    public static final String MOD_NAME = "Rainbow";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static Identifier getModdedIdentifier(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static String bedrockSafeIdentifier(Identifier identifier) {
        return identifier.toString().replace(':', '.').replace('/', '_');
    }

    public static Identifier decorateIdentifier(Identifier identifier, String type, String extension) {
        return identifier.withPath(path -> type + "/" + path + "." + extension);
    }

    public static Identifier decorateTextureIdentifier(Identifier identifier) {
        return decorateIdentifier(identifier, "textures", "png");
    }

    public static Identifier getModelIdentifier(ResolvedModel model) {
        // debugName() returns the resource location of the model as a string
        return Identifier.parse(model.debugName());
    }

    public static boolean isVanilla(String namespace) {
        return namespace.equals(Identifier.DEFAULT_NAMESPACE);
    }

    public static boolean isVanilla(Identifier identifier) {
        return isVanilla(identifier.getNamespace());
    }

    public static boolean isVanilla(ResourceKey<?> key) {
        return isVanilla(key.identifier());
    }
}
