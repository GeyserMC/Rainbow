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

package org.geysermc.rainbow.mapping;

import com.mojang.math.Transformation;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ConditionalItemModel;
import net.minecraft.client.renderer.item.CuboidItemModelWrapper;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ItemModels;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.client.renderer.item.SelectItemModel;
import net.minecraft.client.renderer.item.properties.conditional.Broken;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperties;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.renderer.item.properties.conditional.CustomModelDataProperty;
import net.minecraft.client.renderer.item.properties.conditional.Damaged;
import net.minecraft.client.renderer.item.properties.conditional.FishingRodCast;
import net.minecraft.client.renderer.item.properties.conditional.HasComponent;
import net.minecraft.client.renderer.item.properties.numeric.BundleFullness;
import net.minecraft.client.renderer.item.properties.numeric.Count;
import net.minecraft.client.renderer.item.properties.numeric.Damage;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.client.renderer.item.properties.select.Charge;
import net.minecraft.client.renderer.item.properties.select.ContextDimension;
import net.minecraft.client.renderer.item.properties.select.DisplayContext;
import net.minecraft.client.renderer.item.properties.select.SelectItemModelProperties;
import net.minecraft.client.renderer.item.properties.select.TrimMaterialProperty;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.ArrayUtils;
import org.geysermc.rainbow.ProblemSuccessReporter;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.mapping.attachable.BedrockAttachableContext;
import org.geysermc.rainbow.mapping.geometry.BedrockGeometryContext;
import org.geysermc.rainbow.definition.item.GeyserBaseItemDefinition;
import org.geysermc.rainbow.definition.item.GeyserItemDefinition;
import org.geysermc.rainbow.definition.item.GeyserLegacyItemDefinition;
import org.geysermc.rainbow.definition.item.GeyserSingleItemDefinition;
import org.geysermc.rainbow.definition.item.predicate.GeyserConditionPredicate;
import org.geysermc.rainbow.definition.item.predicate.GeyserMatchPredicate;
import org.geysermc.rainbow.definition.item.predicate.GeyserPredicate;
import org.geysermc.rainbow.definition.item.predicate.GeyserRangeDispatchPredicate;
import org.geysermc.rainbow.mapping.texture.ItemModelTextures;
import org.geysermc.rainbow.mapping.texture.TextureHolder;
import org.geysermc.rainbow.mixin.LateBoundIdMapperAccessor;
import org.geysermc.rainbow.mixin.RangeSelectItemModelAccessor;
import org.geysermc.rainbow.pack.BedrockItem;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

public class BedrockItemMapper {
    private static final List<Identifier> TRIMMABLE_ARMOR_TAGS = Stream.of("is_armor", "trimmable_armors")
            .map(Identifier::withDefaultNamespace)
            .toList();

    private static <T> Identifier getId(ExtraCodecs.LateBoundIdMapper<Identifier, T> mapper,
                                        T type) {
        //noinspection unchecked
        return ((LateBoundIdMapperAccessor<Identifier, ?>) mapper).getIdToValue().inverse().get(type);
    }

    public static void tryMapStack(ItemStackTemplate stack, Identifier modelIdentifier, ProblemSuccessReporter reporter, PackContext context, boolean ignoreTopPlainModel) {
        context.assetResolver().getClientItem(modelIdentifier).map(ClientItem::model)
                .ifPresentOrElse(model -> mapItem(model, stack, reporter.forChild(() -> "client item definition " + modelIdentifier + " "),
                                base -> new GeyserSingleItemDefinition(base, Optional.of(modelIdentifier)), context, ignoreTopPlainModel),
                        () -> reporter.report(() -> "missing client item definition " + modelIdentifier));
    }

    public static boolean tryMapStack(ItemStackTemplate stack, int customModelData, ProblemSuccessReporter reporter, PackContext context) {
        Identifier itemModel = stack.get(DataComponents.ITEM_MODEL);
        assert itemModel != null;
        ItemModel.Unbaked vanillaModel = context.assetResolver().getClientItem(itemModel).map(ClientItem::model).orElseThrow();
        ProblemSuccessReporter childReporter = reporter.forChild(() -> "item model " + itemModel + " with custom model data " + customModelData + " ");
        if (vanillaModel instanceof RangeSelectItemModel.Unbaked(Optional<Transformation> _, RangeSelectItemModelProperty property, float scale, List<RangeSelectItemModel.Entry> entries, Optional<ItemModel.Unbaked> fallback)) {
            // WHY, Mojang?
            if (property instanceof net.minecraft.client.renderer.item.properties.numeric.CustomModelDataProperty(int index)) {
                if (index == 0) {
                    List<RangeSelectItemModel.Entry> sortedEntries = entries.stream()
                            .sorted(RangeSelectItemModel.Entry.BY_THRESHOLD)
                            .toList();
                    float scaledCustomModelData = customModelData * scale;

                    float[] thresholds = ArrayUtils.toPrimitive(sortedEntries.stream()
                            .map(RangeSelectItemModel.Entry::threshold)
                            .toArray(Float[]::new));
                    int modelIndex = RangeSelectItemModelAccessor.invokeLastIndexLessOrEqual(thresholds, scaledCustomModelData);
                    Optional<ItemModel.Unbaked> model = modelIndex == -1 ? fallback : Optional.of(sortedEntries.get(modelIndex).model());
                    model.ifPresentOrElse(present -> mapItem(present, stack, childReporter,
                                    base -> new GeyserLegacyItemDefinition(base, customModelData), context, false),
                            () -> childReporter.report(() -> "custom model data index lookup returned -1, and no fallback is present"));
                    return true;
                } else {
                    childReporter.report(() -> "range_dispatch custom model data property index is not zero, unable to apply custom model data");
                    return false;
                }
            }
        }
        childReporter.report(() -> "item model is not range_dispatch, unable to apply custom model data");
        return false;
    }

    public static void mapItem(ItemModel.Unbaked model, ItemStackTemplate stack, ProblemSuccessReporter reporter,
                               Function<GeyserBaseItemDefinition, GeyserItemDefinition> definitionCreator, PackContext packContext,
                               boolean ignoreTopPlainModel) {
        mapItem(model, new MappingContext(stack, reporter, definitionCreator, packContext, ignoreTopPlainModel));
    }

    private static void mapItem(ItemModel.Unbaked model, MappingContext context) {
        switch (model) {
            case CuboidItemModelWrapper.Unbaked modelWrapper -> {
                if (context.ignorePlainModel) {
                    context.reportSuccess("ignoring plain model as requested by context");
                } else {
                    mapBlockModelWrapper(modelWrapper, context.child("plain model " + modelWrapper.model()));
                }
            }
            case ConditionalItemModel.Unbaked conditional -> mapConditionalModel(conditional, context.child("condition model "));
            case RangeSelectItemModel.Unbaked rangeSelect -> mapRangeSelectModel(rangeSelect, context.child("range select model "));
            case SelectItemModel.Unbaked select -> mapSelectModel(select, context.child("select model "));
            default -> context.report("unsupported item model " + getId(ItemModels.ID_MAPPER, model.type()));
        }
    }

    private static void mapBlockModelWrapper(CuboidItemModelWrapper.Unbaked model, MappingContext context) {
        context.map(model);
    }

    private static void mapConditionalModel(ConditionalItemModel.Unbaked model, MappingContext context) {
        ConditionalItemModelProperty property = model.property();
        GeyserConditionPredicate.Property predicateProperty = switch (property) {
            case Broken _ -> GeyserConditionPredicate.BROKEN;
            case Damaged _ -> GeyserConditionPredicate.DAMAGED;
            case CustomModelDataProperty customModelData -> new GeyserConditionPredicate.CustomModelData(customModelData.index());
            case HasComponent hasComponent -> new GeyserConditionPredicate.HasComponent(hasComponent.componentType()); // ignoreDefault property not a thing, we should look into that in Geyser! TODO
            case FishingRodCast _ -> GeyserConditionPredicate.FISHING_ROD_CAST;
            default -> null;
        };
        ItemModel.Unbaked onTrue = model.onTrue();
        ItemModel.Unbaked onFalse = model.onFalse();

        if (predicateProperty == null) {
            context.report("unsupported conditional model property " + getId(ConditionalItemModelProperties.ID_MAPPER, property.type()) + ", only mapping on_false");
            mapItem(onFalse, context.child("condition on_false (unsupported property)", model.transformation()));
            return;
        }

        mapItem(onTrue, context.child("condition on_true", child -> {
            switch (property) {
                case Broken _ -> child.withComponent(DataComponents.DAMAGE, context.itemStack.getOrDefault(DataComponents.MAX_DAMAGE, 1) - 1);
                case Damaged _ -> child.withComponent(DataComponents.DAMAGE, 1);
                case CustomModelDataProperty customModelData -> child.mergeComponent(DataComponents.CUSTOM_MODEL_DATA, mergeCustomModelDataFlag(true, customModelData.index()));
                default -> {}
            }
            return child
                    .withPredicate(new GeyserConditionPredicate(predicateProperty, true))
                    .withTransformation(model.transformation());
        }));
        mapItem(onFalse, context.child("condition on_false", child -> child
                .withPredicate(new GeyserConditionPredicate(predicateProperty, false))
                .withTransformation(model.transformation())));
    }

    private static void mapRangeSelectModel(RangeSelectItemModel.Unbaked model, MappingContext context) {
        RangeSelectItemModelProperty property = model.property();
        GeyserRangeDispatchPredicate.Property predicateProperty = switch (property) {
            case BundleFullness ignored -> GeyserRangeDispatchPredicate.BUNDLE_FULLNESS;
            case Count count -> new GeyserRangeDispatchPredicate.Count(count.normalize());
            // Mojang, why? :(
            case net.minecraft.client.renderer.item.properties.numeric.CustomModelDataProperty customModelData -> new GeyserRangeDispatchPredicate.CustomModelData(customModelData.index()); // TODO set component in stack
            case Damage damage -> new GeyserRangeDispatchPredicate.Damage(damage.normalize()); // TODO set component in stack
            default -> null;
        };

        if (predicateProperty == null) {
            context.report("unsupported range dispatch model property " + getId(RangeSelectItemModelProperties.ID_MAPPER, property.type()) + ", only mapping fallback, if it is present");
        } else {
            for (RangeSelectItemModel.Entry entry : model.entries()) {
                mapItem(entry.model(), context.child("threshold " + entry.threshold(), child -> child
                        .withPredicate(new GeyserRangeDispatchPredicate(predicateProperty, entry.threshold(), model.scale()))
                        .withTransformation(model.transformation())));
            }
        }

        model.fallback().ifPresent(fallback -> mapItem(fallback, context.child("range dispatch fallback", model.transformation())));
    }

    @SuppressWarnings("unchecked")
    private static void mapSelectModel(SelectItemModel.Unbaked model, MappingContext context) {
        SelectItemModel.UnbakedSwitch<?, ?> unbakedSwitch = model.unbakedSwitch();
        Function<Object, GeyserMatchPredicate.MatchPredicateData> dataConstructor = switch (unbakedSwitch.property()) {
            case Charge ignored -> chargeType -> new GeyserMatchPredicate.ChargeType((CrossbowItem.ChargeType) chargeType);
            case TrimMaterialProperty ignored -> material -> new GeyserMatchPredicate.TrimMaterialData((ResourceKey<TrimMaterial>) material);
            case ContextDimension ignored -> dimension -> new GeyserMatchPredicate.ContextDimension((ResourceKey<Level>) dimension);
            // Why, Mojang?
            case net.minecraft.client.renderer.item.properties.select.CustomModelDataProperty customModelData -> string -> new GeyserMatchPredicate.CustomModelData((String) string, customModelData.index());
            default -> null;
        };
        // TODO: make this cleaner
        // Can't translate trim material to a property since we'd need a pattern and registry context
        Function<Object, UnaryOperator<MappingContext.ChildBuilder>> componentSetter = switch (unbakedSwitch.property()) {
            case Charge _ -> chargeType -> child -> {
                CrossbowItem.ChargeType type = (CrossbowItem.ChargeType) chargeType;
                return switch (type) {
                    case NONE -> child.withoutComponent(DataComponents.CHARGED_PROJECTILES);
                    case ARROW -> child.withComponent(DataComponents.CHARGED_PROJECTILES, new ChargedProjectiles(List.of(new ItemStackTemplate(Items.ARROW))));
                    case ROCKET -> child.withComponent(DataComponents.CHARGED_PROJECTILES, new ChargedProjectiles(List.of(new ItemStackTemplate(Items.FIREWORK_ROCKET))));
                };
            };
            case net.minecraft.client.renderer.item.properties.select.CustomModelDataProperty customModelData -> string ->
                    child -> child.mergeComponent(DataComponents.CUSTOM_MODEL_DATA, mergeCustomModelDataString((String) string, customModelData.index()));
            default -> _ -> UnaryOperator.identity();
        };

        List<? extends SelectItemModel.SwitchCase<?>> cases = unbakedSwitch.cases();

        if (dataConstructor == null) {
            if (unbakedSwitch.property() instanceof DisplayContext) {
                context.report("unsupported select model property display_context, only mapping \"gui\" case, if it exists");
                for (SelectItemModel.SwitchCase<?> switchCase : cases) {
                    if (switchCase.values().contains(ItemDisplayContext.GUI)) {
                        mapItem(switchCase.model(), context.child("select GUI display_context case (unsupported property) ", model.transformation()));
                        return;
                    }
                }
            }
            context.report("unsupported select model property " + getId(SelectItemModelProperties.ID_MAPPER, unbakedSwitch.property().type()) + ", only mapping fallback, if present");
            model.fallback().ifPresent(fallback -> mapItem(fallback, context.child("select fallback case (unsupported property) ", model.transformation())));
            return;
        }

        cases.forEach(switchCase -> {
            switchCase.values().forEach(value -> {
                mapItem(switchCase.model(), context.child("select case " + value + " ", child -> componentSetter.apply(value).apply(child)
                        .withPredicate(new GeyserMatchPredicate(dataConstructor.apply(value)))
                        .withTransformation(model.transformation())));
            });
        });
        model.fallback().ifPresent(fallback -> mapItem(fallback, context.child("select fallback case ", model.transformation())));
    }

    private static Function<@Nullable CustomModelData, CustomModelData> mergeCustomModelDataFloat(float f, int index) {
        return mergeCustomModelData(CustomModelData::floats, f, index, 0.0F,
                (data, floats) -> new CustomModelData(floats, data.flags(), data.strings(), data.colors()));
    }

    private static Function<@Nullable CustomModelData, CustomModelData> mergeCustomModelDataFlag(boolean flag, int index) {
        return mergeCustomModelData(CustomModelData::flags, flag, index, false,
                (data, flags) -> new CustomModelData(data.floats(), flags, data.strings(), data.colors()));
    }

    private static Function<@Nullable CustomModelData, CustomModelData> mergeCustomModelDataString(String string, int index) {
        return mergeCustomModelData(CustomModelData::strings, string, index, "",
                (data, strings) -> new CustomModelData(data.floats(), data.flags(), strings, data.colors()));
    }

    private static <T> Function<@Nullable CustomModelData, CustomModelData> mergeCustomModelData(Function<CustomModelData, List<T>> getter, T value, int index, T filler,
                                                                           BiFunction<CustomModelData, List<T>, CustomModelData> constructor) {
        return data -> {
            if (data == null) {
                data = CustomModelData.EMPTY;
            }
            List<T> existing = new ArrayList<>(getter.apply(data));
            if (existing.size() > index) {
                existing.set(index, value);
            } else if (existing.size() == index) {
                existing.add(value);
            } else {
                for (int i = 0; i < index - existing.size(); i++) {
                    existing.add(filler);
                }
                existing.add(value);
            }
            return constructor.apply(data, Collections.unmodifiableList(existing));
        };
    }

    private record MappingContext(List<GeyserPredicate> predicateStack, Optional<Transformation> transformationStack,
                                  ItemStackTemplate itemStack, ProblemSuccessReporter reporter,
                                  Function<GeyserBaseItemDefinition, GeyserItemDefinition> definitionCreator, PackContext packContext,
                                  boolean ignorePlainModel) {

        public MappingContext(ItemStackTemplate stack, ProblemSuccessReporter reporter, Function<GeyserBaseItemDefinition, GeyserItemDefinition> definitionCreator, PackContext packContext,
                              boolean ignorePlainModel) {
            this(List.of(), Optional.empty(), stack, reporter, definitionCreator, packContext, ignorePlainModel);
        }

        public MappingContext child(String childName, UnaryOperator<ChildBuilder> builder) {
            return builder.apply(new ChildBuilder(childName, this)).build();
        }

        public MappingContext child(String childName, Optional<Transformation> transformation) {
            return child(childName, child -> child.withTransformation(transformation));
        }

        public MappingContext child(String childName)  {
            return child(childName, UnaryOperator.identity());
        }

        public static class ChildBuilder {
            private final String name;
            private final MappingContext base;
            private final List<GeyserPredicate> predicateStack;
            private Optional<Transformation> transformationStack;
            private final DataComponentPatch.Builder componentPatch = DataComponentPatch.builder();

            private ChildBuilder(String name, MappingContext context) {
                this.name = name;
                this.base = context;
                this.predicateStack = new ArrayList<>(context.predicateStack);
                this.transformationStack = context.transformationStack;

                DataComponentPatch.SplitResult splitStack = context.itemStack.components().split();
                splitStack.added().forEach(componentPatch::set);
                splitStack.removed().forEach(componentPatch::remove);
            }

            public ChildBuilder withPredicate(GeyserPredicate predicate) {
                predicateStack.add(predicate);
                return this;
            }

            public ChildBuilder withTransformation(Optional<Transformation> transformation) {
                transformationStack = addTransformation(transformation);
                return this;
            }

            public <T> ChildBuilder withComponent(DataComponentType<T> type, T value) {
                componentPatch.set(type, value);
                return this;
            }

            public <T> ChildBuilder mergeComponent(DataComponentType<T> type, Function<@Nullable T, T> merger) {
                return withComponent(type, merger.apply(componentPatch.build().get(base.itemStack, type)));
            }

            public ChildBuilder withoutComponent(DataComponentType<?> type) {
                componentPatch.remove(type);
                return this;
            }

            private MappingContext build() {
                // Only copy ignorePlainModel when there is not a predicate
                return new MappingContext(Collections.unmodifiableList(predicateStack), transformationStack, new ItemStackTemplate(base.itemStack.item(), base.itemStack.count(), componentPatch.build()),
                        base.reporter.forChild(() -> name), base.definitionCreator, base.packContext, base.ignorePlainModel && predicateStack.isEmpty());
            }

            private Optional<Transformation> addTransformation(Optional<Transformation> optionalChild) {
                return optionalChild.flatMap(child -> transformationStack.map(parent -> parent.compose(child)).or(() -> optionalChild));
            }
        }

        public Transformation finaliseTransformation(Optional<Transformation> finalTransformation) {
            return addTransformation(finalTransformation).orElse(Transformation.IDENTITY);
        }

        public void map(CuboidItemModelWrapper.Unbaked model) {
            Identifier modelIdentifier = model.model();

            packContext.assetResolver().getResolvedModel(modelIdentifier)
                    .ifPresentOrElse(itemModel -> {
                        Identifier bedrockIdentifier;
                        if (Rainbow.isVanilla(modelIdentifier)) {
                            bedrockIdentifier = Identifier.fromNamespaceAndPath("geyser_mc", modelIdentifier.getPath());
                        } else {
                            bedrockIdentifier = modelIdentifier;
                        }

                        ItemModelTextures textures = packContext.itemTextureCache().load(itemModel, () -> ItemModelTextures.load(itemModel, packContext));

                        BedrockGeometryContext geometry = BedrockGeometryContext.create(bedrockIdentifier, itemModel, finaliseTransformation(model.transformation()), textures, packContext);
                        BedrockAttachableContext attachable = BedrockAttachableContext.create(bedrockIdentifier, itemStack, geometry, textures, packContext);

                        if (packContext.reportSuccesses()) {
                            // Not a problem, but just report to get the model printed in the report file
                            reporter.reportSuccess(() -> "creating mapping for block model " + modelIdentifier);
                        }
                        create(bedrockIdentifier, textures, geometry, attachable);
                    }, () -> report("missing block model " + modelIdentifier));
        }

        private void create(Identifier bedrockIdentifier, ItemModelTextures textures, BedrockGeometryContext geometry, BedrockAttachableContext attachable) {
            List<Identifier> tags = itemStack.is(ItemTags.TRIMMABLE_ARMOR) ? TRIMMABLE_ARMOR_TAGS : List.of();

            TextureHolder icon = textures.icon().create(bedrockIdentifier, itemStack);
            GeyserBaseItemDefinition base = new GeyserBaseItemDefinition(bedrockIdentifier,
                    Optional.ofNullable(itemStack.components().split().added().get(DataComponents.ITEM_NAME)).map(Component::tryCollapseToString),
                    predicateStack,
                    new GeyserBaseItemDefinition.BedrockOptions(Optional.of(icon.bedrockSafeName()), true, geometry.handheld(), calculateProtectionValue(itemStack), tags),
                    itemStack.components());
            try {
                packContext.mappings().items().map(itemStack.item(), definitionCreator.apply(base));
            } catch (Exception exception) {
                reporter.forChild(() -> "mapping with bedrock identifier " + bedrockIdentifier + " ").report(() -> "failed to pass mapping: " + exception.getMessage());
                return;
            }

            packContext.assetConsumer().acceptItem(new BedrockItem(bedrockIdentifier, icon, textures, geometry, attachable));
        }

        public void report(String problem) {
            reporter.report(() -> problem);
        }

        public void reportSuccess(String success) {
            reporter.reportSuccess(() -> success);
        }

        private Optional<Transformation> addTransformation(Optional<Transformation> optionalChild) {
            return optionalChild.flatMap(child -> transformationStack.map(parent -> parent.compose(child)).or(() -> optionalChild));
        }

        private static int calculateProtectionValue(ItemStackTemplate stack) {
            ItemAttributeModifiers modifiers = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
            if (modifiers != null) {
                return modifiers.modifiers().stream()
                        .filter(modifier -> modifier.attribute() == Attributes.ARMOR && modifier.modifier().operation() == AttributeModifier.Operation.ADD_VALUE)
                        .mapToInt(entry -> (int) entry.modifier().amount())
                        .sum();
            }
            return 0;
        }
    }
}
