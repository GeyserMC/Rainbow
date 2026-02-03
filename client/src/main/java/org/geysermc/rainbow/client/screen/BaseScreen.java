package org.geysermc.rainbow.client.screen;

import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class BaseScreen extends Screen {
    final Runnable onClose;

    protected Component renderTitle;

    protected BaseScreen(Component title, Runnable onClose) {
        super(title);
        this.renderTitle = title;
        this.onClose = onClose;
    }

    public void renderTitle() {
        int i = this.font.width(this.renderTitle);
        this.addRenderableWidget(new StringWidget(this.width / 2 - i / 2, 40, i, 9, this.renderTitle, this.font));
    }

    @Override
    public void onClose() {
        if (onClose == null) super.onClose();
        else onClose.run();
    }
}
