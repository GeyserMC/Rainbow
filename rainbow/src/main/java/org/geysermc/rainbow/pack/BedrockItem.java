package org.geysermc.rainbow.pack;

import net.minecraft.resources.Identifier;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;
import org.geysermc.rainbow.mapping.attachable.BedrockAttachableContext;
import org.geysermc.rainbow.mapping.geometry.BedrockGeometryContext;
import org.geysermc.rainbow.mapping.texture.ItemModelTextures;
import org.geysermc.rainbow.mapping.texture.TextureHolder;

import java.util.concurrent.CompletableFuture;

public record BedrockItem(Identifier identifier, TextureHolder icon, ItemModelTextures textures, BedrockGeometryContext geometryContext,
                          BedrockAttachableContext attachableContext) implements PackSerializer.Serializable {

    @Override
    public CompletableFuture<?> save(PackSerializingContext context) {
        return icon
                .with(textures)
                .with(geometryContext)
                .with(attachableContext)
                .save(context);
    }
}
