package org.geysermc.rainbow.client.mixin;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.geysermc.rainbow.client.PackManagerUtils;
import org.geysermc.rainbow.client.RainbowClient;
import org.geysermc.rainbow.client.screen.CreatePackScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.atomic.AtomicReference;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin extends Screen {
    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(
            method = "init()V",
            at = @At("TAIL")
    )
    private void injectInit(CallbackInfo ci) {
        this.addRenderableWidget(Button.builder(
                Component.translatable("menu.rainbow.container.map_items"),
                (button) -> {
                    if (!PackManagerUtils.mapItemsInInventory(RainbowClient.getPackManager(), this.minecraft)) {
                        AtomicReference<Screen> current = new AtomicReference<>(this.minecraft.screen);

                        this.minecraft.setScreen(new CreatePackScreen(
                                RainbowClient.getPackManager(),
                                RainbowClient.getPackMapper(),
                                () -> {
                                    this.minecraft.setScreen(current.get());
                                    PackManagerUtils.mapItemsInInventory(RainbowClient.getPackManager(), this.minecraft);
                                }
                        ));
                    }
                }
        ).bounds(5, 5, 74, 20).build());
    }
}
