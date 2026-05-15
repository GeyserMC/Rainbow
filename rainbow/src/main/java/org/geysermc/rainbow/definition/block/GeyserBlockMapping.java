package org.geysermc.rainbow.definition.block;

import com.mojang.datafixers.util.Either;
import com.mojang.math.Quadrant;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import org.geysermc.rainbow.CodecUtil;
import org.geysermc.rainbow.Vectors;
import org.geysermc.rainbow.codec.OptionalMapCodec;
import org.joml.Vector3fc;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

// TODO validation: if block has only one state, then base must be present
// Doesn't include all properties, some (mainly related to creative inventory) we don't use
// TODO full block geometry
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
                        Transformation.CODEC.optionalFieldOf("transformation", Transformation.EMPTY).forGetter(BlockDefinition::transformation),
                        PlacementFilter.CODEC.optionalFieldOf("placement_filter", PlacementFilter.EMPTY).forGetter(BlockDefinition::placementFilter),
                        Identifier.CODEC.listOf().optionalFieldOf("tags", List.of()).forGetter(BlockDefinition::tags)
                ).apply(instance, BlockDefinition::new)
        );
        public static final Codec<BlockDefinition> CODEC = MAP_CODEC.codec();
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
    }

    public record MaterialInstances(Map<String, Instance> instances) {
        public static final Codec<MaterialInstances> CODEC = Codec.unboundedMap(Codec.STRING, Instance.CODEC).xmap(MaterialInstances::new, MaterialInstances::instances);
        public static final Codec<MaterialInstances> NON_EMPTY_CODEC = CODEC.validate(instances -> {
            if (instances.instances.isEmpty()) {
                return DataResult.error(() -> "MaterialInstances must have at least one instance");
            }
            return DataResult.success(instances);
        });

        public record Instance(Optional<String> texture, RenderMethod renderMethod, boolean faceDimming, boolean ambientOcclusion) {
            public static final Codec<Instance> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            Codec.STRING.optionalFieldOf("texture").forGetter(Instance::texture),
                            RenderMethod.CODEC.optionalFieldOf("render_method", RenderMethod.OPAQUE).forGetter(Instance::renderMethod),
                            Codec.BOOL.optionalFieldOf("face_dimming", true).forGetter(Instance::faceDimming),
                            Codec.BOOL.optionalFieldOf("ambient_occlusion", true).forGetter(Instance::ambientOcclusion)
                    ).apply(instance, Instance::new)
            );

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

        public record Condition(List<Direction> allowedFaces, List<Either<BlockFilter, TagFilter>> blockFilters) {
            public static final Codec<Condition> CODEC = RecordCodecBuilder.create(instance ->
                    instance.group(
                            Direction.CODEC.listOf().fieldOf("allowed_faces").forGetter(Condition::allowedFaces),
                            Codec.either(BlockFilter.CODEC, TagFilter.CODEC).listOf().fieldOf("block_filter").forGetter(Condition::blockFilters)
                    ).apply(instance, Condition::new)
            );

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
        public static final Transformation EMPTY = new Transformation(Vectors.VECTOR3F_ONE, Vectors.VECTOR3F_ZERO, ZERO_ROTATION);
    }
}
