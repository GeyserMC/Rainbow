package org.geysermc.rainbow.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;
import net.minecraft.network.chat.ClickEvent;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.client.command.CommandSuggestionsArgumentType;
import org.geysermc.rainbow.client.command.PackGeneratorCommand;
import org.geysermc.rainbow.client.mapper.PackMapper;
import org.geysermc.rainbow.client.screen.CreatePackScreen;
import org.geysermc.rainbow.client.screen.ManagePackScreen;
import org.slf4j.Logger;

public class RainbowClient implements ClientModInitializer {
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final ClickEvent LOG_CLICK_EVENT = new ClickEvent.OpenFile(Minecraft.getInstance().gameDirectory.toPath().resolve("logs/latest.log"));

    private static final PackManager packManager = new PackManager();
    private static final PackMapper packMapper = new PackMapper(packManager);

    public static PackManager getPackManager() {
        return packManager;
    }
    public static PackMapper getPackMapper() {
        return packMapper;
    }

    // TODO export language overrides
    @Override
    public void onInitializeClient() {
        KeyMapping openPackScreenKeyBind = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.rainbow.open_pack_screen", InputConstants.KEY_O, KeyMapping.Category.MISC));

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, buildContext) -> PackGeneratorCommand.register(dispatcher, packManager, packMapper));
        ClientTickEvents.START_CLIENT_TICK.register(packMapper::tick);
        ClientTickEvents.END_CLIENT_TICK.register(minecraft -> {
            if (minecraft.player == null) return;
            if (minecraft.screen != null) return;

            if (openPackScreenKeyBind.consumeClick()) {
                if (packManager.isInProgress()) minecraft.setScreen(new ManagePackScreen(packManager, packMapper));
                else minecraft.setScreen(new CreatePackScreen(packManager, packMapper));
            }
        });

        ArgumentTypeRegistry.registerArgumentType(Rainbow.getModdedIdentifier("command_suggestions"),
                CommandSuggestionsArgumentType.class, SingletonArgumentInfo.contextFree(CommandSuggestionsArgumentType::new));

        RainbowIO.registerExceptionListener(new RainbowClientIOHandler());
    }
}
