package org.geysermc.rainbow.client.accessor;

import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.Identifier;

import java.util.Optional;

// Implemented on ModelManager, since this class doesn't keep the resolved models or unbaked client items after baking, we have to store them manually.
// This comes with some extra memory usage, but Rainbow should only be used to convert packs, so it should be fine
public interface ResolvedModelAccessor {

    Optional<ResolvedModel> rainbow$getResolvedModel(Identifier identifier);

    Optional<ClientItem> rainbow$getClientItem(Identifier identifier);
}
