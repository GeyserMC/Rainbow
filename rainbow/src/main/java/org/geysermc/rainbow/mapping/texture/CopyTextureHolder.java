package org.geysermc.rainbow.mapping.texture;

import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import org.geysermc.rainbow.mapping.AssetResolver;
import org.geysermc.rainbow.mapping.PackSerializer;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class CopyTextureHolder extends TextureHolder {

    public CopyTextureHolder(Identifier identifier) {
        super(identifier);
    }

    @Override
    public Optional<byte[]> load(AssetResolver assetResolver, ProblemReporter reporter) {
        return Optional.empty();
    }

    @Override
    public CompletableFuture<?> save(AssetResolver assetResolver, PackSerializer serializer, Path path, ProblemReporter reporter) {
        return CompletableFuture.completedFuture(null);
    }
}
