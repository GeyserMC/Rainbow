package org.geysermc.rainbow.pack;

import net.minecraft.resources.Identifier;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.mapping.texture.TextureHolder;
import org.geysermc.rainbow.pack.attachable.BedrockAttachable;
import org.geysermc.rainbow.pack.geometry.BedrockGeometry;

import java.nio.file.Path;
import java.util.Optional;

public record PackPaths(Path mappings, Path packRoot, Path attachables, Path geometries, Path animations,
                        Path manifest, Path itemAtlas, Optional<Path> zipOutput, Optional<Path> languageOutput) {

    public Path animationPath(String identifier) {
        return animations.resolve(identifier + ".animation.json");
    }

    public Path attachablePath(BedrockAttachable attachable) {
        return attachables.resolve(Rainbow.bedrockSafeIdentifier(attachable.info().identifier()) + ".json");
    }

    public Path geometryPath(BedrockGeometry geometry) {
        return geometries.resolve(geometry.definitions().getFirst().info().identifier() + ".geo.json");
    }

    public Path texturePath(TextureHolder texture) {
        Identifier textureIdentifier = Rainbow.decorateTextureIdentifier(texture.location());
        // TODO proper texture folder field
        return packRoot.resolve(textureIdentifier.getPath());
    }
}
