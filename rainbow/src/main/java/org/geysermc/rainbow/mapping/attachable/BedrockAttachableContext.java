package org.geysermc.rainbow.mapping.attachable;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStackTemplate;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;
import org.geysermc.rainbow.mapping.geometry.BedrockGeometryContext;
import org.geysermc.rainbow.mapping.geometry.MappedGeometry;
import org.geysermc.rainbow.mapping.texture.ModelTextures;
import org.geysermc.rainbow.pack.PackPaths;
import org.geysermc.rainbow.pack.attachable.BedrockAttachable;
import org.geysermc.rainbow.pack.attachable.VanillaGeometries;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record BedrockAttachableContext(Optional<BedrockAttachable> attachable) implements PackSerializer.Serializable {
    public static final BedrockAttachableContext EMPTY = new BedrockAttachableContext(Optional.empty());

    @Override
    public CompletableFuture<?> save(PackSerializingContext context) {
        return PackSerializer.Serializable.wrapOptionalCodec(BedrockAttachable.CODEC, attachable, PackPaths::attachablePath).save(context);
    }

    public static BedrockAttachableContext create(Identifier identifier, ItemStackTemplate stack, BedrockGeometryContext geometryContext, ModelTextures textures) {
        // Prefer animation or geometry attachable over equippable
        if (textures.requiresAttachable() || geometryContext.geometry().isPresent()) {
            BedrockAttachable.Builder attachable = BedrockAttachable.geometry(identifier, geometryContext.geometry().map(MappedGeometry::identifier).orElse(VanillaGeometries.ITEM_SPRITE));
            textures.applyToAttachable(attachable);
            geometryContext.animation().ifPresent(animation -> {
                attachable.withAnimation("first_person", animation.firstPerson());
                attachable.withAnimation("third_person", animation.thirdPerson());
                attachable.withAnimation("head", animation.head());
                attachable.withScript("animate", "first_person", "context.is_first_person == 1.0");
                attachable.withScript("animate", "third_person", "context.is_first_person == 0.0 && (context.item_slot == 'main_hand' || context.item_slot == 'off_hand')");
                attachable.withScript("animate", "head", "context.is_first_person == 0.0 && context.item_slot == 'head'");
            });
            return new BedrockAttachableContext(Optional.of(attachable.build()));
        } else if (stack.components().split().added().has(DataComponents.EQUIPPABLE)) {
            // TODO also prefer this since bedrock only displays equippable when equippable
        }
        return EMPTY;
    }
}
