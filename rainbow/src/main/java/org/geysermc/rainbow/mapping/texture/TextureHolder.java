package org.geysermc.rainbow.mapping.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.image.NativeImageUtil;
import org.geysermc.rainbow.mapping.AssetResolver;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public abstract class TextureHolder implements PackSerializer.Serializable {
    protected final Identifier destination;

    public TextureHolder(Identifier destination) {
        this.destination = destination;
    }

    public abstract Optional<TextureResource> load(AssetResolver assetResolver, ProblemReporter reporter);

    @Override
    public CompletableFuture<?> save(PackSerializingContext context) {
        return load(context.assetResolver(), context.reporter())
                .flatMap(texture -> {
                    try (texture) {
                        try (NativeImage firstFrame = texture.getFirstFrame()) {
                            return RainbowIO.safeIO(() -> context.serializer().saveTexture(NativeImageUtil.writeToByteArray(firstFrame), context.paths().texturePath(this)));
                        }
                    }
                })
                .orElseGet(() -> {
                    if (shouldReportMissingWhenAbsent()) {
                        reportMissing(context.reporter());
                    }
                    return PackSerializer.noop();
                });
    }

    protected boolean shouldReportMissingWhenAbsent() {
        return true;
    }

    public static TextureHolder createCustom(Identifier destination, Supplier<NativeImage> supplier) {
        return new CustomTextureHolder(destination, supplier);
    }

    public static TextureHolder createBuiltIn(Identifier destination, Identifier source) {
        return new BuiltInTextureHolder(destination, source);
    }

    public static TextureHolder createBuiltIn(Identifier source) {
        return createBuiltIn(source, source);
    }

    public static TextureHolder createNonExistent(Identifier destination) {
        return new MissingTextureHolder(destination);
    }

    public static TextureHolder createCopy(TextureHolder original) {
        return new CopyTextureHolder(original.destination);
    }

    public Identifier destination() {
        return destination;
    }

    protected void reportMissing(ProblemReporter reporter) {
        reporter.report(() -> "missing texture for " + destination + "; please provide it manually");
    }
}
