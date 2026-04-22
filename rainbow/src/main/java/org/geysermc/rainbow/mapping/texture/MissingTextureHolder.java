package org.geysermc.rainbow.mapping.texture;

import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import org.geysermc.rainbow.mapping.AssetResolver;

import java.util.Optional;

public class MissingTextureHolder extends TextureHolder {

    public MissingTextureHolder(Identifier destination) {
        super(destination);
    }

    @Override
    public Optional<TextureResource> load(AssetResolver assetResolver, ProblemReporter reporter) {
        return Optional.empty();
    }
}
