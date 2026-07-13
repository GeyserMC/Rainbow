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

package org.geysermc.rainbow.pack;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.waypoints.WaypointStyleAsset;
import org.geysermc.rainbow.CodecUtil;
import org.geysermc.rainbow.PackConstants;
import org.geysermc.rainbow.ProblemSuccessReporter;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.definition.GeyserMappings;
import org.geysermc.rainbow.definition.block.GeyserBlockMappings;
import org.geysermc.rainbow.definition.waypoint.GeyserWaypointStyleMappings;
import org.geysermc.rainbow.mapping.AssetResolver;
import org.geysermc.rainbow.mapping.BedrockAssetConsumer;
import org.geysermc.rainbow.mapping.BedrockBlockMapper;
import org.geysermc.rainbow.mapping.BedrockItemMapper;
import org.geysermc.rainbow.mapping.BedrockWaypointStyleMapper;
import org.geysermc.rainbow.mapping.PackContext;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;
import org.geysermc.rainbow.mapping.geometry.GeometryRenderer;
import org.geysermc.rainbow.definition.item.GeyserItemMappings;
import org.geysermc.rainbow.pack.sound.BedrockSoundDefinitions;
import org.geysermc.rainbow.pack.texture.BedrockFlipbookTextures;
import org.geysermc.rainbow.pack.texture.BedrockTextureAtlas;
import org.geysermc.rainbow.pack.texture.BedrockTextures;
import org.geysermc.rainbow.stats.PackStatKeys;
import org.geysermc.rainbow.stats.PackStats;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class BedrockPack implements BedrockAssetConsumer, PackSerializer.Serializable {
    // Debug only
    private static final boolean ALLOW_MAPPING_VANILLA_ITEMS = false;

    private final String name;
    private final Optional<PackManifest> manifest;
    private final PackPaths paths;
    private final PackSerializer serializer;

    private final BedrockTextures.Builder itemTextures = BedrockTextures.builder();
    private final BedrockTextures.Builder terrainTextures = BedrockTextures.builder();
    private final BedrockFlipbookTextures.Builder flipbookTextures = BedrockFlipbookTextures.builder();
    private final BedrockSoundDefinitions.Builder soundDefinitions = BedrockSoundDefinitions.builder();
    private final Set<BedrockBlock> bedrockBlocks = new HashSet<>();
    private final Set<BedrockItem> bedrockItems = new HashSet<>();
    private final Set<BedrockWaypointStyle> bedrockWaypointStyles = new HashSet<>();
    private final Set<Identifier> modelsMapped = new HashSet<>();
    private final Set<Pair<Holder<Item>, Integer>> customModelDataMapped = new HashSet<>();

    private final PackContext context;
    private final ProblemReporter reporter;

    public BedrockPack(String name, Optional<PackManifest> manifest, PackPaths paths, PackSerializer serializer, AssetResolver assetResolver,
                       Optional<GeometryRenderer> geometryRenderer, ProblemReporter reporter,
                       boolean reportSuccesses) {
        this.name = name;
        this.manifest = manifest;
        this.paths = paths;
        this.serializer = serializer;

        // Not reading existing item mappings/texture atlas for now since that doesn't work all that well yet
        this.context = new PackContext(new GeyserMappings(), paths, this, assetResolver, geometryRenderer, reportSuccesses);
        this.reporter = reporter;
    }

    public String name() {
        return name;
    }

    public MappingResults tryMapAllVanillaBlocks() {
        int startCount = bedrockBlocks.size();
        ProblemSuccessReporter mapReporter = new ProblemSuccessReporter(reporter);
        for (Block block : BuiltInRegistries.BLOCK) {
            Identifier identifier = BuiltInRegistries.BLOCK.getKey(block);
            if (Rainbow.isVanilla(identifier)) {
                BedrockBlockMapper.tryMapBlock(block, mapReporter, context);
            }
        }
        return new MappingResults(bedrockBlocks.size() - startCount, mapReporter.problemsSeen());
    }

    public MappingResult mapBlockStateExplicitly(BlockState state) {
        int startCount = bedrockBlocks.size();
        ProblemSuccessReporter mapReporter = new ProblemSuccessReporter(reporter);
        BedrockBlockMapper.tryMapBlockState(state, mapReporter, context);
        return bedrockBlocks.size() == startCount ? MappingResult.NONE_MAPPED : mapReporter.problemsSeen() > 0 ? MappingResult.PROBLEMS_OCCURRED : MappingResult.MAPPED_SUCCESSFULLY;
    }

    public MappingResult mapItem(ItemStackTemplate stack) {
        ProblemSuccessReporter mapReporter = new ProblemSuccessReporter(reporter);

        Identifier customModel = ALLOW_MAPPING_VANILLA_ITEMS ? stack.get(DataComponents.ITEM_MODEL) : stack.components().split().added().get(DataComponents.ITEM_MODEL);
        if (customModel == null) {
            // If no custom item_model patch exists, try custom model data
            CustomModelData customModelData = stack.components().split().added().get(DataComponents.CUSTOM_MODEL_DATA);
            if (customModelData == null) {
                return MappingResult.NONE_MAPPED;
            } else if (isLegacyCustomModelData(customModelData)) {
                // Legacy custom model data - only one float, nothing else
                int customModelInt = Objects.requireNonNull(customModelData.getFloat(0)).intValue();
                if (!customModelDataMapped.add(Pair.of(stack.item(), customModelInt))) {
                    return MappingResult.NONE_MAPPED;
                }

                if (BedrockItemMapper.tryMapStack(stack, customModelInt, mapReporter, context)) {
                    return mapReporter.problemsSeen() > 0 ? MappingResult.PROBLEMS_OCCURRED : MappingResult.MAPPED_SUCCESSFULLY;
                }
                // Fall through to mapping vanilla model if unable to map legacy custom model data - usually occurs when something *looks* like legacy custom model data, but actually isn't
                // (looking at you, Tool Trims)
            }

            // Try to map the vanilla model, but ignore the first direct plain model if present - this is the vanilla case
            Identifier vanillaModel = Objects.requireNonNull(stack.get(DataComponents.ITEM_MODEL));
            if (!modelsMapped.add(vanillaModel)) {
                return MappingResult.NONE_MAPPED;
            }
            BedrockItemMapper.tryMapStack(stack, vanillaModel, mapReporter, context, true);
        } else {
            if (!modelsMapped.add(customModel)) {
                return MappingResult.NONE_MAPPED;
            }
            BedrockItemMapper.tryMapStack(stack, customModel, mapReporter, context, false);
        }

        return mapReporter.problemsSeen() > 0 ? MappingResult.PROBLEMS_OCCURRED : MappingResult.MAPPED_SUCCESSFULLY;
    }

    public MappingResult mapItem(Holder<Item> item, DataComponentPatch patch) {
        return mapItem(new ItemStackTemplate(item, 1, patch));
    }

    public MappingResult mapWaypointStyle(ResourceKey<WaypointStyleAsset> key) {
        ProblemSuccessReporter mapReporter = new ProblemSuccessReporter(reporter);
        BedrockWaypointStyleMapper.tryMapWaypointStyle(key, mapReporter, context);
        return mapReporter.problemsSeen() > 0 ? MappingResult.PROBLEMS_OCCURRED : MappingResult.MAPPED_SUCCESSFULLY;
    }

    public boolean mapSounds(String namespace) {
        Map<String, SoundEventRegistration> sounds = context.assetResolver().getSoundRegistrations().get(namespace);
        if (sounds == null) {
            return false;
        }
        soundDefinitions.withRegistrations(namespace, sounds);
        return true;
    }

    public boolean mapAllSounds() {
        Map<String, Map<String, SoundEventRegistration>> sounds = context.assetResolver().getSoundRegistrations();
        if (sounds.isEmpty()) {
            return false;
        }
        soundDefinitions.withRegistrations(sounds);
        return true;
    }

    @Override
    public void acceptBlock(BedrockBlock block) {
        terrainTextures.withBlockTextures(block);
        block.textures().addFlipbookTextures(flipbookTextures);
        bedrockBlocks.add(block);
    }

    @Override
    public void acceptItem(BedrockItem item) {
        itemTextures.withItemTexture(item);
        bedrockItems.add(item);
    }

    @Override
    public void acceptWaypointStyle(BedrockWaypointStyle style) {
        bedrockWaypointStyles.add(style);
    }

    public CompletableFuture<?> save() {
        CompletableFuture<?> baseSerialization = save(createSerializingContext());
        if (reporter instanceof AutoCloseable closeable) {
            try {
                closeable.close();
            } catch (Exception ignored) {}
        }

        if (paths.zipOutput().isPresent()) {
            return baseSerialization.thenAcceptAsync(_ -> RainbowIO.safeIO(() -> CodecUtil.tryZipDirectory(paths.packRoot(), paths.zipOutput().get())));
        }
        return baseSerialization;
    }

    @Override
    public CompletableFuture<?> save(PackSerializingContext serializingContext) {
        return PackSerializer.Serializable.wrapCodec(GeyserBlockMappings.CODEC, context.mappings().blocks(), PackPaths::blockMappings)
                .with(GeyserItemMappings.CODEC, context.mappings().items(), PackPaths::itemMappings)
                .with(GeyserWaypointStyleMappings.CODEC, context.mappings().waypointStyles(), PackPaths::waypointStyleMappings)
                .with(PackManifest.CODEC, manifest, PackPaths::manifest)
                .with(BedrockTextureAtlas.CODEC, BedrockTextureAtlas.itemAtlas(name, itemTextures), PackPaths::itemAtlas)
                .with(BedrockTextureAtlas.CODEC, BedrockTextureAtlas.terrainAtlas(name, terrainTextures), PackPaths::terrainAtlas)
                .with(BedrockFlipbookTextures.CODEC, flipbookTextures.build(), PackPaths::flipbookTextures)
                .with(soundDefinitions.build())
                .with(bedrockBlocks)
                .with(bedrockItems)
                .with(bedrockWaypointStyles)
                .with(paths.languageOutput().map(languageFolder -> context -> LanguageUtil.saveLanguages(context, languageFolder)))
                .save(serializingContext);
    }

    public PackStats collectStats() {
        return PackStats.collector()
                .collect(context.mappings())
                .collect(PackStatKeys.ITEM_TEXTURE_ATLAS_SIZE, itemTextures.build())
                .collect(PackStatKeys.TERRAIN_TEXTURE_ATLAS_SIZE, terrainTextures.build())
                .collect(PackStatKeys.FLIPBOOK_TEXTURE_ATLAS_SIZE, flipbookTextures.build())
                .collect(PackStatKeys.SOUND_DEFINITIONS, soundDefinitions.build())
                .collect(PackStatKeys.ATTACHABLES, (int) bedrockItems.stream().filter(item -> item.attachableContext().attachable().isPresent()).count())
                .collect(context.geometryCache())
                .collect(context.blockTextureCache())
                .collect(context.itemTextureCache())
                .finish();
    }

    public Set<BedrockItem> getBedrockItems() {
        return Collections.unmodifiableSet(bedrockItems);
    }

    public ProblemReporter getReporter() {
        return reporter;
    }

    private PackSerializingContext createSerializingContext() {
        return new PackSerializingContext(context.assetResolver(), serializer, paths, reporter);
    }

    private static boolean isLegacyCustomModelData(CustomModelData customModelData) {
        return customModelData.floats().size() == 1 && customModelData.colors().isEmpty() && customModelData.flags().isEmpty() && customModelData.strings().isEmpty();
    }

    public static Builder builder(String name, Path mappingsPath, Path packRootPath, PackSerializer packSerializer, AssetResolver assetResolver) {
        return new Builder(name, mappingsPath, packRootPath, packSerializer, assetResolver);
    }

    public static class Builder {
        private final String name;
        private final Path mappingsRoot;
        private final Path packRoot;
        private final PackSerializer packSerializer;
        private final AssetResolver assetResolver;
        private @Nullable PackManifest manifest;
        private @Nullable Path packZipFile = null;
        private @Nullable Path languageFolder = null;
        private @Nullable GeometryRenderer geometryRenderer = null;
        private Function<ProblemReporter.PathElement, ProblemReporter> reporter;
        private boolean reportSuccesses = false;

        public Builder(String name, Path mappingsRoot, Path packRoot, PackSerializer packSerializer, AssetResolver assetResolver) {
            this.name = name;
            this.mappingsRoot = mappingsRoot;
            this.packRoot = packRoot;
            this.reporter = ProblemReporter.Collector::new;
            this.packSerializer = packSerializer;
            this.assetResolver = assetResolver;
            manifest = defaultManifest(name);
        }

        public Builder withManifest(@Nullable PackManifest manifest) {
            this.manifest = manifest;
            return this;
        }

        public Builder withPackZipFile(Path absolute) {
            packZipFile = absolute;
            return this;
        }

        public Builder withLanguageFolder(Path absolute) {
            languageFolder = absolute;
            return this;
        }

        public Builder withGeometryRenderer(GeometryRenderer renderer) {
            geometryRenderer = renderer;
            return this;
        }

        public Builder withReporter(Function<ProblemReporter.PathElement, ProblemReporter> reporter) {
            this.reporter = reporter;
            return this;
        }

        public Builder reportSuccesses() {
            this.reportSuccesses = true;
            return this;
        }

        public BedrockPack build() {
            PackPaths paths = new PackPaths(mappingsRoot, packRoot, Optional.ofNullable(packZipFile), Optional.ofNullable(languageFolder));
            return new BedrockPack(name, Optional.ofNullable(manifest), paths, packSerializer, assetResolver, Optional.ofNullable(geometryRenderer),
                    reporter.apply(() -> "Bedrock pack " + name), reportSuccesses);
        }

        private static PackManifest defaultManifest(String name) {
            return PackManifest.create(name, PackConstants.DEFAULT_PACK_DESCRIPTION, UUID.randomUUID(), UUID.randomUUID(), BedrockVersion.of(0));
        }
    }

    public enum MappingResult {
        NONE_MAPPED,
        MAPPED_SUCCESSFULLY,
        PROBLEMS_OCCURRED
    }

    public record MappingResults(int amountMapped, int problems) {

        public MappingResult toSingleResult() {
            return problems > 0 ? MappingResult.PROBLEMS_OCCURRED : amountMapped > 0 ? BedrockPack.MappingResult.MAPPED_SUCCESSFULLY : BedrockPack.MappingResult.NONE_MAPPED;
        }
    }
}
