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

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.client.accessor.SoundManagerAccessor;

import java.util.concurrent.CompletableFuture;

public class SoundNamespaceSuggestionProvider implements SuggestionProvider<FabricClientCommandSource> {
    public static final SoundNamespaceSuggestionProvider INSTANCE = new SoundNamespaceSuggestionProvider();

    protected SoundNamespaceSuggestionProvider() {}

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(((SoundManagerAccessor) context.getSource().getClient().getSoundManager()).rainbow$getRawRegistrations().keySet().stream()
                .filter(namespace -> !Rainbow.isVanilla(namespace)), builder);
    }
}
