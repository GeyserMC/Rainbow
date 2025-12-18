package org.geysermc.rainbow.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import org.geysermc.rainbow.client.PackManager;
import org.geysermc.rainbow.client.PackManagerUtils;
import org.geysermc.rainbow.client.mapper.InventoryMapper;
import org.geysermc.rainbow.client.mapper.PackMapper;
import org.geysermc.rainbow.pack.BedrockPack;

import java.util.function.BiConsumer;

public class PackGeneratorCommand {

    private static final Component NO_PACK_CREATED = Component.translatable("feedback.rainbow.no_pack", Component.literal("/rainbow create <name>")
            .withStyle(style -> style.withColor(ChatFormatting.BLUE).withUnderlined(true)
                    .withClickEvent(new ClickEvent.SuggestCommand("/rainbow create ")))).withStyle(ChatFormatting.RED);

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, PackManager packManager, PackMapper packMapper) {
        dispatcher.register(ClientCommandManager.literal("rainbow")
                .then(ClientCommandManager.literal("create")
                        .then(ClientCommandManager.argument("name", StringArgumentType.word())
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "name");

                                    PackManagerUtils.startPack(name, packManager, context.getSource().getClient());

                                    return 0;
                                })
                        )
                )
                .then(ClientCommandManager.literal("map")
                        .executes(runWithPack(packManager, (source, pack) -> {
                            PackManagerUtils.mapItemInHand(packManager, source.getClient());
                        }))
                )
                .then(ClientCommandManager.literal("mapinventory")
                        .executes(runWithPack(packManager, (source, pack) -> {
                            PackManagerUtils.mapItemsInInventory(packManager, source.getClient());
                        }))
                )
                .then(ClientCommandManager.literal("auto")
                        /* This is disabled for now.
                        .then(ClientCommandManager.literal("command")
                                .then(ClientCommandManager.argument("suggestions", CommandSuggestionsArgumentType.TYPE)
                                        .executes(context -> {
                                            Pair<String, CompletableFuture<Suggestions>> suggestions = CommandSuggestionsArgumentType.getSuggestions(context, "suggestions");
                                            String baseCommand = suggestions.getFirst();
                                            suggestions.getSecond().thenAccept(completed -> {
                                                ItemSuggestionProvider provider = new ItemSuggestionProvider(completed.getList().stream()
                                                        .map(suggestion -> baseCommand.substring(0, suggestion.getRange().getStart()) + suggestion.getText())
                                                        .toList());
                                                packMapper.setItemProvider(provider);
                                                context.getSource().sendFeedback(Component.literal("Running " + provider.queueSize() + " commands to obtain custom items to map"));
                                            });
                                            return 0;
                                        })
                                )
                        )
                         */
                        .then(ClientCommandManager.literal("inventory")
                                .executes(runWithPack(packManager, (source, pack) -> {
                                    packMapper.setItemProvider(InventoryMapper.INSTANCE);
                                    source.sendFeedback(
                                            Component.translatable("feedback.rainbow.automatic_mapping_inventory")
                                                    .withStyle(ChatFormatting.GREEN)
                                    );
                                }))
                        )
                        .then(ClientCommandManager.literal("stop")
                                .executes(runWithPack(packManager, (source, pack) -> {
                                    packMapper.setItemProvider(null);
                                    source.sendFeedback(
                                            Component.translatable("feedback.rainbow.automatic_mapping_none")
                                                    .withStyle(ChatFormatting.GREEN)
                                    );
                                }))
                        )
                )
                .then(ClientCommandManager.literal("finish")
                        .executes(context -> {
                            PackManagerUtils.finishPack(packManager, context.getSource().getClient());
                            return 0;
                        })
                )
        );
    }

    private static Command<FabricClientCommandSource> runWithPack(PackManager manager, BiConsumer<FabricClientCommandSource, BedrockPack> executor) {
        return context -> {
            manager.runOrElse(pack -> executor.accept(context.getSource(), pack),
                    () -> context.getSource().sendError(NO_PACK_CREATED));
            return 0;
        };
    }
}
