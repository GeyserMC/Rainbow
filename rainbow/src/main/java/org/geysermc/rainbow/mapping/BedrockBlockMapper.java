package org.geysermc.rainbow.mapping;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.SingleVariant;
import net.minecraft.client.renderer.block.dispatch.Variant;
import net.minecraft.client.renderer.block.dispatch.WeightedVariants;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.cuboid.CuboidModelElement;
import net.minecraft.client.resources.model.cuboid.UnbakedCuboidGeometry;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.definition.block.GeyserBlockMapping;
import org.geysermc.rainbow.mixin.BlockStateModelSimpleCachedUnbakedRootAccessor;

import java.util.List;
import java.util.function.Consumer;

public class BedrockBlockMapper {

    public static void tryMapBlock(Block block, ProblemReporter reporter, PackContext context) {
        Identifier key = BuiltInRegistries.BLOCK.getKey(block);
        GeyserBlockMapping.Builder mapping = GeyserBlockMapping.builder(Rainbow.getModdedIdentifier(key.getPath()).toString());

        StateDefinition<Block, BlockState> stateDefinition = block.getStateDefinition();
        if (stateDefinition.isSingletonState()) {
            tryMapBlockState(stateDefinition.any(), reporter, context, definition -> context.mappings().blocks().map(block.builtInRegistryHolder(), mapping.withBase(definition)));
        }

        mapping.onlyOverrideStates();
        stateDefinition.getPossibleStates().forEach(state -> tryMapBlockState(state, reporter.forChild(() -> "state " + state + " "), context,
                definition -> mapping.withStateOverride("", definition))); // TODO

        if (mapping.hasStateOverrides()) {
            context.mappings().blocks().map(block.builtInRegistryHolder(), mapping);
        }
    }

    public static void tryMapBlockState(BlockState state, ProblemReporter reporter, PackContext context, Consumer<GeyserBlockMapping.BlockDefinition.Builder> definitionConsumer) {
        context.assetResolver().getBlockStateModel(state).ifPresentOrElse(root -> {
            if (root instanceof BlockStateModel.SimpleCachedUnbakedRoot simpleRoot) {
                BlockStateModel.Unbaked contents = ((BlockStateModelSimpleCachedUnbakedRootAccessor) simpleRoot).getContents();
                // TODO better weight handling
                Variant stateVariant = contents instanceof SingleVariant.Unbaked(Variant variant) ? variant
                        : ((SingleVariant.Unbaked) ((WeightedVariants.Unbaked) contents).entries().unwrap().getFirst().value()).variant();
                // Only map variants that don't use a vanilla block model
                if (!stateVariant.modelLocation().getNamespace().equals(Identifier.DEFAULT_NAMESPACE)) {
                    context.assetResolver().getResolvedModel(stateVariant.modelLocation())
                            .ifPresentOrElse(model -> definitionConsumer.accept(mapBlockState(state, model, stateVariant.modelState())),
                                    () -> reporter.report(() -> "missing block model: " + stateVariant.modelLocation()));
                }
            } else {
                reporter.report(() -> "only mapping of simple roots is supported for now");
            }
        }, () -> reporter.report(() -> "missing block state definition"));
    }

    private static GeyserBlockMapping.BlockDefinition.Builder mapBlockState(BlockState state, ResolvedModel model, Variant.SimpleModelState modelState) {
        GeyserBlockMapping.BlockDefinition.Builder builder = GeyserBlockMapping.definition();

        if (looksLikeFullBlockModel(model.getTopGeometry())) {
            builder.withFullBlockGeometry(GeyserBlockMapping.materials()
                    .withInstance("*", "FIXME", GeyserBlockMapping.MaterialInstances.Instance.RenderMethod.OPAQUE, true, true));
        } else {
            // FIXME
        }
        return builder;
    }

    private static boolean looksLikeFullBlockModel(UnbakedGeometry geometry) {
        if (geometry instanceof UnbakedCuboidGeometry(List<CuboidModelElement> elements) && elements.size() == 1) {
            CuboidModelElement element = elements.getFirst();
            return element.from().equals(0.0F, 0.0F, 0.0F) && element.to().equals(16.0F, 16.0F, 16.0F);
        }
        return false;
    }
}
