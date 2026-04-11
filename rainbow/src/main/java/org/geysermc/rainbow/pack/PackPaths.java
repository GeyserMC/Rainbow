package org.geysermc.rainbow.pack;

import java.nio.file.Path;
import java.util.Optional;

public record PackPaths(Path mappings, Path packRoot, Path attachables, Path geometry, Path animation,
                        Path manifest, Path itemAtlas, Optional<Path> zipOutput, Optional<Path> languageOutput) {
}
