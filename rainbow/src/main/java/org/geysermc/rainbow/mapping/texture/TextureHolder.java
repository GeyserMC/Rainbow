package org.geysermc.rainbow.mapping.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import org.geysermc.rainbow.mapping.AssetResolver;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public abstract class TextureHolder implements PackSerializer.Serializable {
    protected final Identifier identifier;

    public TextureHolder(Identifier identifier) {
        this.identifier = identifier;
    }

    public abstract Optional<byte[]> load(AssetResolver assetResolver, ProblemReporter reporter);

    @Override
    public CompletableFuture<?> save(PackSerializingContext context) {
        return load(context.assetResolver(), context.reporter())
                .map(bytes -> context.serializer().saveTexture(bytes, context.paths().texturePath(this)))
                .orElse(PackSerializer.noop());
    }

    public static TextureHolder createCustom(Identifier identifier, Supplier<NativeImage> supplier) {
        return new CustomTextureHolder(identifier, supplier);
    }

    public static TextureHolder createBuiltIn(Identifier identifier, Identifier source) {
        return new BuiltInTextureHolder(identifier, source);
    }

    public static TextureHolder createBuiltIn(Identifier identifier) {
        return createBuiltIn(identifier, identifier);
    }

    public static TextureHolder createNonExistent(Identifier identifier) {
        return new MissingTextureHolder(identifier);
    }

    public static TextureHolder createCopy(TextureHolder original) {
        return new CopyTextureHolder(original.identifier);
    }

    public Identifier location() {
        return identifier;
    }

    protected void reportMissing(ProblemReporter reporter) {
        reporter.report(() -> "missing texture for " + identifier + "; please provide it manually");
    }
}
