package org.geysermc.rainbow.pack.attachable;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EquipmentSlot;
import org.geysermc.rainbow.PackConstants;
import org.geysermc.rainbow.pack.BedrockTextures;
import org.geysermc.rainbow.pack.BedrockVersion;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public record BedrockAttachable(BedrockVersion formatVersion, AttachableInfo info) {
    public static final Codec<BedrockAttachable> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    BedrockVersion.STRING_CODEC.fieldOf("format_version").forGetter(BedrockAttachable::formatVersion),
                    AttachableInfo.CODEC.fieldOf("description").fieldOf("minecraft:attachable").forGetter(BedrockAttachable::info)
            ).apply(instance, BedrockAttachable::new)
    );

    public static Builder builder(Identifier identifier) {
        return new Builder(identifier);
    }

    public static BedrockAttachable.Builder equipment(Identifier identifier, EquipmentSlot slot, String texture) {
        String script = switch (slot) {
            case HEAD -> "v.helmet_layer_visible = 0.0;";
            case CHEST -> "v.chest_layer_visible = 0.0;";
            case LEGS -> "v.leg_layer_visible = 0.0;";
            case FEET -> "v.boot_layer_visible = 0.0;";
            default -> "";
        };
        return builder(identifier)
                .withMaterial(DisplaySlot.DEFAULT, VanillaMaterials.ARMOR)
                .withMaterial(DisplaySlot.ENCHANTED, VanillaMaterials.ARMOR_ENCHANTED)
                .withTexture(DisplaySlot.DEFAULT, texture)
                .withTexture(DisplaySlot.ENCHANTED, VanillaTextures.ENCHANTED_ACTOR_GLINT)
                .withGeometry(DisplaySlot.DEFAULT, Objects.requireNonNull(VanillaGeometries.fromEquipmentSlot(slot)))
                .withScript("parent_setup", script)
                .withRenderController(VanillaRenderControllers.ARMOR);
    }

    public static BedrockAttachable.Builder geometry(Identifier identifier, String geometry) {
        return builder(identifier)
                .withMaterial(DisplaySlot.DEFAULT, VanillaMaterials.ENTITY_ALPHATEST)
                .withMaterial(DisplaySlot.ENCHANTED, VanillaMaterials.ENTITY_ALPHATEST_GLINT)
                .withTexture(DisplaySlot.ENCHANTED, VanillaTextures.ENCHANTED_ITEM_GLINT)
                .withGeometry(DisplaySlot.DEFAULT, geometry);
    }

    public static class Builder {
        private final Identifier identifier;
        private final EnumMap<DisplaySlot, String> materials = new EnumMap<>(DisplaySlot.class);
        private final Map<String, String> textures = new Object2ObjectOpenHashMap<>();
        private final EnumMap<DisplaySlot, String> geometries = new EnumMap<>(DisplaySlot.class);
        private final Map<String, String> animations = new HashMap<>();
        private final Map<String, List<Script>> scripts = new HashMap<>();
        private final List<String> renderControllers = new ArrayList<>();

        public Builder(Identifier identifier) {
            this.identifier = identifier;
        }

        public Builder withMaterial(DisplaySlot slot, String material) {
            materials.put(slot, material);
            return this;
        }

        public Builder withTexture(DisplaySlot slot, String texture) {
            return withTexture(slot.name, texture);
        }

        public Builder withTexture(String slot, String texture) {
            textures.put(slot, BedrockTextures.TEXTURES_FOLDER + texture);
            return this;
        }

        public Builder withGeometry(DisplaySlot slot, String geometry) {
            geometries.put(slot, geometry);
            return this;
        }

        public Builder withAnimation(String key, String animation) {
            animations.put(key, animation);
            return this;
        }

        public Builder withScript(String key, Script script) {
            scripts.merge(key, List.of(script), (scripts, newScript) -> Stream.concat(scripts.stream(), newScript.stream()).toList());
            return this;
        }

        public Builder withScript(String key, String script, String condition) {
            return withScript(key, new Script(script, Optional.of(condition)));
        }

        public Builder withScript(String key, String script) {
            return withScript(key, new Script(script, Optional.empty()));
        }

        public Builder withRenderController(String controller) {
            renderControllers.add(controller);
            return this;
        }

        public BedrockAttachable build() {
            return new BedrockAttachable(PackConstants.ENGINE_VERSION,
                    new AttachableInfo(identifier, verifyDefault(materials), Map.copyOf(textures), verifyDefault(geometries), Map.copyOf(animations),
                            new Scripts(Map.copyOf(scripts)), List.copyOf(renderControllers)));
        }

        private static DisplayMap verifyDefault(EnumMap<DisplaySlot, String> map) {
            if (!map.containsKey(DisplaySlot.DEFAULT)) {
                throw new IllegalStateException("DisplayMap must have a default key");
            }
            return new DisplayMap(Map.copyOf(map));
        }
    }

    public record AttachableInfo(Identifier identifier, DisplayMap materials, Map<String, String> textures,
                                 DisplayMap geometry, Map<String, String> animations, Scripts scripts,
                                 List<String> renderControllers) {
        private static final Codec<Map<String, String>> STRING_MAP_CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING);
        public static final Codec<AttachableInfo> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Identifier.CODEC.fieldOf("identifier").forGetter(AttachableInfo::identifier),
                        DisplayMap.CODEC.fieldOf("materials").forGetter(AttachableInfo::materials),
                        Codec.unboundedMap(Codec.STRING, Codec.STRING).fieldOf("textures").forGetter(AttachableInfo::textures),
                        DisplayMap.CODEC.fieldOf("geometry").forGetter(AttachableInfo::geometry),
                        STRING_MAP_CODEC.optionalFieldOf("animations", Map.of()).forGetter(AttachableInfo::animations),
                        Scripts.CODEC.optionalFieldOf("scripts", Scripts.EMPTY).forGetter(AttachableInfo::scripts),
                        Codec.STRING.listOf().optionalFieldOf("render_controllers", List.of()).forGetter(AttachableInfo::renderControllers)
                ).apply(instance, AttachableInfo::new)
        );
    }

    public record DisplayMap(Map<DisplaySlot, String> map) {
        public static final Codec<DisplayMap> CODEC = Codec.unboundedMap(DisplaySlot.CODEC, Codec.STRING)
                .xmap(map -> map.isEmpty() ? new DisplayMap(new EnumMap<>(DisplaySlot.class)) : new DisplayMap(new EnumMap<>(map)), DisplayMap::map);
    }

    public enum DisplaySlot implements StringRepresentable {
        DEFAULT("default"),
        ENCHANTED("enchanted");

        public static final Codec<DisplaySlot> CODEC = StringRepresentable.fromEnum(DisplaySlot::values);

        private final String name;

        DisplaySlot(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }

    public record Scripts(Map<String, List<Script>> scripts) {
        public static final Codec<Scripts> CODEC = Codec.unboundedMap(Codec.STRING, ExtraCodecs.compactListCodec(Script.CODEC)).xmap(Scripts::new, Scripts::scripts);
        public static final Scripts EMPTY = new Scripts(Map.of());
    }

    public record Script(String script, Optional<String> condition) {
        private static final Codec<Script> SCRIPT_WITH_CONDITION_CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING).flatXmap(
                scriptMap -> {
                    if (scriptMap.size() != 1) {
                        return DataResult.error(() -> "Script with condition must have exactly one key-value pair");
                    }
                    String script = scriptMap.keySet().iterator().next();
                    return DataResult.success(new Script(script, Optional.of(scriptMap.get(script))));
                },
                script -> script.condition.map(condition -> DataResult.success(Map.of(script.script, condition)))
                        .orElse(DataResult.error(() -> "Script must have a condition"))
        );
        public static final Codec<Script> CODEC = SCRIPT_WITH_CONDITION_CODEC.mapResult(new Codec.ResultFunction<>() {
            @Override
            public <T> DataResult<Pair<Script, T>> apply(DynamicOps<T> ops, T input, DataResult<Pair<Script, T>> decoded) {
                if (decoded.isError()) {
                    return Codec.STRING.map(script -> new Script(script, Optional.empty())).decode(ops, input);
                }
                return decoded;
            }

            @Override
            public <T> DataResult<T> coApply(DynamicOps<T> ops, Script input, DataResult<T> encoded) {
                if (encoded.isError()) {
                    return Codec.STRING.encodeStart(ops, input.script);
                }
                return encoded;
            }
        });
    }
}
