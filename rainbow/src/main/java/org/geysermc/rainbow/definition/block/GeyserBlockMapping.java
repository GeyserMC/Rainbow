/*
 * Copyright (c) 2026 GeyserMC. https://geysermc.org
 *
 * This file is part of Rainbow.
 *
 * Rainbow is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or (at your option) any later version.
 *
 * Rainbow is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * Rainbow. If not, see <https://www.gnu.org/licenses/>.
 */

package org.geysermc.rainbow.definition.block;

import com.mojang.datafixers.util.Either;
import com.mojang.math.Quadrant;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import org.geysermc.rainbow.CodecUtil;
import org.geysermc.rainbow.Vectors;
import org.geysermc.rainbow.codec.OptionalMapCodec;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

// Doesn't include all properties, some (mainly related to creative inventory) we don't use
public record GeyserBlockMapping(String name, Optional<BlockDefinition> base, boolean includeInCreativeInventory,
                                 boolean onlyOverrideStates, Map<String, BlockDefinition> stateOverrides) {
    public static final Codec<GeyserBlockMapping> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    ExtraCodecs.NON_EMPTY_STRING.fieldOf("name").forGetter(GeyserBlockMapping::name),
                    OptionalMapCodec.of(BlockDefinition.MAP_CODEC).forGetter(GeyserBlockMapping::base),
                    Codec.BOOL.optionalFieldOf("included_in_creative_inventory", false).forGetter(GeyserBlockMapping::includeInCreativeInventory),
                    Codec.BOOL.optionalFieldOf("only_override_states", false).forGetter(GeyserBlockMapping::onlyOverrideStates),
                    Codec.unboundedMap(Codec.STRING, BlockDefinition.CODEC).fieldOf("state_overrides").forGetter(GeyserBlockMapping::stateOverrides)
            ).apply(instance, GeyserBlockMapping::new)
    );

    public GeyserBlockMapping mergeStateOverrides(GeyserBlockMapping other) {
        if (other.stateOverrides.isEmpty()) {
            return this;
        } else if (stateOverrides.isEmpty()) {
            return new GeyserBlockMapping(name, base, includeInCreativeInventory, onlyOverrideStates, other.stateOverrides);
        }
        Map<String, BlockDefinition> newOverrides = new Object2ObjectOpenHashMap<>(stateOverrides);
        newOverrides.putAll(other.stateOverrides);
        return new GeyserBlockMapping(name, base, includeInCreativeInventory, onlyOverrideStates, Collections.unmodifiableMap(newOverrides));
    }

    public GeyserBlockMapping withStateOverride(String state, BlockDefinition.Builder override) {
        return withStateOverride(state, override.build());
    }

    public GeyserBlockMapping withStateOverride(String state, BlockDefinition override) {
        Map<String, BlockDefinition> newOverrides = new Object2ObjectOpenHashMap<>(stateOverrides);
        newOverrides.put(state, override);
        return new GeyserBlockMapping(name, base, includeInCreativeInventory, onlyOverrideStates, Collections.unmodifiableMap(newOverrides));
    }

    public static Builder builder(String name) {
        return new Builder(name);
    }

    public static BlockDefinition.Builder definition() {
        return new BlockDefinition.Builder();
    }

    public static Geometry.Builder geometry(String identifier) {
        return new Geometry.Builder(identifier);
    }

    public static MaterialInstances.Builder materials() {
        return new MaterialInstances.Builder();
    }

    public static PlacementFilter.Builder placementFilter() {
        return new PlacementFilter.Builder();
    }

    public static PlacementFilter.Condition.Builder placementCondition() {
        return new PlacementFilter.Condition.Builder();
    }

    public static class Builder {
        private final String name;
        private @Nullable BlockDefinition base;
        private boolean includeInCreativeInventory = false;
        private boolean onlyOverrideStates = false;
        private final Map<String, BlockDefinition> stateOverrides = new Object2ObjectOpenHashMap<>();

        public Builder(String name) {
            this.name = name;
        }

        public Builder withBase(BlockDefinition.Builder base) {
            return withBase(base.build());
        }

        public Builder withBase(BlockDefinition base) {
            this.base = base;
            return this;
        }

        public Builder includeInCreativeInventory() {
            this.includeInCreativeInventory = true;
            return this;
        }

        public Builder onlyOverrideStates() {
            this.onlyOverrideStates = true;
            return this;
        }

        public Builder withStateOverride(String blockState, BlockDefinition.Builder override) {
            return withStateOverride(blockState, override.build());
        }

        public Builder withStateOverride(String blockState, BlockDefinition override) {
            stateOverrides.put(blockState, override);
            return this;
        }

        public boolean hasStateOverrides() {
            return !stateOverrides.isEmpty();
        }

        public GeyserBlockMapping build() {
            if (onlyOverrideStates && stateOverrides.isEmpty()) {
                throw new IllegalStateException("onlyOverrideStates is set to true, but there are no state overrides");
            }
            return new GeyserBlockMapping(name, Optional.ofNullable(base), includeInCreativeInventory, onlyOverrideStates, Map.copyOf(stateOverrides));
        }
    }

    // Note that destructible by mining is documented as int but read as float
    public record BlockDefinition(Optional<Box> selectionBox, List<Box> collisionBoxes, Optional<Float> destructibleByMining,
                                  Optional<GeometryWithMaterials> geometry, Optional<Float> friction, OptionalInt lightEmission,
                                  OptionalInt lightDampening, boolean placeAir, Transformation transformation,
                                  PlacementFilter placementFilter, List<Identifier> tags) {
        public static final MapCodec<BlockDefinition> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Box.CODEC.optionalFieldOf("selection_box").forGetter(BlockDefinition::selectionBox),
                        ExtraCodecs.compactListCodec(Box.CODEC).optionalFieldOf("collision_box", List.of()).forGetter(BlockDefinition::collisionBoxes),
                        Codec.FLOAT.optionalFieldOf("destructible_by_mining").forGetter(BlockDefinition::destructibleByMining),
                        OptionalMapCodec.of(GeometryWithMaterials.MAP_CODEC).forGetter(BlockDefinition::geometry),
                        Codec.FLOAT.optionalFieldOf("friction").forGetter(BlockDefinition::friction),
                        CodecUtil.optionalInt("light_emission").forGetter(BlockDefinition::lightEmission),
                        CodecUtil.optionalInt("light_dampening").forGetter(BlockDefinition::lightDampening),
                        Codec.BOOL.optionalFieldOf("place_air", true).forGetter(BlockDefinition::placeAir),
                        Transformation.CODEC.optionalFieldOf("transformation", Transformation.IDENTITY).forGetter(BlockDefinition::transformation),
                        PlacementFilter.CODEC.optionalFieldOf("placement_filter", PlacementFilter.EMPTY).forGetter(BlockDefinition::placementFilter),
                        Identifier.CODEC.listOf().optionalFieldOf("tags", List.of()).forGetter(BlockDefinition::tags)
                ).apply(instance, BlockDefinition::new)
        );
        public static final Codec<BlockDefinition> CODEC = MAP_CODEC.codec();

        public static class Builder {
            private @Nullable Box selectionBox = null;
            private final List<Box> collisionBoxes = new ArrayList<>();
            private float destructibleByMining = Float.MAX_VALUE;
            private @Nullable GeometryWithMaterials geometry = null;
            private Optional<Float> friction = Optional.empty();
            private OptionalInt lightEmission = OptionalInt.empty();
            private OptionalInt lightDampening = OptionalInt.empty();
            private boolean placeAir = true;
            private Transformation transformation = Transformation.IDENTITY;
            private PlacementFilter placementFilter = PlacementFilter.EMPTY;
            private final List<Identifier> tags = new ArrayList<>();

            public Builder withSelectionBox(Box selectionBox) {
                this.selectionBox = selectionBox;
                return this;
            }

            public Builder withCollisionBox(Box collisionBox) {
                collisionBoxes.add(collisionBox);
                return this;
            }

            public Builder withHardness(float hardness) {
                destructibleByMining = hardness;
                return this;
            }

            public Builder withFullBlockGeometry(MaterialInstances.Builder materials) {
                return withGeometry("minecraft:geometry.full_block", materials);
            }

            public Builder withCrossGeometry(MaterialInstances.Builder materials) {
                return withGeometry("minecraft:geometry.cross", materials);
            }

            public Builder withGeometry(String geometry, MaterialInstances.Builder materials) {
                return withGeometry(geometry, materials.build());
            }

            public Builder withGeometry(String geometry, MaterialInstances materials) {
                return withGeometry(new Geometry(geometry), materials);
            }

            public Builder withGeometry(Geometry.Builder geometry, MaterialInstances.Builder materials) {
                return withGeometry(geometry.build(), materials.build());
            }

            public Builder withGeometry(Geometry geometry, MaterialInstances materials) {
                return withGeometry(new GeometryWithMaterials(geometry, materials));
            }

            public Builder withGeometry(GeometryWithMaterials geometry) {
                this.geometry = geometry;
                return this;
            }

            public Builder withFriction(float friction) {
                this.friction = Optional.of(friction);
                return this;
            }

            public Builder withLightEmission(int lightEmission) {
                this.lightEmission = OptionalInt.of(lightEmission);
                return this;
            }

            public Builder withLightDampening(int lightDampening) {
                this.lightDampening = OptionalInt.of(lightDampening);
                return this;
            }

            public Builder noPlaceAir() {
                this.placeAir = false;
                return this;
            }

            public Builder withTransformation(Vector3fc scale, Vector3fc translation) {
                return withTransformation(new Transformation(scale, translation));
            }

            public Builder withTransformation(Quadrant rotationX, Quadrant rotationY, Quadrant rotationZ) {
                return withTransformation(new Transformation(Vectors.VECTOR3F_ONE, Vectors.VECTOR3F_ZERO, rotationX, rotationY, rotationZ));
            }

            public Builder withTransformation(Vector3fc scale, Vector3fc translation, Quadrant rotationX, Quadrant rotationY, Quadrant rotationZ) {
                return withTransformation(new Transformation(scale, translation, rotationX, rotationY, rotationZ));
            }

            public Builder withTransformation(Transformation transformation) {
                this.transformation = transformation;
                return this;
            }

            public Builder withPlacementFilter(PlacementFilter.Builder placementFilter) {
                return withPlacementFilter(placementFilter.build());
            }

            public Builder withPlacementFilter(PlacementFilter placementFilter) {
                this.placementFilter = placementFilter;
                return this;
            }

            public Builder withTag(Identifier tag) {
                tags.add(tag);
                return this;
            }

            public BlockDefinition build() {
                return new BlockDefinition(Optional.ofNullable(selectionBox), List.copyOf(collisionBoxes),
                        destructibleByMining == Float.MAX_VALUE ? Optional.empty() : Optional.of(destructibleByMining),
                        Optional.ofNullable(geometry), friction, lightEmission,
                        lightDampening, placeAir, transformation,
                        placementFilter, List.copyOf(tags));
            }
        }
    }

    public record Box(Vector3fc origin, Vector3fc size) {
        public static final Codec<Box> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        ExtraCodecs.VECTOR3F.fieldOf("origin").forGetter(Box::origin),
                        ExtraCodecs.VECTOR3F.fieldOf("size").forGetter(Box::size)
                ).apply(instance, Box::new)
        );
    }

    // https://learn.microsoft.com/en-us/minecraft/creator/reference/content/blockreference/examples/blockcomponents/minecraftblock_material_instances?view=minecraft-bedrock-stable#render-method-choices
    // "From 1.21.80 onward, when using a minecraft:geometry component or minecraft:material_instances component, you must include both."
    public record GeometryWithMaterials(Geometry geometry, MaterialInstances materialInstances) {
        public static final MapCodec<GeometryWithMaterials> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Geometry.CODEC.fieldOf("geometry").forGetter(GeometryWithMaterials::geometry),
                        MaterialInstances.NON_EMPTY_CODEC.fieldOf("material_instances").forGetter(GeometryWithMaterials::materialInstances)
                ).apply(instance, GeometryWithMaterials::new)
        );
    }

    public record Geometry(String identifier, Map<String, String> visibilityFilter) {
        public static final Codec<Geometry> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        Codec.STRING.fieldOf("identifier").forGetter(Geometry::identifier),
                        Codec.unboundedMap(Codec.STRING, Codec.STRING).optionalFieldOf("bone_visibility", Map.of()).forGetter(Geometry::visibilityFilter)
                ).apply(instance, Geometry::new)
        );

        public Geometry(String identifier) {
            this(identifier, Map.of());
        }

        public static class Builder {
            private final String identifier;
            private final Map<String, String> visibilityFilter = new Object2ObjectOpenHashMap<>();

            public Builder(String identifier) {
                this.identifier = identifier;
            }

            public Builder withVisibilityFilter(String bone, boolean visible) {
                return withVisibilityFilter(bone, visible ? "1" : "0");
            }

            public Builder withVisibilityFilter(String bone, String molang) {
                visibilityFilter.put(bone, molang);
                return this;
            }

            public Geometry build() {
                return new Geometry(identifier, Map.copyOf(visibilityFilter));
            }
        }
    }

    public record MaterialInstances(Map<String, Instance> instances) {
        public static final Codec<MaterialInstances> CODEC = Codec.unboundedMap(Codec.STRING, Instance.CODEC).xmap(MaterialInstances::new, MaterialInstances::instances);
        public static final Codec<MaterialInstances> NON_EMPTY_CODEC = CODEC.validate(instances -> {
            if (instances.instances.isEmpty()) {
                return DataResult.error(() -> "MaterialInstances must have at least one instance");
            }
            return DataResult.success(instances);
        });
        public static MaterialInstances EMPTY = new MaterialInstances(Map.of());

        public static class Builder {
            private final Map<String, Instance> instances = new Object2ObjectOpenHashMap<>();

            public Builder withInstance(String key, String texture) {
                return withInstance(key, texture, Instance.RenderMethod.OPAQUE, true, true);
            }

            public Builder withInstance(String key, String texture, Instance.RenderMethod renderMethod, boolean faceDimming, boolean ambientOcclusion) {
                return withInstance(key, new Instance(texture, renderMethod, faceDimming, ambientOcclusion));
            }

            public Builder withInstance(String key, Instance instance) {
                instances.put(key, instance);
                return this;
            }

            public MaterialInstances build() {
                if (instances.isEmpty()) {
                    throw new IllegalStateException("Must have at least one material instance");
                }
                return new MaterialInstances(Map.copyOf(instances));
            }
        }

        public record Instance(Optional<String> texture, RenderMethod renderMethod, boolean faceDimming, boolean ambientOcclusion) {
            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codec.STRING.optionalFieldOf("texture").forGetter(Instance::texture),
                            RenderMethod.CODEC.optionalFieldOf("render_method", RenderMethod.OPAQUE).forGetter(Instance::renderMethod),
                            Codec.BOOL.optionalFieldOf("face_dimming", true).forGetter(Instance::faceDimming),
                            Codec.BOOL.optionalFieldOf("ambient_occlusion", true).forGetter(Instance::ambientOcclusion)
                    ).apply(instance, Instance::new)
            );

            public Instance(String texture, RenderMethod renderMethod, boolean faceDimming, boolean ambientOcclusion) {
                this(Optional.of(texture), renderMethod, faceDimming, ambientOcclusion);
            }

            public enum RenderMethod implements StringRepresentable {
                OPAQUE("opaque"),
                DOUBLE_SIDED("double_sided"),
                BLEND("blend"),
                ALPHA_TEST("alpha_test"),
                ALPHA_TEST_SINGLE_SIDED("alpha_test_single_sided"),
                BLEND_TO_OPAQUE("blend_to_opaque"),
                ALPHA_TEST_TO_OPAQUE("alpha_test_to_opaque"),
                ALPHA_TEST_SINGLE_SIDED_TO_OPAQUE("alpha_test_single_sided_to_opaque");

                public static final Codec<RenderMethod> CODEC = StringRepresentable.fromEnum(RenderMethod::values);

                private final String name;

                RenderMethod(String name) {
                    this.name = name;
                }

                @Override
                public String getSerializedName() {
                    return name;
                }
            }
        }
    }

    public record PlacementFilter(List<Condition> conditions) {
        public static final Codec<PlacementFilter> CODEC = Condition.CODEC.listOf().fieldOf("conditions").codec().xmap(PlacementFilter::new, PlacementFilter::conditions);
        public static final PlacementFilter EMPTY = new PlacementFilter(List.of());

        public static class Builder {
            private final List<Condition> conditions = new ArrayList<>();

            public Builder withCondition(Condition.Builder condition) {
                return withCondition(condition.build());
            }

            public Builder withCondition(Condition condition) {
                conditions.add(condition);
                return this;
            }

            public PlacementFilter build() {
                if (conditions.isEmpty()) {
                    return EMPTY;
                }
                return new PlacementFilter(List.copyOf(conditions));
            }
        }

        public record Condition(List<Direction> allowedFaces, List<Either<BlockFilter, TagFilter>> blockFilters) {
            public static final Codec<Condition> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            Direction.CODEC.listOf().fieldOf("allowed_faces").forGetter(Condition::allowedFaces),
                            Codec.either(BlockFilter.CODEC, TagFilter.CODEC).listOf().fieldOf("block_filter").forGetter(Condition::blockFilters)
                    ).apply(instance, Condition::new)
            );

            public static class Builder {
                private final Set<Direction> allowedFaces = new HashSet<>();
                private final List<Either<BlockFilter, TagFilter>> blockFilters = new ArrayList<>();

                public Builder allowFace(Direction face) {
                    allowedFaces.add(face);
                    return this;
                }

                public Builder withBlockFilter(Identifier block) {
                    this.blockFilters.add(Either.left(new BlockFilter(block)));
                    return this;
                }

                public Builder withTagFilter(Identifier tag) {
                    this.blockFilters.add(Either.right(new TagFilter(tag)));
                    return this;
                }

                public Condition build() {
                    return new Condition(List.copyOf(allowedFaces), List.copyOf(blockFilters));
                }
            }

            public record BlockFilter(Identifier block) {
                public static final Codec<BlockFilter> CODEC = Identifier.CODEC.xmap(BlockFilter::new, BlockFilter::block);
            }

            public record TagFilter(Identifier tag) {
                public static final Codec<TagFilter> CODEC = Identifier.CODEC.fieldOf("tags").codec().xmap(TagFilter::new, TagFilter::tag);
            }
        }
    }

    public record Transformation(Vector3fc scale, Vector3fc translation, List<Quadrant> rotation) {
        private static final List<Quadrant> ZERO_ROTATION = List.of(Quadrant.R0, Quadrant.R0, Quadrant.R0);
        public static final Codec<Transformation> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        ExtraCodecs.VECTOR3F.optionalFieldOf("scale", Vectors.VECTOR3F_ONE).forGetter(Transformation::scale),
                        ExtraCodecs.VECTOR3F.optionalFieldOf("translation", Vectors.VECTOR3F_ZERO).forGetter(Transformation::translation),
                        Codec.list(Quadrant.CODEC, 3, 3).optionalFieldOf("rotation", ZERO_ROTATION).forGetter(Transformation::rotation)
                ).apply(instance, Transformation::new)
        );
        public static final Transformation IDENTITY = new Transformation(Vectors.VECTOR3F_ONE, Vectors.VECTOR3F_ZERO, ZERO_ROTATION);

        public Transformation {
            if (rotation.size() != 3) {
                throw new IllegalArgumentException("Rotation must have exactly 3 elements");
            }
        }

        public Transformation(Vector3fc scale, Vector3fc translation, Quadrant rotationX, Quadrant rotationY, Quadrant rotationZ) {
            this(scale, translation, List.of(rotationX, rotationY, rotationZ));
        }

        public Transformation(Vector3fc scale, Vector3fc translation) {
            this(scale, translation, ZERO_ROTATION);
        }
    }
}
