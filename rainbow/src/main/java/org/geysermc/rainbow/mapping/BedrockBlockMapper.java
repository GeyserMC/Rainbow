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
        tryMapBlockState(state, reporter, context, true, definition -> {
            mapping.withStateOverride(blockStatePropertiesToString(state), definition);
            context.mappings().blocks().map(state.getBlock().builtInRegistryHolder(), mapping);
        });
    }

    private static ProblemReporter blockChild(ProblemReporter reporter, Block block) {
        Identifier identifier = BuiltInRegistries.BLOCK.getKey(block);
        return reporter.forChild(() -> "block " + identifier + " ");
    }

    private static ProblemReporter stateChild(ProblemReporter reporter, BlockState state) {
        return blockChild(reporter, state.getBlock()).forChild(() -> "state " + blockStatePropertiesToString(state) + " ");
    }

    private static String getNameForBlock(Block block) {
        return Rainbow.getModdedIdentifier(BuiltInRegistries.BLOCK.getKey(block).getPath()).toString();
    }

    private static void tryMapBlockState(BlockState state, ProblemReporter reporter, PackContext context, boolean includeVanilla, Consumer<GeyserBlockMapping.BlockDefinition.Builder> definitionConsumer) {
        context.assetResolver().getBlockStateModel(state).ifPresentOrElse(root -> {
            if (root instanceof BlockStateModel.SimpleCachedUnbakedRoot simpleRoot) {
                BlockStateModel.Unbaked contents = ((BlockStateModelSimpleCachedUnbakedRootAccessor) simpleRoot).getContents();
                // TODO better weight handling
                Variant stateVariant = contents instanceof SingleVariant.Unbaked(Variant variant) ? variant
                        : ((SingleVariant.Unbaked) ((WeightedVariants.Unbaked) contents).entries().unwrap().getFirst().value()).variant();
                // Only map variants that don't use a vanilla block model
                if (includeVanilla || !stateVariant.modelLocation().getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
                    context.assetResolver().getResolvedModel(stateVariant.modelLocation())
                            .ifPresentOrElse(model -> definitionConsumer.accept(mapBlockState(state, model, stateVariant.modelState(), context)),
                                    () -> reporter.report(() -> "missing block model: " + stateVariant.modelLocation()));
                }
            } else {
                reporter.report(() -> "only mapping of simple roots is supported for now");
            }
        }, () -> reporter.report(() -> "missing block state definition"));
    }

    private static GeyserBlockMapping.BlockDefinition.Builder mapBlockState(BlockState state, ResolvedModel model, Variant.SimpleModelState modelState, PackContext context) {
        GeyserBlockMapping.BlockDefinition.Builder builder = GeyserBlockMapping.definition();

        UnbakedGeometry geometry = model.getTopGeometry();
        boolean ambientOcclusion = model.getTopAmbientOcclusion();

        // We can't specify light emission per single model element, so we specify the highest value that is used across the model
        // FIXME this seem to be actual light on bedrock?
        builder.withLightEmission(getMaxLightEmission(geometry));
        BlockModelTextures textures = context.blockTextureCache().load(model, () -> new BlockModelTextures(model.getTopTextureSlots(), context.assetResolver()));

        /*if (looksLikeFullBlockModel(model.getTopGeometry())) {
            builder.withFullBlockGeometry(GeyserBlockMapping.materials()
                    .withInstance("*", "FIXME", GeyserBlockMapping.MaterialInstances.Instance.RenderMethod.OPAQUE, true, ambientOcclusion));
        } else if (looksLikeCrossBlockModel(model.getTopGeometry())) {
            builder.withCrossGeometry(GeyserBlockMapping.materials()
                    .withInstance("*", "FIXME", GeyserBlockMapping.MaterialInstances.Instance.RenderMethod.OPAQUE, true, ambientOcclusion));
        } else {*/
            Identifier modelIdentifier = Rainbow.getModelIdentifier(model);
            Optional<MappedGeometry> mappedGeometry = Optional.of(context.geometryCache().mapGeometry(modelIdentifier, model, Transformation.IDENTITY, textures));
            GeyserBlockMapping.MaterialInstances materials = mapMaterials(geometry, model.getTopTextureSlots(), ambientOcclusion);
            builder.withGeometry(mappedGeometry.get().identifier(), materials);
        //}

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
                        builder.withInstance(GeometryMapper.getMeaningfulMaterialInstanceName(direction, face.texture(), elementIndex),
                                Rainbow.bedrockSafeIdentifier(material.sprite()), GeyserBlockMapping.MaterialInstances.Instance.RenderMethod.OPAQUE, true, ambientOcclusion);
                    }
                });
            }
            return builder.build();
        }
        return GeyserBlockMapping.MaterialInstances.EMPTY;
    }

    private static int getMaxLightEmission(UnbakedGeometry geometry) {
        if (geometry instanceof UnbakedCuboidGeometry(List<CuboidModelElement> elements)) {
            int maxEmission = 0;
            for (CuboidModelElement element : elements) {
                if (element.lightEmission() > maxEmission) {
                    maxEmission = element.lightEmission();
                }
            }
            return maxEmission;
        }
        return 0;
    }

    private static String blockStatePropertiesToString(BlockState state) {
        // generics... you get what I mean
        return new PropertyValueList((List) state.getProperties().stream().map(property -> property.value(state)).toList()).getKey();
    }
}
