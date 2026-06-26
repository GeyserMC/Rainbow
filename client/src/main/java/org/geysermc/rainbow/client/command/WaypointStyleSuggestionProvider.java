package org.geysermc.rainbow.client.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.resources.ResourceKey;
import org.geysermc.rainbow.client.mixin.WaypointStyleManagerAccessor;

import java.util.concurrent.CompletableFuture;

public class WaypointStyleSuggestionProvider implements SuggestionProvider<FabricClientCommandSource> {
    public static final WaypointStyleSuggestionProvider INSTANCE = new WaypointStyleSuggestionProvider();

    protected WaypointStyleSuggestionProvider() {}

    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggestResource(
                ((WaypointStyleManagerAccessor) context.getSource().getClient().gui.hud.getWaypointStyles()).getWaypointStyles()
                        .keySet()
                        .stream()
                        .map(ResourceKey::identifier),
                builder);
    }
}
