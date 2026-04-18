package org.geysermc.rainbow.pack;

import net.minecraft.resources.Identifier;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;
import org.geysermc.rainbow.mapping.attachable.BedrockAttachableContext;
import org.geysermc.rainbow.mapping.geometry.BedrockGeometryContext;
import org.geysermc.rainbow.mapping.texture.ModelTextures;

import java.util.concurrent.CompletableFuture;

public record BedrockItem(Identifier identifier, String textureName, ModelTextures textures, BedrockGeometryContext geometryContext,
                          BedrockAttachableContext attachableContext) implements PackSerializer.Serializable, AutoCloseable {

    @Override
    public CompletableFuture<?> save(PackSerializingContext context) {
        //List<TextureHolder> attachableTextures = new ArrayList<>();
        //Optional<BedrockAttachable> createdAttachable = attachableCreator.create(identifier, attachableTextures::add);

        return textures
                .with(geometryContext)
                .with(attachableContext)
                .save(context);
    }

    // FIXME this method is never called
    @Override
    public void close() throws Exception {
        // Is closed here and in the cache, which is in the context
        // Where do we close this?
        textures.close();
    }
}
