package org.geysermc.packgenerator.pack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import java.util.List;

public record BedrockVersion(int major, int minor, int patch) {
    public static final Codec<BedrockVersion> CODEC = Codec.INT.listOf(3, 3)
            .xmap(list -> BedrockVersion.of(list.getFirst(), list.get(1), list.getLast()),
                    version -> List.of(version.major, version.minor, version.patch));
    public static final Codec<BedrockVersion> STRING_CODEC = Codec.STRING.comapFlatMap(string -> {
        String[] segments = string.split("\\.");
        if (segments.length != 3) {
            return DataResult.error(() -> "Semantic version must consist of 3 versions");
        }
        try {
            int major = Integer.parseInt(segments[0]);
            int minor = Integer.parseInt(segments[1]);
            int patch = Integer.parseInt(segments[2]);
            return DataResult.success(new BedrockVersion(major, minor, patch));
        } catch (NumberFormatException exception) {
            return DataResult.error(() -> "Failed to parse semantic version number");
        }
    }, version -> String.format("%d.%d.%d", version.major, version.minor, version.patch));

    public static BedrockVersion of(int patch) {
        return of(0, 0, patch);
    }

    public static BedrockVersion of(int minor, int patch) {
        return of(0, minor, patch);
    }

    public static BedrockVersion of(int major, int minor, int patch) {
        return new BedrockVersion(major, minor, patch);
    }

    public BedrockVersion increment() {
        return new BedrockVersion(major, minor, patch + 1);
    }
}
