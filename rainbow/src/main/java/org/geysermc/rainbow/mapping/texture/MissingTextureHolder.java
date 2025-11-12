package org.geysermc.rainbow.mapping.texture;

import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import org.geysermc.rainbow.mapping.AssetResolver;

import java.util.Optional;

public class MissingTextureHolder extends TextureHolder {

    public MissingTextureHolder(Identifier identifier) {
        super(identifier);
    }

    @Override
    public Optional<byte[]> load(AssetResolver assetResolver, ProblemReporter reporter) {
        reportMissing(reporter);
        return Optional.empty();
    }
}
