package org.geysermc.rainbow.client.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.geysermc.rainbow.client.PackManager;
import org.geysermc.rainbow.client.mapper.InventoryMapper;
import org.geysermc.rainbow.client.mapper.PackMapper;
import org.geysermc.rainbow.pack.BedrockPack;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.BiConsumer;

public class PackGeneratorCommand {

    private static final Component NO_PACK_CREATED = Component.translatable("commands.rainbow.no_pack", Component.literal("/rainbow create <name>")
            .withStyle(style -> style.withColor(ChatFormatting.BLUE).withUnderlined(true)
                    .withClickEvent(new ClickEvent.SuggestCommand("/rainbow create "))));

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, PackManager packManager, PackMapper packMapper) {
        dispatcher.register(ClientCommands.literal("rainbow")
                .then(ClientCommands.literal("create")
                        .then(ClientCommands.argument("name", StringArgumentType.word())
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "name");
                                    try {
                                        packManager.startPack(name);
                                    } catch (Exception exception) {
                                        context.getSource().sendError(Component.translatable("commands.rainbow.create_pack_failed"));
                                        throw new RuntimeException(exception);
                                    }
                                    context.getSource().sendFeedback(Component.translatable("commands.rainbow.pack_created", name));
                                    return 0;
                                })
                        )
                )
                .then(ClientCommands.literal("map")
                        .executes(runWithPack(packManager, (source, pack) -> {
                            ItemStack heldItem = source.getPlayer().getMainHandItem();
                            switch (pack.map(heldItem)) {
                                case NONE_MAPPED -> source.sendError(Component.translatable("commands.rainbow.no_item_mapped"));
                                case PROBLEMS_OCCURRED -> source.sendFeedback(Component.translatable("commands.rainbow.mapped_held_item_problems"));
                                case MAPPED_SUCCESSFULLY -> source.sendFeedback(Component.translatable("commands.rainbow.mapped_held_item"));
                            }
                        }))
                )
                .then(ClientCommands.literal("mapinventory")
                        .executes(runWithPack(packManager, (source, pack) -> {
                            int mapped = 0;
                            boolean errors = false;
                            Inventory inventory = source.getPlayer().getInventory();

                            for (ItemStack stack : inventory) {
                                BedrockPack.MappingResult result = pack.map(stack);
                                if (result != BedrockPack.MappingResult.NONE_MAPPED) {
                                    mapped++;
                                    if (result == BedrockPack.MappingResult.PROBLEMS_OCCURRED) {
                                        errors = true;
                                    }
                                }
                            }

                            if (mapped > 0) {
                                source.sendFeedback(Component.translatable("commands.rainbow.mapped_items_from_inventory", mapped));
                                if (errors) {
                                    source.sendFeedback(Component.translatable("commands.rainbow.mapped_items_problems"));
                                }
                            } else {
                                source.sendError(Component.translatable("commands.rainbow.no_items_mapped"));
                            }
                        }))
                )
                .then(ClientCommands.literal("auto")
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
                        .then(ClientCommands.literal("inventory")
                                .executes(runWithPack(packManager, (source, pack) -> {
                                    packMapper.setItemProvider(InventoryMapper.INSTANCE);
                                    source.sendFeedback(Component.translatable("commands.rainbow.automatic_inventory_mapping"));
                                }))
                        )
                        .then(ClientCommands.literal("stop")
                                .executes(runWithPack(packManager, (source, pack) -> {
                                    packMapper.setItemProvider(null);
                                    source.sendFeedback(Component.translatable("commands.rainbow.stopped_automatic_mapping"));
                                }))
                        )
                )
                .then(ClientCommands.literal("finish")
                        .executes(context -> {
                            Optional<Path> exportPath = packManager.getExportPath();
                            Runnable onFinish = () -> context.getSource().sendFeedback(Component.translatable("commands.rainbow.pack_finished_successfully").withStyle(style
                                    -> style.withUnderlined(true).withClickEvent(new ClickEvent.OpenFile(exportPath.orElseThrow()))));
                            if (!packManager.finish(onFinish)) {
                                context.getSource().sendError(NO_PACK_CREATED);
                            }
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
