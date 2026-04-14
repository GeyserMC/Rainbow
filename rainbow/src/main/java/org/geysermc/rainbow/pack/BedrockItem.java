package org.geysermc.rainbow.pack;

import net.minecraft.resources.Identifier;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.attachable.AttachableMapper;
import org.geysermc.rainbow.mapping.geometry.BedrockGeometryContext;
import org.geysermc.rainbow.mapping.texture.TextureHolder;
import org.geysermc.rainbow.mapping.texture.TextureSerializer;
import org.geysermc.rainbow.pack.attachable.BedrockAttachable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record BedrockItem(Identifier identifier, String textureName, BedrockGeometryContext geometryContext, AttachableMapper.AttachableCreator attachableCreator) {

    public CompletableFuture<?> save(PackSerializer serializer, PackPaths paths, TextureSerializer textureSerializer) {
        List<TextureHolder> attachableTextures = new ArrayList<>();
        Optional<BedrockAttachable> createdAttachable = attachableCreator.create(identifier, attachableTextures::add);
        return CompletableFuture.allOf(
                textureSerializer.apply(geometryContext.icon()),
                createdAttachable.map(attachable -> attachable.save(serializer, paths.attachables())).orElse(noop()),
                CompletableFuture.allOf(attachableTextures.stream().map(textureSerializer).toArray(CompletableFuture[]::new)),
                geometryContext.geometry().map(geometry -> geometry.save(serializer, paths.geometry(), textureSerializer)).orElse(noop()),
                geometryContext.animation().map(context -> context.animation().save(serializer, paths.animation(), Rainbow.bedrockSafeIdentifier(identifier))).orElse(noop())
        );
    }

    private static <T> CompletableFuture<T> noop() {
        return CompletableFuture.completedFuture(null);
    }
}
