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
