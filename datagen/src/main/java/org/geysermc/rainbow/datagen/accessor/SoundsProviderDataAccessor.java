package org.geysermc.rainbow.datagen.accessor;

import net.fabricmc.fabric.impl.datagen.client.SoundTypeBuilderImpl;

import java.util.Map;

public interface SoundsProviderDataAccessor {

    Map<String, Map<String, SoundTypeBuilderImpl.SoundType>> rainbow$getData();
}
