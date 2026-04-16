package org.geysermc.rainbow.mapping.geometry;

import com.mojang.math.Transformation;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.mapping.PackContext;
import org.geysermc.rainbow.mapping.texture.ModelTextures;
import org.geysermc.rainbow.mapping.texture.TextureHolder;
import org.geysermc.rainbow.mixin.TextureSlotsAccessor;
import org.geysermc.rainbow.pack.geometry.BedrockGeometry;

import java.util.HashMap;
import java.util.Map;

public class MappedGeometryCache {
    private final Map<GeometryCacheKey, MappedGeometryInstance> cachedGeometry = new HashMap<>();

    public MappedGeometry mapGeometry(Identifier bedrockIdentifier, ResolvedModel model, Transformation transformation, ItemStackTemplate stackToRender, PackContext context) {
        GeometryCacheKey cacheKey = new GeometryCacheKey(model, transformation);
        MappedGeometry cached = cachedGeometry.get(cacheKey);
        if (cached != null) {
            return cached.cachedCopy();
        }

        Identifier modelIdentifier = Identifier.parse(model.debugName());
        Identifier stitchedTexturesIdentifier = modelIdentifier.withSuffix("_stitched");
        String safeIdentifier = Rainbow.bedrockSafeIdentifier(bedrockIdentifier);

        ModelTextures textures = ModelTextures.load(model, context);
        BedrockGeometry geometry = GeometryMapper.mapGeometry(safeIdentifier, "bone", model, transformation, textures);
        TextureHolder icon = context.geometryRenderer().isPresent() ? context.geometryRenderer().orElseThrow().render(modelIdentifier, stackToRender)
                                                                    : TextureHolder.createNonExistent(modelIdentifier);
        MappedGeometryInstance instance = new MappedGeometryInstance(geometry, TextureHolder.createCustom(stitchedTexturesIdentifier, textures.stitched()), icon);
        cachedGeometry.put(cacheKey, instance);
        return instance;
    }

    private record GeometryCacheKey(UnbakedGeometry geometry, Transformation transformation, Map<String, Material> textures) {

        private GeometryCacheKey(ResolvedModel model, Transformation transformation) {
            this(model.getTopGeometry(), transformation, ((TextureSlotsAccessor) model.getTopTextureSlots()).getResolvedValues());
        }
    }
}
