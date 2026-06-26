package org.geysermc.rainbow.client.mixin;

import net.minecraft.client.resources.WaypointStyle;
import net.minecraft.client.resources.WaypointStyleManager;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.waypoints.WaypointStyleAsset;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(WaypointStyleManager.class)
public interface WaypointStyleManagerAccessor {

    @Accessor("MISSING")
    static WaypointStyle getMissing() {
        throw new AssertionError();
    }

    @Accessor
    Map<ResourceKey<WaypointStyleAsset>, WaypointStyle> getWaypointStyles();
}
