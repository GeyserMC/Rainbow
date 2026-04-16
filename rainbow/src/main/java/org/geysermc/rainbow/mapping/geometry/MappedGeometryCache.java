package org.geysermc.rainbow.mapping.geometry;

import com.mojang.math.Transformation;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.resources.Identifier;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.mapping.PackAssetCache;
import org.geysermc.rainbow.mapping.PackContext;
import org.geysermc.rainbow.mapping.texture.ModelTextures;
import org.geysermc.rainbow.pack.geometry.BedrockGeometry;

public class MappedGeometryCache extends PackAssetCache<MappedGeometryCache.Key, MappedGeometry> {

    public MappedGeometry mapGeometry(Identifier bedrockIdentifier, ResolvedModel model, Transformation transformation, ModelTextures textures, PackContext context) {
        return getOrCompute(new Key(model, transformation), () -> {
            String safeIdentifier = Rainbow.bedrockSafeIdentifier(bedrockIdentifier);
            BedrockGeometry geometry = GeometryMapper.mapGeometry(safeIdentifier, "bone", model, transformation, textures);
            return new MappedGeometryInstance(geometry);
        });
    }

    public record Key(UnbakedGeometry geometry, Transformation transformation) {

        public Key(ResolvedModel model, Transformation transformation) {
            this(model.getTopGeometry(), transformation);
        }
    }
}
