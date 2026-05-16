package org.geysermc.rainbow.pack;

import net.minecraft.resources.Identifier;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.mapping.texture.TextureHolder;
import org.geysermc.rainbow.pack.attachable.BedrockAttachable;

import java.nio.file.Path;
import java.util.Optional;

public record PackPaths(Path mappingsRoot, Path packRoot, Optional<Path> zipOutput, Optional<Path> languageOutput) {
    private static final Path BLOCK_MAPPINGS = Path.of("geyser_block_mappings.json");
    private static final Path ITEM_MAPPINGS = Path.of("geyser_item_mappings.json");

    private static final Path ANIMATION_DIRECTORY = Path.of("animations");
    private static final Path ATTACHABLES_DIRECTORY = Path.of("attachables");
    private static final Path GEOMETRY_DIRECTORY = Path.of("models/entity");
    private static final Path RENDER_CONTROLLERS_DIRECTORY = Path.of("render_controllers");

    private static final Path MANIFEST = Path.of("manifest.json");
    private static final Path ITEM_ATLAS = Path.of("textures/item_texture.json");
    private static final Path TERRAIN_ATLAS = Path.of("textures/terrain_texture.json");

    public Path blockMappings() {
        return mappingsRoot.resolve(BLOCK_MAPPINGS);
    }

    public Path itemMappings() {
        return mappingsRoot.resolve(ITEM_MAPPINGS);
    }

    public Path animation(String identifier) {
        return packRoot.resolve(ANIMATION_DIRECTORY, Path.of(identifier + ".animation.json"));
    }

    public Path attachable(BedrockAttachable attachable) {
        return packRoot.resolve(ATTACHABLES_DIRECTORY, Path.of(Rainbow.bedrockSafeIdentifier(attachable.info().identifier()) + ".json"));
    }

    public Path geometry(String identifier) {
        return packRoot.resolve(GEOMETRY_DIRECTORY, Path.of(identifier + ".geo.json"));
    }

    public Path renderControllers(String identifier) {
        return packRoot.resolve(RENDER_CONTROLLERS_DIRECTORY, Path.of(identifier + ".render_controllers.json"));
    }

    public Path texturePath(TextureHolder texture) {
        Identifier textureIdentifier = Rainbow.decorateTextureIdentifier(texture.destination());
        return packRoot.resolve(textureIdentifier.getPath());
    }

    public Path manifest() {
        return packRoot.resolve(MANIFEST);
    }

    public Path itemAtlas() {
        return packRoot.resolve(ITEM_ATLAS);
    }

    public Path terrainAtlas() {
        return packRoot.resolve(TERRAIN_ATLAS);
    }
}
