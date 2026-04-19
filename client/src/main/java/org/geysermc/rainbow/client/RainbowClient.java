package org.geysermc.rainbow.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.client.command.CommandSuggestionsArgumentType;
import org.geysermc.rainbow.client.command.PackGeneratorCommand;
import org.geysermc.rainbow.client.mapper.PackMapper;

public class RainbowClient implements ClientModInitializer {

    public static final String MOD_ID = Rainbow.MOD_ID + "-client";

    private final PackManager packManager = new PackManager();
    private final PackMapper packMapper = new PackMapper(packManager);

    // TODO export language overrides
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, _) -> PackGeneratorCommand.register(dispatcher, packManager, packMapper));
        ClientTickEvents.START_CLIENT_TICK.register(packMapper::tick);

        ArgumentTypeRegistry.registerArgumentType(Rainbow.getModdedIdentifier("command_suggestions"),
                CommandSuggestionsArgumentType.class, SingletonArgumentInfo.contextFree(CommandSuggestionsArgumentType::new));

        RainbowIO.registerExceptionListener(new RainbowClientIOHandler());
    }

    public static String getVersion() {
        return FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown_sadface");
    }
}
