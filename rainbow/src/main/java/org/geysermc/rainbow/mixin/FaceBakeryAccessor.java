package org.geysermc.rainbow.mixin;

import net.minecraft.client.resources.model.cuboid.CuboidFace;
import net.minecraft.client.resources.model.cuboid.FaceBakery;
import net.minecraft.core.Direction;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FaceBakery.class)
public interface FaceBakeryAccessor {

    @Invoker
    static CuboidFace.UVs invokeDefaultFaceUV(Vector3fc posFrom, Vector3fc posTo, Direction facing) {
        throw new AssertionError();
    }
}
