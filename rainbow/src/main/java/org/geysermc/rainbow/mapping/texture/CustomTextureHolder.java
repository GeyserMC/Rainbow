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

    public CustomTextureHolder(Identifier identifier, Supplier<NativeImage> supplier) {
        super(identifier);
        this.supplier = supplier;
    }

    @Override
    public Optional<byte[]> load(AssetResolver assetResolver, ProblemReporter reporter) {
        return RainbowIO.safeIO(() -> {
            NativeImage texture;
            try {
                texture = supplier.get();
            } catch (Exception exception) {
                reporter.report(() -> "unable to get texture for " + identifier + "; please provide it manually");
                return null;
            }
            return NativeImageUtil.writeToByteArray(texture);
        });
    }
}
