package org.geysermc.rainbow.mapping.texture;

import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.sprite.Material;
import org.geysermc.rainbow.mapping.PackAssetCache;
import org.geysermc.rainbow.mixin.TextureSlotsAccessor;
import org.geysermc.rainbow.stats.PackStatKey;

import java.util.Map;
import java.util.function.Supplier;

public class ModelTextureCache<T extends ModelTextures<T>> extends PackAssetCache<ModelTextureCache.Key, T> {

    public ModelTextureCache(PackStatKey.AssetCacheStatKey statKey) {
        super(statKey);
    }

    public T load(ResolvedModel model, Supplier<T> computer) {
        return getOrCompute(new Key(model), computer);
    }

    public record Key(Map<String, Material> textures) {

        private Key(ResolvedModel model) {
            this(((TextureSlotsAccessor) model.getTopTextureSlots()).getResolvedValues());
        }
    }
}
