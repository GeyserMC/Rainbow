package org.geysermc.rainbow.mapping.texture;

import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import org.geysermc.rainbow.mapping.PackAssetCache;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mixin.TextureSlotsAccessor;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public interface ModelTextures<T extends ModelTextures<T>> extends PackAssetCache.Cacheable<T>, PackSerializer.Serializable {

    int width();

    int height();

    Optional<SpriteInfo> getSprite(String key);

    boolean requiresAttachable();

    record SpriteInfo(int x, int y, int width, int height) {

        public SpriteInfo(TextureResource texture) {
            this(0, 0, texture.sizeOfFrame().width(), texture.sizeOfFrame().height());
        }
    }

    static Map<String, Material> getCleanMaterials(TextureSlots textures) {
        Map<String, Material> materials = new HashMap<>(((TextureSlotsAccessor) textures).getResolvedValues());
        materials.remove(UnbakedModel.PARTICLE_TEXTURE_REFERENCE);
        return materials;
    }

    static String sanitizeMaterialReference(String reference) {
        if (TextureSlotsAccessor.invokeIsTextureReference(reference)) {
            return reference.substring(1);
        }
        return reference;
    }
}
