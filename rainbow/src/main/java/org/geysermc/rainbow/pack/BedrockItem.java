package org.geysermc.rainbow.pack;

import net.minecraft.resources.Identifier;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;
import org.geysermc.rainbow.mapping.attachable.AttachableMapper;
import org.geysermc.rainbow.mapping.geometry.BedrockGeometryContext;
import org.geysermc.rainbow.mapping.texture.TextureHolder;
import org.geysermc.rainbow.pack.attachable.BedrockAttachable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record BedrockItem(Identifier identifier, String textureName, BedrockGeometryContext geometryContext,
                          AttachableMapper.AttachableCreator attachableCreator) implements PackSerializer.Serializable {

    @Override
    public CompletableFuture<?> save(PackSerializingContext context) {
        List<TextureHolder> attachableTextures = new ArrayList<>();
        Optional<BedrockAttachable> createdAttachable = attachableCreator.create(identifier, attachableTextures::add);

        return geometryContext.icon()
                .with(createdAttachable)
                .with(attachableTextures)
                .with(geometryContext)
                .save(context);
    }
}
