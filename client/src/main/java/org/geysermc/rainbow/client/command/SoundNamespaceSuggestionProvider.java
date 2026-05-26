package org.geysermc.rainbow.client.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.resources.Identifier;
import org.geysermc.rainbow.client.accessor.SoundManagerAccessor;

import java.util.concurrent.CompletableFuture;

public class SoundNamespaceSuggestionProvider implements SuggestionProvider<FabricClientCommandSource> {
    public static final SoundNamespaceSuggestionProvider INSTANCE = new SoundNamespaceSuggestionProvider();

    protected SoundNamespaceSuggestionProvider() {}

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {

        return SharedSuggestionProvider.suggest(((SoundManagerAccessor) context.getSource().getClient().getSoundManager()).rainbow$getRawRegistrations().keySet().stream()
                .filter(namespace -> !namespace.equals(Identifier.DEFAULT_NAMESPACE)), builder);
    }
}
