package org.geysermc.rainbow.client;

import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.Util;
import org.geysermc.rainbow.CodecUtil;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.mapping.PackSerializer;

import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class MinecraftPackSerializer implements PackSerializer {
    private final HolderLookup.Provider registries;

    public MinecraftPackSerializer(Minecraft minecraft) {
        registries = Objects.requireNonNull(minecraft.level).registryAccess();
    }

    @Override
    public <T> CompletableFuture<?> saveJson(Codec<T> codec, T object, Path path) {
        DynamicOps<JsonElement> ops = RegistryOps.create(JsonOps.INSTANCE, registries);
        return CompletableFuture.runAsync(() -> RainbowIO.safeIO(() -> CodecUtil.trySaveJson(codec, object, path, ops)),
                Util.backgroundExecutor().forName("PackSerializer-saveJson"));
    }

    @Override
    public CompletableFuture<?> saveTexture(byte[] texture, Path path) {
        return CompletableFuture.runAsync(() -> RainbowIO.safeIO(() -> {
            CodecUtil.ensureDirectoryExists(path.getParent());
            try (OutputStream outputTexture = new FileOutputStream(path.toFile())) {
                outputTexture.write(texture);
            }
        }), Util.backgroundExecutor().forName("PackSerializer-saveTexture"));
    }
}
