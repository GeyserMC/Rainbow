package org.geysermc.rainbow.mapping.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import org.geysermc.rainbow.mapping.AssetResolver;
import org.geysermc.rainbow.mapping.PackSerializer;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public abstract class TextureHolder {
    protected final Identifier identifier;

    public TextureHolder(Identifier identifier) {
        this.identifier = identifier;
    }

    public abstract Optional<byte[]> load(AssetResolver assetResolver, ProblemReporter reporter);

    public CompletableFuture<?> save(AssetResolver assetResolver, PackSerializer serializer, Path path, ProblemReporter reporter) {
        return load(assetResolver, reporter)
                .map(bytes -> serializer.saveTexture(bytes, path))
                .orElse(CompletableFuture.completedFuture(null));
    }

    public static TextureHolder createCustom(Identifier identifier, Supplier<NativeImage> supplier) {
        return new CustomTextureHolder(identifier, supplier);
    }

    public static TextureHolder createBuiltIn(Identifier identifier, Identifier atlas, Identifier source) {
        return new BuiltInTextureHolder(identifier, atlas, source);
    }

    public static TextureHolder createBuiltIn(Identifier atlas, Identifier identifier) {
        return createBuiltIn(identifier, atlas, identifier);
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
