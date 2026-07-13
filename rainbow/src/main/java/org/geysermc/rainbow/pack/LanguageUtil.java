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
