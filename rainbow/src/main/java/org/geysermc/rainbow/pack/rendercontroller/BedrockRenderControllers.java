package org.geysermc.rainbow.pack.rendercontroller;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.Keyable;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.geysermc.rainbow.CodecUtil;
import org.geysermc.rainbow.codec.DispatchedMapMapCodec;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    public record RenderController(Map<RenderProperty<?>, Map<String, List<String>>> arrays, RenderPropertyMap properties, Optional<UVAnimation> uvAnimation) {
        private static final Codec<Map<RenderProperty<?>, Map<String, List<String>>>> ARRAYS_CODEC = Codec.unboundedMap(RenderProperty.CODEC,
                Codec.unboundedMap(Codec.STRING, Codec.STRING.listOf()));
        public static final Codec<RenderController> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        ARRAYS_CODEC.optionalFieldOf("arrays", Map.of()).forGetter(RenderController::arrays),
                        RenderPropertyMap.MAP_CODEC.forGetter(RenderController::properties),
                        UVAnimation.CODEC.optionalFieldOf("uv_anim").forGetter(RenderController::uvAnimation)
                ).apply(instance, RenderController::new)
        );

        public static class Builder {
            private final Map<RenderProperty<?>, Map<String, List<String>>> arrays = new HashMap<>();
            private final Map<RenderProperty<?>, ?> properties = new HashMap<>();
            private @Nullable UVAnimation uvAnimation;

            public Builder(String geometry) {
                withRenderProperty(RenderProperty.GEOMETRY, geometry);
            }

            public Builder withArray(RenderProperty<?> property, String name, List<String> values) {
                arrays.compute(property, (_, arrayMap) -> {
                    if (arrayMap == null) {
                        return Map.of(name, values);
                    }
                    Map<String, List<String>> merged = new HashMap<>(arrayMap);
                    merged.put(name, values);
                    return Map.copyOf(merged);
                });
                return this;
            }

            public <T> Builder withRenderProperty(RenderProperty<T> property, T value) {
                ((Map) properties).put(property, value);
                return this;
            }

            public Builder withUVAnimation(UVAnimation uvAnimation) {
                this.uvAnimation = uvAnimation;
                return this;
            }

            public RenderController build() {
                return new RenderController(Map.copyOf(arrays), new RenderPropertyMap(Map.copyOf(properties)), Optional.ofNullable(uvAnimation));
            }
        }
    }

    public record RenderPropertyMap(Map<RenderProperty<?>, ?> map) {
        public static final MapCodec<RenderPropertyMap> MAP_CODEC = new DispatchedMapMapCodec<>(RenderProperty.CODEC, RenderProperty::codec, RenderProperty.KEYS)
                .xmap(RenderPropertyMap::new, propertyMap -> (Map) propertyMap.map);

        public <T> @Nullable T get(RenderProperty<T> property) {
            //noinspection unchecked
            return (T) map.get(property);
        }

        public <T> T getOrDefault(RenderProperty<T> property, T defaultValue) {
            T value = get(property);
            return value == null ? defaultValue : value;
        }
    }

    public record RenderProperty<T>(String name, Codec<T> codec) {
        private static final Map<String, RenderProperty<?>> properties = new HashMap<>();
        public static final Codec<RenderProperty<?>> CODEC = Codec.STRING.comapFlatMap(RenderProperty::getByName, RenderProperty::name);
        public static final Keyable KEYS = new Keyable() {
            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return properties.keySet().stream().map(ops::createString);
            }
        };

        public static final RenderProperty<String> GEOMETRY = register("geometry", Codec.STRING);
        public static final RenderProperty<Map<String, String>> MATERIALS = register("materials", Codec.unboundedMap(Codec.STRING, Codec.STRING).listOf()
                .xmap(materials -> materials.stream()
                                .flatMap(map -> map.entrySet().stream())
                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)),
                        materials -> materials.entrySet().stream()
                                .map(Map::ofEntries)
                                .toList()));
        public static final RenderProperty<List<String>> TEXTURES = register("textures", Codec.STRING.listOf());

        public static DataResult<RenderProperty<?>> getByName(String name) {
            RenderProperty<?> property = properties.get(name);
            if (property == null) {
                return DataResult.error(() -> "Unknown render property " + name);
            }
            return DataResult.success(property);
        }

        private static <T> RenderProperty<T> register(String name, Codec<T> codec) {
            RenderProperty<T> property = new RenderProperty<>(name, codec);
            properties.put(name, property);
            return property;
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
