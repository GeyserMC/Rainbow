package org.geysermc.rainbow.mapping.geometry;

import com.mojang.math.Transformation;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.mapping.PackContext;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;
import org.geysermc.rainbow.mapping.animation.AnimationMapper;
import org.geysermc.rainbow.mapping.animation.BedrockAnimationContext;
import org.geysermc.rainbow.mapping.texture.TextureHolder;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public record BedrockGeometryContext(Optional<MappedGeometry> geometry,
                                     Optional<BedrockAnimationContext> animation, TextureHolder icon,
                                     boolean handheld) implements PackSerializer.Serializable {
    private static final List<Identifier> HANDHELD_MODELS = Stream.of("item/handheld", "item/handheld_rod", "item/handheld_mace")
            .map(Identifier::withDefaultNamespace)
            .toList();

    @Override
    public CompletableFuture<?> save(PackSerializingContext context) {
        return PackSerializer.Serializable.wrapOptional(geometry)
                .with(animation) // TODO maybe move this out of this
                .save(context);
    }

    public static BedrockGeometryContext create(Identifier bedrockIdentifier, ResolvedModel model, Transformation definitionTransformation, ItemStackTemplate stackToRender, PackContext context) {
        ResolvedModel parentModel = model.parent();
        // debugName() returns the resource location of the model as a string
        boolean handheld = parentModel != null && HANDHELD_MODELS.contains(Identifier.parse(parentModel.debugName()));

        TextureSlots textures = model.getTopTextureSlots();
        Material layer0Texture = textures.getMaterial("layer0");
        Optional<MappedGeometry> geometry;
        Optional<BedrockAnimationContext> animation;
        TextureHolder icon;

        if (layer0Texture != null) {
            geometry = Optional.empty();
            animation = Optional.empty();
            icon = TextureHolder.createBuiltIn(layer0Texture.sprite());
        } else {
            // Unknown model (doesn't use layer0), so we immediately assume the geometry is custom
            // This check should probably be done differently (actually check if the model is 2D or 3D)

            geometry = Optional.of(context.geometryCache().mapGeometry(bedrockIdentifier, model, definitionTransformation, stackToRender, context));
            animation = Optional.of(AnimationMapper.mapAnimation(Rainbow.bedrockSafeIdentifier(bedrockIdentifier), "bone", model.getTopTransforms()));
            icon = geometry.get().icon();
        }

        return new BedrockGeometryContext(geometry, animation, icon, handheld);
    }
}
