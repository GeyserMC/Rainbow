package org.geysermc.rainbow.mapping.animation;

import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;
import org.geysermc.rainbow.pack.animation.BedrockAnimation;

import java.util.concurrent.CompletableFuture;

public record BedrockAnimationContext(String identifier, BedrockAnimation animation, String firstPerson, String thirdPerson, String head) implements PackSerializer.Serializable {

    @Override
    public CompletableFuture<?> save(PackSerializingContext context) {
        return context.serializer().saveJson(BedrockAnimation.CODEC, animation, context.paths().animationPath(identifier));
    }
}
