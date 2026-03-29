package org.geysermc.rainbow.client.mixin;

import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.ScreenArea;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(GuiItemRenderState.class)
public abstract class GuiItemRenderStateMixin implements ScreenArea {

    @ModifyConstant(method = "calculateOversizedItemBounds", constant = @Constant(intValue = 16))
    public int neverReturnNull(int i) {
        return -1;
    }
}
