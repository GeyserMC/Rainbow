package org.geysermc.rainbow.pack.rendercontroller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.geysermc.rainbow.CodecUtil;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public record BedrockRenderControllers(Map<String, RenderController> renderControllers) {
    public static final Codec<BedrockRenderControllers> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CodecUtil.unitVerifyCodec(Codec.STRING, "format_version", "1.8.0"),
                    Codec.unboundedMap(Codec.STRING, RenderController.CODEC).fieldOf("render_controllers").forGetter(BedrockRenderControllers::renderControllers)
            ).apply(instance, (_, renderControllers) -> new BedrockRenderControllers(renderControllers))
    );

    public static Builder builder() {
        return new Builder();
    }

    public static RenderController.Builder renderController(String geometry) {
        return new RenderController.Builder(geometry);
    }

    public static class Builder {
        private final Map<String, RenderController> renderControllers = new HashMap<>();

        public Builder withRenderController(String name, RenderController controller) {
            renderControllers.put(name, controller);
            return this;
        }

        public Builder withRenderController(String name, RenderController.Builder controller) {
            return withRenderController(name, controller.build());
        }

        public BedrockRenderControllers build() {
            return new BedrockRenderControllers(Map.copyOf(renderControllers));
        }
    }

    // TODO expand this for the full render controller format
    public record RenderController(String geometry, Map<String, String> materials, List<String> textures,
                                   Optional<UVAnimation> uvAnimation) {
        private static final Codec<Map<String, String>> MATERIALS_CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING).listOf()
                .xmap(materials -> materials.stream()
                                .flatMap(map -> map.entrySet().stream())
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                        materials -> materials.entrySet().stream()
                                .map(Map::ofEntries)
                                .toList());
        public static final Codec<RenderController> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("geometry").forGetter(RenderController::geometry),
                        MATERIALS_CODEC.fieldOf("materials").forGetter(RenderController::materials),
                        Codec.STRING.listOf().fieldOf("textures").forGetter(RenderController::textures),
                        UVAnimation.CODEC.optionalFieldOf("uv_anim").forGetter(RenderController::uvAnimation)
                ).apply(instance, RenderController::new)
        );

        public static class Builder {
            private final String geometry;
            private final Map<String, String> materials = new HashMap<>();
            private final List<String> textures = new ArrayList<>();
            private @Nullable UVAnimation uvAnimation;

            public Builder(String geometry) {
                this.geometry = geometry;
            }

            public Builder withMaterial(String option, String material) {
                materials.put(option, material);
                return this;
            }

            public Builder withTexture(String texture) {
                textures.add(texture);
                return this;
            }

            public Builder withUVAnimation(UVAnimation uvAnimation) {
                this.uvAnimation = uvAnimation;
                return this;
            }

            public RenderController build() {
                return new RenderController(geometry, Map.copyOf(materials), List.copyOf(textures), Optional.ofNullable(uvAnimation));
            }
        }
    }

    public record UVAnimation(List<String> offset, List<String> scale) {
        public static final Codec<UVAnimation> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.listOf(2, 2).fieldOf("offset").forGetter(UVAnimation::offset),
                        Codec.STRING.listOf(2, 2).fieldOf("scale").forGetter(UVAnimation::scale)
                ).apply(instance, UVAnimation::new)
        );

        public static UVAnimation create(int framesPerSecond, int frameCount) {
            return new UVAnimation(
                    List.of("0", "math.mod(math.floor(q.life_time * %s), %s) / %s".formatted(framesPerSecond, frameCount, frameCount)),
                    List.of("1", "1 / %s".formatted(frameCount))
            );
        }
    }
}
