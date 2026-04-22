package org.geysermc.rainbow.mapping.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.image.NativeImageUtil;
import org.geysermc.rainbow.mapping.AssetResolver;

import java.util.Optional;
import java.util.function.Supplier;

public class CustomTextureHolder extends TextureHolder {
    private final Supplier<NativeImage> supplier;

    public CustomTextureHolder(Identifier destination, Supplier<NativeImage> supplier) {
        super(destination);
        this.supplier = supplier;
    }

    @Override
    public Optional<TextureResource> load(AssetResolver assetResolver, ProblemReporter reporter) {
        NativeImage texture;
        try {
            texture = supplier.get();
        } catch (Exception exception) {
            reporter.report(() -> "unable to get texture for " + destination + "; please provide it manually");
            return Optional.empty();
        }
        return Optional.of(TextureResource.createNonAnimated(texture));
    }

    @Override
    protected boolean shouldReportMissingWhenAbsent() {
        return false;
    }
}
