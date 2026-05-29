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
