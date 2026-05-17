package org.geysermc.rainbow.mapping;

import com.mojang.math.Transformation;
import net.minecraft.client.data.models.blockstates.PropertyValueList;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.block.dispatch.WeightedVariants;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.definition.block.GeyserBlockMapping;
import org.geysermc.rainbow.mapping.geometry.GeometryMapper;
import org.geysermc.rainbow.mapping.geometry.MappedGeometry;
import org.geysermc.rainbow.mapping.texture.BlockModelTextures;
import org.geysermc.rainbow.mapping.texture.ModelTextures;
import org.geysermc.rainbow.mixin.BlockStateModelSimpleCachedUnbakedRootAccessor;
import org.geysermc.rainbow.pack.BedrockBlock;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class BedrockBlockMapper {

    public static void tryMapBlock(Block block, ProblemReporter reporter, PackContext context) {
        GeyserBlockMapping.Builder mapping = GeyserBlockMapping.builder(getNameForBlock(block));

        StateDefinition<Block, BlockState> stateDefinition = block.getStateDefinition();
        if (stateDefinition.isSingletonState()) {
            tryMapBlockState(stateDefinition.any(), blockChild(reporter, block), context, false,
                    definition -> context.mappings().blocks().map(block.builtInRegistryHolder(), mapping.withBase(definition)));
        }

        mapping.onlyOverrideStates();
        stateDefinition.getPossibleStates().forEach(state -> {
            String key = blockStatePropertiesToString(state);
            tryMapBlockState(state, stateChild(reporter, state), context, false, definition -> mapping.withStateOverride(key, definition));
        });

        if (mapping.hasStateOverrides()) {
            context.mappings().blocks().map(block.builtInRegistryHolder(), mapping);
        }
    }

    public static void tryMapBlockState(BlockState state, ProblemReporter reporter, PackContext context) {
        GeyserBlockMapping.Builder mapping = GeyserBlockMapping.builder(getNameForBlock(state.getBlock()));
        mapping.onlyOverrideStates();
        tryMapBlockState(state, stateChild(reporter, state), context, true, definition -> {
            mapping.withStateOverride(blockStatePropertiesToString(state), definition);
            context.mappings().blocks().map(state.getBlock().builtInRegistryHolder(), mapping);
        });
    }

    private static ProblemReporter blockChild(ProblemReporter reporter, Block block) {
        Identifier identifier = BuiltInRegistries.BLOCK.getKey(block);
        return reporter.forChild(() -> "block " + identifier);
    }

    private static ProblemReporter stateChild(ProblemReporter reporter, BlockState state) {
        return blockChild(reporter, state.getBlock()).forChild(() -> "[" + blockStatePropertiesToString(state) + "]");
    }

    private static String getNameForBlock(Block block) {
        return Rainbow.bedrockSafeIdentifier(BuiltInRegistries.BLOCK.getKey(block));
    }

    private static void tryMapBlockState(BlockState state, ProblemReporter reporter, PackContext context, boolean includeVanilla, Consumer<GeyserBlockMapping.BlockDefinition.Builder> definitionConsumer) {
        context.assetResolver().getBlockStateModel(state).ifPresentOrElse(root -> {
            if (root instanceof BlockStateModel.SimpleCachedUnbakedRoot simpleRoot) {
                BlockStateModel.Unbaked contents = ((BlockStateModelSimpleCachedUnbakedRootAccessor) simpleRoot).getContents();
                // TODO better weight handling
                Variant stateVariant = contents instanceof SingleVariant.Unbaked(Variant variant) ? variant
                        : ((SingleVariant.Unbaked) ((WeightedVariants.Unbaked) contents).entries().unwrap().getFirst().value()).variant();
                Identifier stateModel = stateVariant.modelLocation();

                // If not including vanilla, only map variants that don't use a vanilla block model
                if (includeVanilla || !stateModel.getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
                    context.assetResolver().getResolvedModel(stateModel)
                            .ifPresentOrElse(model -> {
                                if (ModelTextures.hasNoMaterials(model.getTopTextureSlots())) {
                                    reporter.report(() -> "unable to map block model " + stateModel + " as it has no textures");
                                } else {
                                    if (context.reportSuccesses()) {
                                        reporter.report(() -> "creating mapping for block model " + stateModel);
                                    }
                                    definitionConsumer.accept(mapBlockState(state, model, stateVariant.modelState(), context));
                                }
                            }, () -> reporter.report(() -> "missing block model: " + stateModel));
                }
            } else if (includeVanilla) {
                // Only report this one if explicitly mapping vanilla
                reporter.report(() -> "multi-part block state definitions are unsupported");
            }
        }, () -> reporter.report(() -> "missing block state definition"));
    }

    private static GeyserBlockMapping.BlockDefinition.Builder mapBlockState(BlockState state, ResolvedModel model, Variant.SimpleModelState modelState, PackContext context) {
        GeyserBlockMapping.BlockDefinition.Builder builder = GeyserBlockMapping.definition();

        UnbakedGeometry geometry = model.getTopGeometry();
        boolean ambientOcclusion = model.getTopAmbientOcclusion();

        BlockModelTextures textures = context.blockTextureCache().load(model, () -> BlockModelTextures.create(model.getTopTextureSlots(), context.assetResolver()));

        Optional<MappedGeometry> mappedGeometry = Optional.empty();
        if (looksLikeFullBlockModel(model.getTopGeometry()) && textures.isSingleMaterial()) {
            builder.withFullBlockGeometry(GeyserBlockMapping.materials().withInstance("*", mapMaterial(textures.getSingleMaterial().orElseThrow().material(), ambientOcclusion)));
        } else if (looksLikeCrossBlockModel(model.getTopGeometry()) && textures.isSingleMaterial()) {
            builder.withCrossGeometry(GeyserBlockMapping.materials().withInstance("*", mapMaterial(textures.getSingleMaterial().orElseThrow().material(), ambientOcclusion)));
        } else {
            Identifier modelIdentifier = Rainbow.getModelIdentifier(model);
            mappedGeometry = Optional.of(context.geometryCache().mapGeometry(modelIdentifier, model, Transformation.IDENTITY, textures));
            GeyserBlockMapping.MaterialInstances materials = textures.getSingleMaterial()
                    .map(material -> GeyserBlockMapping.materials()
                            .withInstance("*", mapMaterial(material.material(), ambientOcclusion))
                            .build())
                    .orElseGet(() -> mapMaterials(geometry, model.getTopTextureSlots(), ambientOcclusion));
            builder.withGeometry(mappedGeometry.get().identifier(), materials);
        }

        Block base = state.getBlock();
        // Java and bedrock do friction differently.
        // As friction goes up on bedrock, friction increases and the surface is less slippery
        // On Java, this is the opposite: as friction increases, a surface is more slippery
        // This is an approximate conversion
        builder.withFriction(-base.getFriction() + 1.0F);
        builder.withLightEmission(state.getLightEmission());
        builder.withLightDampening(state.getLightDampening());
        builder.withTransformation(modelState.x(), modelState.y(), modelState.z());
        context.assetConsumer().acceptBlock(new BedrockBlock(textures, mappedGeometry));
        return builder;
    }

    private static boolean looksLikeFullBlockModel(UnbakedGeometry geometry) {
        if (geometry instanceof UnbakedCuboidGeometry(List<CuboidModelElement> elements) && elements.size() == 1) {
            CuboidModelElement element = elements.getFirst();
            return element.from().equals(0.0F, 0.0F, 0.0F) && element.to().equals(16.0F, 16.0F, 16.0F);
        }
        return false;
    }

    private static boolean looksLikeCrossBlockModel(UnbakedGeometry geometry) {
        if (geometry instanceof UnbakedCuboidGeometry(List<CuboidModelElement> elements) && elements.size() == 2) {
            CuboidModelElement first = elements.getFirst();
            CuboidModelElement second = elements.getLast();
            return first.from().equals(0.8F, 0.0F, 8.0F) && first.to().equals(15.2F, 16.0F, 8.0F)
                    && second.from().equals(8.0F, 0.0F, 0.8F) && second.to().equals(8.0F, 16.0F, 15.2F);
        }
        return false;
    }

    private static GeyserBlockMapping.MaterialInstances mapMaterials(UnbakedGeometry geometry, TextureSlots textures, boolean ambientOcclusion) {
        if (geometry instanceof UnbakedCuboidGeometry(List<CuboidModelElement> elements)) {
            GeyserBlockMapping.MaterialInstances.Builder builder = GeyserBlockMapping.materials();
            for (int i = 0; i < elements.size(); i++) {
                CuboidModelElement element = elements.get(i);
                int elementIndex = i;
                element.faces().forEach((direction, face) -> {
                    Material material = textures.getMaterial(face.texture());
                    if (material != null) {
                        builder.withInstance(GeometryMapper.getMeaningfulMaterialInstanceName(direction, face.texture(), elementIndex), mapMaterial(material, ambientOcclusion));
                    }
                });
            }
            return builder.build();
        }
        return GeyserBlockMapping.MaterialInstances.EMPTY;
    }

    private static GeyserBlockMapping.MaterialInstances.Instance mapMaterial(Material material, boolean ambientOcclusion) {
        // TODO OPAQUE
        return new GeyserBlockMapping.MaterialInstances.Instance(Rainbow.bedrockSafeIdentifier(material.sprite()), GeyserBlockMapping.MaterialInstances.Instance.RenderMethod.OPAQUE,
                true, ambientOcclusion);
    }

    private static String blockStatePropertiesToString(BlockState state) {
        // generics... you get what I mean
        return new PropertyValueList((List) state.getProperties().stream().map(property -> property.value(state)).toList()).getKey();
    }
}
