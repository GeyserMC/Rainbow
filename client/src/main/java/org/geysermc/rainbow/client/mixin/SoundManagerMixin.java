package org.geysermc.rainbow.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.geysermc.rainbow.client.accessor.SoundManagerAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(SoundManager.class)
public abstract class SoundManagerMixin extends SimplePreparableReloadListener<Object> implements SoundManagerAccessor {
    @Unique
    private final Map<String, Map<String, SoundEventRegistration>> rawRegistrations = new Object2ObjectOpenHashMap<>();

    @Inject(method = "prepare(Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)Lnet/minecraft/client/sounds/SoundManager$Preparations;", at = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;"))
    public void storeRawRegistrations(ResourceManager manager, ProfilerFiller profiler, CallbackInfoReturnable<Object> callbackInfoReturnable,
                                      @Local(name = "namespace") String namespace, @Local(name = "map") Map<String, SoundEventRegistration> map) {
        rawRegistrations.put(namespace, map);
    }

    @Override
    public Map<String, Map<String, SoundEventRegistration>> rainbow$getRawRegistrations() {
        return rawRegistrations;
    }
}
