package org.geysermc.rainbow.mapping.texture;

import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.world.item.ItemStackTemplate;
import org.geysermc.rainbow.mapping.PackAssetCache;
import org.geysermc.rainbow.mapping.PackContext;
import org.geysermc.rainbow.mixin.TextureSlotsAccessor;

import java.util.Map;

public class ModelTextureCache extends PackAssetCache<ModelTextureCache.Key, ModelTextures> implements AutoCloseable {

    public ModelTextures load(ItemStackTemplate stack, ResolvedModel model, PackContext context) {
        return getOrCompute(new Key(model), () -> ModelTextures.load(stack, model, context));
    }

    @Override
    public void close() throws Exception {
        for (ModelTextures value : values()) {
            value.close();
        }
        clear();
    }

    public record Key(Map<String, Material> textures) {

        public Key(ResolvedModel model) {
            this(((TextureSlotsAccessor) model.getTopTextureSlots()).getResolvedValues());
        }
    }
}
