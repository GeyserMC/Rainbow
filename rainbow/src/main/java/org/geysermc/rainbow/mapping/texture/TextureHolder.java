package org.geysermc.rainbow.mapping.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.image.NativeImageUtil;
import org.geysermc.rainbow.mapping.AssetResolver;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;
import org.geysermc.rainbow.pack.texture.BedrockTextures;
import org.jspecify.annotations.Nullable;

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
                        return RainbowIO.safeIO(() -> context.serializer().saveTexture(NativeImageUtil.writeToByteArray(texture.texture()), context.paths().texturePath(this)));
                    }
                })
                .orElseGet(() -> {
                    if (shouldReportMissingWhenAbsent()) {
                        reportMissing(context.reporter());
                    }
                    return PackSerializer.noop();
                });
    }

    public TextureHolder copy() {
        return new CopyTextureHolder(destination);
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

    public Identifier destination() {
        return destination;
    }

    public String bedrockSafeDestination() {
        return bedrockSafeDestination(destination);
    }

    public static String bedrockSafeDestination(Identifier destination) {
        return BedrockTextures.TEXTURES_FOLDER + destination.getPath();
    }

    public String bedrockSafeName() {
        return Rainbow.bedrockSafeIdentifier(destination);
    }

    protected void reportMissing(ProblemReporter reporter) {
        reporter.report(() -> "missing texture for " + destination + "; please provide it manually");
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof TextureHolder that)) {
            return false;
        }
        return destination.equals(that.destination);
    }

    @Override
    public int hashCode() {
        return destination.hashCode();
    }
}
