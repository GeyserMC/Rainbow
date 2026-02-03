package org.geysermc.rainbow.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.layouts.FrameLayout;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.network.chat.Component;
import org.geysermc.rainbow.client.PackManager;
import org.geysermc.rainbow.client.PackManagerUtils;
import org.geysermc.rainbow.client.mapper.*;
import org.jetbrains.annotations.Nullable;

public class ManagePackScreen extends BaseScreen {
    private static final Component MAP_ITEM_HAND = Component.translatable("menu.rainbow.manage_pack.map_item_hand");
    private static final Component MAP_ITEM_INVENTORY = Component.translatable("menu.rainbow.manage_pack.map_item_inventory");
    private static final Component FINISH_PACK = Component.translatable("menu.rainbow.manage_pack.finish_pack");

    private final PackManager manager;
    private final PackMapper mapper;

    @Nullable
    private String packName;

    public ManagePackScreen(PackManager manager, PackMapper mapper) {
        this(manager, mapper, null);
    }

    public ManagePackScreen(PackManager manager, PackMapper mapper, Runnable onClose) {
        super(Component.translatable("menu.rainbow.manage_pack"), onClose);
        this.manager = manager;
        this.mapper = mapper;
        this.minecraft = Minecraft.getInstance();
        manager.runOrElse(pack -> {
            this.packName = pack.name();
        }, this::showCreatePackScreen);
        this.renderTitle = Component.literal(this.packName);
    }

    @Override
    protected void init() {
        renderTitle();
        GridLayout gridLayout = new GridLayout();
        gridLayout.defaultCellSetting().padding(4, 4, 4, 0);
        GridLayout.RowHelper rowHelper = gridLayout.createRowHelper(2);

        rowHelper.addChild(Button.builder(MAP_ITEM_HAND, (button) -> {
            if (PackManagerUtils.mapItemInHand(manager, minecraft))
                this.minecraft.setScreen(null);
            else this.showCreatePackScreen();
        }).width(204).build(), 2);

        rowHelper.addChild(Button.builder(MAP_ITEM_INVENTORY, (button) -> {
            if (PackManagerUtils.mapItemsInInventory(manager, minecraft))
                this.minecraft.setScreen(null);
            else this.showCreatePackScreen();
        }).width(204).build(), 2);

        rowHelper.addChild(CycleButton.builder((provider) -> {
                    CustomItemProvider itemProvider = ((CustomItemProvider) provider);
                    return itemProvider.name();
                })
                .withInitialValue(mapper.getItemProvider())
                .displayOnlyValue()
                // TODO make this list dyanmic, another mod may add a method of mapping
                .withValues(NoItemProvider.INSTANCE, InventoryMapper.INSTANCE).create(0, 0, 204, 20, Component.empty(), (button, value) -> {
                    CustomItemProvider provider = (CustomItemProvider) value;
                    mapper.setItemProvider(provider);
                }), 2);

        rowHelper.addChild(Button.builder(FINISH_PACK, (button) -> {
            if (PackManagerUtils.finishPack(manager, minecraft))
                this.minecraft.setScreen(null);
            else this.showCreatePackScreen();
        }).width(204).build(), 2);

        gridLayout.arrangeElements();
        FrameLayout.alignInRectangle(gridLayout, 0, 0, this.width, this.height, 0.5F, 0.35F);
        gridLayout.visitWidgets(this::addRenderableWidget);
    }

    private void showCreatePackScreen() {
        this.minecraft.setScreen(new CreatePackScreen(this.manager, this.mapper));
    }
}
