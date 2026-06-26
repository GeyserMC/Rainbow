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

package org.geysermc.rainbow.client.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;

import java.util.concurrent.CompletableFuture;

public class CommandSuggestionsArgumentType implements ArgumentType<Pair<String, CompletableFuture<Suggestions>>> {

    public static final CommandSuggestionsArgumentType TYPE = new CommandSuggestionsArgumentType();

    @Override
    public Pair<String, CompletableFuture<Suggestions>> parse(StringReader reader) {
        String command = reader.getRemaining();
        reader.setCursor(reader.getTotalLength());
        return Pair.of(command, Minecraft.getInstance().getConnection().getSuggestionsProvider().customSuggestion(createCommandSuggestionsContext(command)));
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        int offset = builder.getStart();
        return Minecraft.getInstance().getConnection().getSuggestionsProvider().customSuggestion(createCommandSuggestionsContext(builder.getRemaining()))
                .thenApply(suggestions -> addOffset(suggestions, offset));
    }

    public static Pair<String, CompletableFuture<Suggestions>> getSuggestions(CommandContext<?> context, String argument) {
        //noinspection unchecked
        return context.getArgument(argument, Pair.class);
    }

    private Suggestions addOffset(Suggestions suggestions, int offset) {
        StringRange offsetRange = addOffset(suggestions.getRange(), offset);
        return new Suggestions(offsetRange, suggestions.getList().stream()
                .map(suggestion -> new Suggestion(addOffset(suggestion.getRange(), offset), suggestion.getText(), suggestion.getTooltip()))
                .toList());
    }

    private StringRange addOffset(StringRange range, int offset) {
        return new StringRange(range.getStart() + offset, range.getEnd() + offset);
    }

    private static CommandContext<?> createCommandSuggestionsContext(String string) {
        // hack
        return new CommandContext<>(null,
                string,
                null, null, null, null, null, null, null, false);
    }
}
