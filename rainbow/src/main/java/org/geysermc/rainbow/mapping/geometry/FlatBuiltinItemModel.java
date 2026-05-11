package org.geysermc.rainbow.mapping.geometry;

import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.resources.Identifier;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.mapping.AssetResolver;
import org.jspecify.annotations.Nullable;

public class FlatBuiltinItemModel implements ResolvedModel {
    private static final Identifier FLAT_BUILTIN_GENERATED_MODEL = Rainbow.getModdedIdentifier("item/builtin/generated");
    private final ResolvedModel delegate;
    private final ResolvedModel flatBuiltinGeneratedModel;

    public FlatBuiltinItemModel(AssetResolver assetResolver, ResolvedModel delegate) {
        this.delegate = delegate;
        this.flatBuiltinGeneratedModel = assetResolver.getResolvedModel(FLAT_BUILTIN_GENERATED_MODEL).orElseThrow();
    }

    @Override
    public UnbakedModel wrapped() {
        return delegate.wrapped();
    }

    @Override
    public @Nullable ResolvedModel parent() {
        return delegate.parent();
    }

    @Override
    public String debugName() {
        return delegate.debugName();
    }

    @Override
    public UnbakedGeometry getTopGeometry() {
        return flatBuiltinGeneratedModel.getTopGeometry();
    }
}
