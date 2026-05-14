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
import net.minecraft.world.item.ItemStackTemplate;
import org.geysermc.rainbow.client.PackManager;
import org.geysermc.rainbow.client.PackManagerUtils;
import org.geysermc.rainbow.client.mapper.InventoryMapper;
import org.geysermc.rainbow.client.mapper.PackMapper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;

public class PackGeneratorCommand {

    private static final Component NO_PACK_CREATED = Component.translatable("feedback.rainbow.no_pack", Component.literal("/rainbow create <name>")
            .withStyle(style -> style.withColor(ChatFormatting.BLUE).withUnderlined(true)
                    .withClickEvent(new ClickEvent.SuggestCommand("/rainbow create ")))).withStyle(ChatFormatting.RED);

    public static void register(CommandDispatcher<FabricClientCommandSource> dispatcher, PackManager packManager, PackMapper packMapper) {
        dispatcher.register(ClientCommands.literal("rainbow")
                .then(ClientCommands.literal("create")
                        .then(ClientCommands.argument("name", StringArgumentType.word())
                                .executes(context -> {
                                    String name = StringArgumentType.getString(context, "name");

                                    PackManagerUtils.startPack(name, packManager, context.getSource().getClient());

                                    return 0;
                                })
                        )
                )
                .then(ClientCommands.literal("map")
                        .executes(runWithPack(packManager, (source, pack) -> {
                            PackManagerUtils.mapItemInHand(packManager, source.getClient());
                        }))
                )
                .then(ClientCommands.literal("mapinventory")
                        .executes(runWithPack(packManager, (source, pack) -> {
                            PackManagerUtils.mapItemsInInventory(packManager, source.getClient());
                        }))
                )
                .then(ClientCommands.literal("auto")
                        /* This is disabled for now.
                        .then(ClientCommands.literal("command")
                                .then(ClientCommands.argument("suggestions", CommandSuggestionsArgumentType.TYPE)
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
                                .executes(runWithPack(packManager, (source, _) -> {
                                    packMapper.setItemProvider(InventoryMapper.INSTANCE);
                                    source.sendFeedback(
                                            Component.translatable("feedback.rainbow.automatic_mapping_inventory")
                                                    .withStyle(ChatFormatting.GREEN)
                                    );
                                }))
                        )
                        .then(ClientCommands.literal("stop")
                                .executes(runWithPack(packManager, (source, _) -> {
                                    packMapper.setItemProvider(null);
                                    source.sendFeedback(
                                            Component.translatable("feedback.rainbow.automatic_mapping_none")
                                                    .withStyle(ChatFormatting.GREEN)
                                    );
                                }))
                        )
                )
                .then(ClientCommands.literal("finish")
                        .executes(context -> {
                            PackManagerUtils.finishPack(packManager, context.getSource().getClient());
                            return 0;
                        })
                )
        );
    }

    private static Command<FabricClientCommandSource> runWithPack(PackManager manager, BiConsumer<FabricClientCommandSource, PackManager.RainbowPack> executor) {
        return context -> {
            manager.runOrElse(pack -> executor.accept(context.getSource(), pack),
                    () -> context.getSource().sendError(NO_PACK_CREATED));
            return 0;
        };
    }
}
