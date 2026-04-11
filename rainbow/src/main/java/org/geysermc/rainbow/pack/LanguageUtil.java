package org.geysermc.rainbow.pack;

import com.mojang.serialization.Codec;
import net.minecraft.util.ProblemReporter;
import org.geysermc.rainbow.mapping.AssetResolver;
import org.geysermc.rainbow.mapping.PackSerializer;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class LanguageUtil {
    public static final Codec<Map<String, String>> LANGUAGE_FILE_CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING);

    private LanguageUtil() {}

    public static List<? extends CompletableFuture<?>> saveLanguages(AssetResolver assets, ProblemReporter reporter,
                                                                     PackSerializer serializer, Path outputFolder) {
        return assets.getForeignLanguages().entrySet().stream()
                .map(entry -> {
                    // Not a problem, report anyway to show that it happened
                    reporter.report(() -> "exporting language " + entry.getKey() + " with " + entry.getValue().size() + " keys");
                    return serializer.saveJson(LANGUAGE_FILE_CODEC, entry.getValue(), outputFolder.resolve(entry.getKey() + ".json"));
                })
                .toList();
    }
}
