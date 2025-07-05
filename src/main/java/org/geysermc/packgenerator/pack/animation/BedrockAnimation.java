package org.geysermc.packgenerator.pack.animation;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import org.geysermc.packgenerator.CodecUtil;
import org.geysermc.packgenerator.pack.BedrockVersion;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public record BedrockAnimation(BedrockVersion formatVersion, Map<String, AnimationDefinition> definitions) {
    public static final BedrockVersion FORMAT_VERSION = BedrockVersion.of(1, 8, 0);

    public static final Codec<BedrockAnimation> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BedrockVersion.STRING_CODEC.fieldOf("format_version").forGetter(BedrockAnimation::formatVersion),
                    Codec.unboundedMap(Codec.STRING, AnimationDefinition.CODEC).fieldOf("animations").forGetter(BedrockAnimation::definitions)
            ).apply(instance, BedrockAnimation::new)
    );

    public void save(Path animationDirectory, String identifier) throws IOException {
        CodecUtil.trySaveJson(CODEC, this, animationDirectory.resolve(identifier + ".animation.json"));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static AnimationDefinition.Builder animation() {
        return new AnimationDefinition.Builder();
    }

    public static class Builder {
        private final Map<String, AnimationDefinition> animations = new HashMap<>();

        public Builder withAnimation(String identifier, AnimationDefinition definition) {
            animations.put("animation." + identifier, definition);
            return this;
        }

        public Builder withAnimation(String identifier, AnimationDefinition.Builder builder) {
            return withAnimation(identifier, builder.build());
        }

        public BedrockAnimation build() {
            return new BedrockAnimation(FORMAT_VERSION, Map.copyOf(animations));
        }
    }

    public record AnimationDefinition(LoopMode loopMode, Optional<String> startDelay, Optional<String> loopDelay, Optional<String> animationTimeUpdate,
                                      Optional<String> blendWeight, boolean overridePreviousAnimation, Map<String, SimpleAnimation> bones) {
        public static final Codec<AnimationDefinition> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        LoopMode.CODEC.optionalFieldOf("loop", LoopMode.STOP).forGetter(AnimationDefinition::loopMode),
                        Codec.STRING.optionalFieldOf("start_delay").forGetter(AnimationDefinition::startDelay),
                        Codec.STRING.optionalFieldOf("loop_delay").forGetter(AnimationDefinition::loopDelay),
                        Codec.STRING.optionalFieldOf("anim_time_update").forGetter(AnimationDefinition::animationTimeUpdate),
                        Codec.STRING.optionalFieldOf("blend_weight").forGetter(AnimationDefinition::blendWeight),
                        Codec.BOOL.optionalFieldOf("override_previous_animation", false).forGetter(AnimationDefinition::overridePreviousAnimation),
                        Codec.unboundedMap(Codec.STRING, SimpleAnimation.CODEC).optionalFieldOf("bones", Map.of()).forGetter(AnimationDefinition::bones)
                ).apply(instance, AnimationDefinition::new)
        );

        public static class Builder {
            private final Map<String, SimpleAnimation> bones = new HashMap<>();

            private LoopMode loopMode = LoopMode.STOP;
            private Optional<String> startDelay;
            private Optional<String> loopDelay;
            private Optional<String> animationTimeUpdate;
            private Optional<String> blendWeight;
            private boolean overridePreviousAnimation;

            public Builder withLoopMode(LoopMode loopMode) {
                this.loopMode = loopMode;
                return this;
            }

            public Builder withStartDelay(String startDelay) {
                this.startDelay = Optional.of(startDelay);
                return this;
            }

            public Builder withLoopDelay(String loopDelay) {
                this.loopDelay = Optional.of(loopDelay);
                return this;
            }

            public Builder withAnimationTimeUpdate(String animationTimeUpdate) {
                this.animationTimeUpdate = Optional.of(animationTimeUpdate);
                return this;
            }

            public Builder withBlendWeight(String blendWeight) {
                this.blendWeight = Optional.of(blendWeight);
                return this;
            }

            public Builder overridePreviousAnimation() {
                this.overridePreviousAnimation = true;
                return this;
            }

            public Builder withBone(String bone, SimpleAnimation animation) {
                bones.put(bone, animation);
                return this;
            }

            public Builder withBone(String bone, Vector3f position, Vector3f rotation, Vector3f scale) {
                return withBone(bone, new SimpleAnimation(position, rotation, scale));
            }

            public AnimationDefinition build() {
                return new AnimationDefinition(loopMode, startDelay, loopDelay, animationTimeUpdate, blendWeight, overridePreviousAnimation, Map.copyOf(bones));
            }
        }
    }

    public enum LoopMode {
        STOP,
        LOOP,
        HOLD_ON_LAST_FRAME;

        public static final Codec<LoopMode> CODEC = Codec.either(Codec.BOOL, Codec.STRING)
                .comapFlatMap(either -> either.map(bool -> DataResult.success(bool ? LOOP : STOP),
                        string -> {
                            if (string.equals("hold_on_last_frame")) {
                                return DataResult.success(HOLD_ON_LAST_FRAME);
                            }
                            return DataResult.error(() -> "unknown loop mode");
                        }), mode -> mode == HOLD_ON_LAST_FRAME ? Either.right("hold_on_last_frame") : Either.left(mode == LOOP));
    }

    public record SimpleAnimation(Vector3f position, Vector3f rotation, Vector3f scale) {
        public static final Codec<SimpleAnimation> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        ExtraCodecs.VECTOR3F.fieldOf("position").forGetter(SimpleAnimation::position),
                        ExtraCodecs.VECTOR3F.fieldOf("rotation").forGetter(SimpleAnimation::rotation),
                        ExtraCodecs.VECTOR3F.fieldOf("scale").forGetter(SimpleAnimation::scale)
                ).apply(instance, SimpleAnimation::new)
        );
    }
}
