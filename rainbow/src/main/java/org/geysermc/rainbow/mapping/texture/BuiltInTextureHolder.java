package org.geysermc.rainbow.mapping.texture;

import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import org.geysermc.rainbow.mapping.AssetResolver;

import java.util.Optional;

public class BuiltInTextureHolder extends TextureHolder {
    private final Identifier source;

    public BuiltInTextureHolder(Identifier destination, Identifier source) {
        super(destination);
        this.source = source;
    }

    @Override
    public Optional<TextureResource> load(AssetResolver assetResolver, ProblemReporter reporter) {
        return assetResolver.getPossibleAtlasTextureSafely(source);
    }
}
