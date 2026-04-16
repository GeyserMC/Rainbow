package org.geysermc.rainbow.pack;

import com.mojang.serialization.Codec;
import org.geysermc.rainbow.mapping.PackSerializingContext;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class LanguageUtil {
    public static final Codec<Map<String, String>> LANGUAGE_FILE_CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING);

    private LanguageUtil() {}

    public static CompletableFuture<?> saveLanguages(PackSerializingContext context, Path outputFolder) {
        return CompletableFuture.allOf(context.assetResolver().getForeignLanguages().entrySet().stream()
                .map(entry -> {
                    // Not a problem, report anyway to show that it happened
                    context.reporter().report(() -> "exporting language " + entry.getKey() + " with " + entry.getValue().size() + " keys");
                    return context.serializer().saveJson(LANGUAGE_FILE_CODEC, entry.getValue(), outputFolder.resolve(entry.getKey() + ".json"));
                })
                .toArray(CompletableFuture[]::new));
    }
}
