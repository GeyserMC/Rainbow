package org.geysermc.rainbow.mapping.geometry;

import com.mojang.math.Transformation;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.mapping.PackAssetCache;
import org.geysermc.rainbow.mapping.PackContext;
import org.geysermc.rainbow.mapping.texture.ModelTextures;
import org.geysermc.rainbow.mapping.texture.TextureHolder;
import org.geysermc.rainbow.pack.geometry.BedrockGeometry;

public class MappedGeometryCache extends PackAssetCache<MappedGeometryCache.Key, MappedGeometry> {

    public MappedGeometry mapGeometry(Identifier bedrockIdentifier, ResolvedModel model, Transformation transformation, ItemStackTemplate stackToRender, PackContext context) {
        return getOrCompute(new Key(model, transformation), () -> {
            // TODO get rid of this identifiers here, now in ModelTextures, render icon for everything when possible
            Identifier modelIdentifier = Rainbow.getModelIdentifier(model);
            String safeIdentifier = Rainbow.bedrockSafeIdentifier(bedrockIdentifier);

            ModelTextures textures = ModelTextures.load(model, context);
            BedrockGeometry geometry = GeometryMapper.mapGeometry(safeIdentifier, "bone", model, transformation, textures);
            TextureHolder icon = context.geometryRenderer().isPresent() ? context.geometryRenderer().orElseThrow().render(modelIdentifier, stackToRender)
                    : TextureHolder.createNonExistent(modelIdentifier);
            return new MappedGeometryInstance(geometry);
        });
    }

    public record Key(UnbakedGeometry geometry, Transformation transformation) {

        public Key(ResolvedModel model, Transformation transformation) {
            this(model.getTopGeometry(), transformation);
        }
    }
}
