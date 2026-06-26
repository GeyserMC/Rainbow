package org.geysermc.rainbow.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import net.minecraft.world.item.component.ResolvableProfile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ResolvableProfile.class)
public interface ResolvableProfileAccessor {

    @Invoker
    Either<GameProfile, ResolvableProfile.Partial> invokeUnpack();
}
