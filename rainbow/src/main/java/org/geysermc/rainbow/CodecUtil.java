package org.geysermc.rainbow;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class CodecUtil {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public static <O, T> RecordCodecBuilder<O, T> unitVerifyCodec(Codec<T> codec, String field, T value) {
        return codec.validate(read -> {
            if (!read.equals(value)) {
                return DataResult.error(() -> field + " must equal " + value + ", was " + read);
            }
            return DataResult.success(read);
        }).fieldOf(field).forGetter(object -> value);
    }

    public static <T> T readOrCompute(Codec<T> codec, Path path, Supplier<T> supplier) throws IOException {
        if (Files.exists(path)) {
            return tryReadJson(codec, path);
        }
        return supplier.get();
    }

    public static <T> T tryReadJson(Codec<T> codec, Path path) throws IOException {
        return tryReadJson(codec, path, JsonOps.INSTANCE);
    }

    public static <T> T tryReadJson(Codec<T> codec, Path path, DynamicOps<JsonElement> ops) throws IOException {
        try {
            String raw = Files.readString(path);
            JsonElement json = GSON.fromJson(raw, JsonElement.class);
            return codec.parse(ops, json).getOrThrow();
        } catch (IOException exception) {
            Rainbow.LOGGER.warn("Failed to read JSON file {}!", path, exception);
            throw exception;
        }
    }

    public static <T> void trySaveJson(Codec<T> codec, T object, Path path) throws IOException {
        trySaveJson(codec, object, path, JsonOps.INSTANCE);
    }

    public static <T> void trySaveJson(Codec<T> codec, T object, Path path, DynamicOps<JsonElement> ops) throws IOException {
        JsonElement json = codec.encodeStart(ops, object).getOrThrow();

        try {
            ensureDirectoryExists(path.getParent());
            Files.writeString(path, GSON.toJson(json));
        } catch (IOException exception) {
            Rainbow.LOGGER.warn("Failed to write file {}!", path, exception);
            throw exception;
        }
    }

    public static void tryZipDirectory(Path directory, Path output) throws IOException {
        try (FileSystem zip = FileSystems.newFileSystem(output, Map.of("create", "true"))) {
            try (Stream<Path> paths = Files.walk(directory)) {
                paths.forEach(path -> {
                    try {
                        Path inZip = zip.getPath(String.valueOf(directory.relativize(path)));
                        if (Files.isDirectory(path)) {
                            Files.createDirectories(inZip);
                        } else {
                            Files.copy(path, inZip, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException exception) {
                        Rainbow.LOGGER.warn("Failed to copy contents from {} to ZIP-file {}!", path, output, exception);
                    }
                });
            }
        } catch (IOException exception) {
            Rainbow.LOGGER.warn("Failed to write ZIP-file {}!", output, exception);
            throw exception;
        }
    }

    public static void ensureDirectoryExists(Path directory) throws IOException {
        if (!Files.isDirectory(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException exception) {
                Rainbow.LOGGER.warn("Failed to create directory!", exception);
                throw exception;
            }
        }
    }
}
