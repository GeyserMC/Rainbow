package org.geysermc.rainbow.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;
import org.geysermc.rainbow.client.PackManager;
import org.geysermc.rainbow.client.PackManagerUtils;
import org.geysermc.rainbow.client.mapper.PackMapper;
import org.jetbrains.annotations.Nullable;

public class CreatePackScreen extends BaseScreen {
    private static final Component PACK_NAME = Component.translatable("menu.rainbow.create_pack.pack_name");
    private static final Component CREATE = Component.translatable("menu.rainbow.create_pack.create");
    private static final Component CANCEL = Component.translatable("menu.rainbow.create_pack.cancel");

    private final PackManager manager;
    private final PackMapper mapper;

    @Nullable
    private EditBox PACK_NAME_EDIT_BOX;

    public CreatePackScreen(PackManager manager, PackMapper mapper) {
        this(manager, mapper, null);
    }

    public CreatePackScreen(PackManager manager, PackMapper mapper, Runnable onClose) {
        super(Component.translatable("menu.rainbow.create_pack"), onClose);
        this.manager = manager;
        this.mapper = mapper;
        this.minecraft = Minecraft.getInstance();
        if (manager.isInProgress()) this.minecraft.setScreen(new ManagePackScreen(manager, mapper));
    }

    @Override
    protected void init() {
        renderTitle();
        GridLayout gridLayout = new GridLayout();
        gridLayout.defaultCellSetting().padding(4, 4, 4, 0);
        GridLayout.RowHelper rowHelper = gridLayout.createRowHelper(2);
        rowHelper.addChild(PACK_NAME_EDIT_BOX = new EditBox(this.minecraft.font, 204, 20, PACK_NAME), 2);

        rowHelper.addChild(Button.builder(CREATE, (button) -> {
            if (PackManagerUtils.startPack(PACK_NAME_EDIT_BOX.getValue(), this.manager, this.minecraft)) {
                if (this.onClose == null)
                    this.minecraft.setScreen(new ManagePackScreen(this.manager, this.mapper));
                else this.onClose.run();
            } else this.minecraft.setScreen(null);
        }).width(98).build(), 1);

        rowHelper.addChild(Button.builder(CANCEL, (button) -> {
            this.minecraft.setScreen(null);
        }).width(98).build(), 1);

        gridLayout.arrangeElements();
        FrameLayout.alignInRectangle(gridLayout, 0, 0, this.width, this.height, 0.5F, 0.35F);
        gridLayout.visitWidgets(this::addRenderableWidget);
    }
}
