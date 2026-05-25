package org.geysermc.rainbow.client.accessor;

import net.minecraft.client.resources.sounds.SoundEventRegistration;

import java.util.Map;

public interface SoundManagerAccessor {

    Map<String, Map<String, SoundEventRegistration>> rainbow$getRawRegistrations();
}
