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

package org.geysermc.rainbow.client;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Util;
import org.apache.commons.io.IOUtils;
import org.geysermc.rainbow.CodecUtil;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.jspecify.annotations.Nullable;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class ClientPackSerializer implements PackSerializer {
    private HolderLookup.@Nullable Provider registries = null;
    private int jsonExported = 0;
    private int texturesExported = 0;

    public void prepare(HolderLookup.Provider registries) {
        this.registries = registries;
        jsonExported = 0;
        texturesExported = 0;
    }

    public int jsonExported() {
        return jsonExported;
    }

    public int texturesExported() {
        return texturesExported;
    }

    @Override
    public <T> CompletableFuture<?> saveJson(Codec<T> codec, T object, Path path) {
        if (registries == null) {
            throw new IllegalStateException("saveJson called whilst registries was null");
        }
        DynamicOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        jsonExported++;
        return CompletableFuture.runAsync(() -> RainbowIO.safeIO(() -> {
            CodecUtil.trySaveJson(codec, object, path, ops);
        }), Util.backgroundExecutor().forName("PackSerializer-saveJson"));
    }

    @Override
    public CompletableFuture<?> saveTexture(byte[] texture, Path path) {
        texturesExported++;
        return CompletableFuture.runAsync(() -> RainbowIO.safeIO(() -> {
            CodecUtil.ensureDirectoryExists(path.getParent());
            try (OutputStream outputTexture = new FileOutputStream(path.toFile())) {
                outputTexture.write(texture);
            }
        }), Util.backgroundExecutor().forName("PackSerializer-saveTexture"));
    }

    @Override
    public CompletableFuture<?> saveResource(Resource resource, Path path) {
        return CompletableFuture.runAsync(() -> RainbowIO.safeIO(() -> {
            CodecUtil.ensureDirectoryExists(path.getParent());
            try (InputStream input = resource.open()) {
                try (OutputStream output = new FileOutputStream(path.toFile())) {
                    IOUtils.copy(input, output);
                }
            }
        }), Util.backgroundExecutor().forName("PackSerializer-saveResource"));
    }
}
