package org.geysermc.rainbow.datagen;

import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.model.ItemModelGenerator;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.mapping.AssetResolver;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.texture.TextureResource;
import org.geysermc.rainbow.pack.BedrockPack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

public abstract class RainbowModelProvider extends FabricModelProvider {
    private static final Logger PROBLEM_LOGGER = LoggerFactory.getLogger(Rainbow.MOD_ID);

    private final CompletableFuture<HolderLookup.Provider> registries;
    private final Map<ResourceKey<EquipmentAsset>, EquipmentClientInfo> equipmentInfos;
    private final String packName;
    private final Path geyserMappingsPath;
    private final Path packPath;

    private Map<Item, ClientItem> itemInfos;
    private Map<Identifier, ModelInstance> models;

    protected RainbowModelProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries,
                                   Map<ResourceKey<EquipmentAsset>, EquipmentClientInfo> equipmentInfos, String packName,
                                   Identifier outputRoot, Path geyserMappingsPath, Path packPath) {
        super(output);
        this.registries = registries;
        this.equipmentInfos = equipmentInfos;
        this.packName = packName;

        Path computedOutputRoot = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, outputRoot.getPath())
                .file(outputRoot, "").getParent();
        this.geyserMappingsPath = computedOutputRoot.resolve(geyserMappingsPath);
        this.packPath = computedOutputRoot.resolve(packPath);
    }

    protected RainbowModelProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries,
                                   Map<ResourceKey<EquipmentAsset>, EquipmentClientInfo> equipmentInfos, String packName,
                                   Identifier outputRoot) {
        this(output, registries, equipmentInfos, packName, outputRoot, Path.of("geyser_mappings.json"), Path.of("pack"));
    }

    protected RainbowModelProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries,
                                Map<ResourceKey<EquipmentAsset>, EquipmentClientInfo> equipmentInfos, String packName) {
        this(output, registries, equipmentInfos, packName, Identifier.withDefaultNamespace("bedrock"));
    }

    protected RainbowModelProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries, String packName) {
        this(output, registries, Map.of(), packName);
    }

    protected RainbowModelProvider(FabricDataOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this(output, registries, Rainbow.MOD_ID + "-generated");
    }

    @Override
    public @NotNull CompletableFuture<?> run(CachedOutput output) {
        CompletableFuture<?> vanillaModels = super.run(output);

        CompletableFuture<BedrockPack> bedrockPack = ClientPackLoader.openClientResources()
                .thenCompose(resourceManager -> registries.thenApply(registries -> {
                    try (resourceManager) {
                        BedrockPack pack = createBedrockPack(new Serializer(output, registries),
                                new DatagenResolver(resourceManager, equipmentInfos, itemInfos, models)).build();

                        Set<Item> sortedItemInfos = new TreeSet<>(Comparator.comparing(item -> item.builtInRegistryHolder().key().identifier()));
                        sortedItemInfos.addAll(itemInfos.keySet());
                        for (Item item : sortedItemInfos) {
                            pack.map(getVanillaItem(item).builtInRegistryHolder(), getVanillaDataComponentPatch(item));
                        }
                        return pack;
                    }
                }));

        return CompletableFuture.allOf(vanillaModels, bedrockPack.thenCompose(BedrockPack::save));
    }

    protected BedrockPack.Builder createBedrockPack(PackSerializer serializer, AssetResolver resolver) {
        return BedrockPack.builder(packName, geyserMappingsPath, packPath, serializer, resolver)
                .withReporter(path -> new ProblemReporter.ScopedCollector(path, PROBLEM_LOGGER));
    }

    protected abstract Item getVanillaItem(Item modded);

    protected DataComponentPatch getVanillaDataComponentPatch(Item modded) {
        DataComponentPatch.Builder builder = DataComponentPatch.builder();
        modded.components().forEach(builder::set);
        return builder.build();
    }

    @ApiStatus.Internal
    public void setItemInfos(Map<Item, ClientItem> itemInfos) {
        this.itemInfos = itemInfos;
    }

    @ApiStatus.Internal
    public void setModels(Map<Identifier, ModelInstance> models) {
        this.models = models;
    }

    private record Serializer(CachedOutput output, HolderLookup.Provider registries) implements PackSerializer {

        @Override
        public <T> CompletableFuture<?> saveJson(Codec<T> codec, T object, Path path) {
            return DataProvider.saveStable(output, registries, codec, object, path);
        }

        @Override
        public CompletableFuture<?> saveTexture(byte[] texture, Path path) {
            return CompletableFuture.runAsync(() -> {
                try {
                    output.writeIfNeeded(path, texture, Hashing.sha1().hashBytes(texture));
                } catch (IOException exception) {
                    LOGGER.error("Failed to save texture to {}", path, exception);
                }
            }, Util.backgroundExecutor().forName("PackSerializer-saveTexture"));
        }
    }

    private static class DatagenResolver implements AssetResolver {
        private final ResourceManager resourceManager;
        private final Map<ResourceKey<EquipmentAsset>, EquipmentClientInfo> equipmentInfos;
        private final Map<Identifier, ClientItem> itemInfos;
        private final Map<Identifier, ModelInstance> models;
        private final Map<Identifier, Optional<ResolvedModel>> resolvedModelCache = new HashMap<>();

        private DatagenResolver(ResourceManager resourceManager, Map<ResourceKey<EquipmentAsset>, EquipmentClientInfo> equipmentInfos,
                                Map<Item, ClientItem> itemInfos, Map<Identifier, ModelInstance> models) {
            this.resourceManager = resourceManager;
            this.equipmentInfos = equipmentInfos;
            this.itemInfos = new HashMap<>();
            for (Map.Entry<Item, ClientItem> entry : itemInfos.entrySet()) {
                this.itemInfos.put(entry.getKey().builtInRegistryHolder().key().identifier(), entry.getValue());
            }
            this.models = models;
        }

        @Override
        public Optional<ResolvedModel> getResolvedModel(Identifier identifier) {
            return resolvedModelCache.computeIfAbsent(identifier, key -> Optional.ofNullable(models.get(identifier))
                    .<UnbakedModel>map(instance -> BlockModel.fromStream(new StringReader(instance.get().toString())))
                    .or(() -> {
                        if (identifier.equals(ItemModelGenerator.GENERATED_ITEM_MODEL_ID)) {
                            return Optional.of(new ItemModelGenerator());
                        }
                        return Optional.empty();
                    })
                    .or(() -> RainbowIO.safeIO(() -> {
                        try (BufferedReader reader = resourceManager.openAsReader(identifier.withPrefix("models/").withSuffix(".json"))) {
                            return BlockModel.fromStream(reader);
                        }
                    }))
                    .map(model -> new ResolvedModel() {
                        @Override
                        public @NotNull UnbakedModel wrapped() {
                            return model;
                        }

                        @Override
                        public @Nullable ResolvedModel parent() {
                            return Optional.ofNullable(model.parent()).flatMap(parent -> getResolvedModel(parent)).orElse(null);
                        }

                        @Override
                        public @NotNull String debugName() {
                            return identifier.toString();
                        }
                    }));
        }

        @Override
        public Optional<ClientItem> getClientItem(Identifier identifier) {
            return Optional.ofNullable(itemInfos.get(identifier));
        }

        @Override
        public Optional<EquipmentClientInfo> getEquipmentInfo(ResourceKey<EquipmentAsset> key) {
            return Optional.ofNullable(equipmentInfos.get(key));
        }

        @Override
        public Optional<TextureResource> getTexture(Identifier atlas, Identifier identifier) {
            // We don't care about atlas since there are none loaded at datagen
            return resourceManager.getResource(Rainbow.decorateTextureIdentifier(identifier))
                    .flatMap(resource -> RainbowIO.safeIO(() -> {
                        Optional<AnimationMetadataSection> animationMetadata = resource.metadata().getSection(AnimationMetadataSection.TYPE);
                        try (InputStream textureStream = resource.open()) {
                            NativeImage texture = NativeImage.read(textureStream);
                            return new TextureResource(texture, animationMetadata.map(animation -> animation.calculateFrameSize(texture.getWidth(), texture.getHeight())));
                        }
                    }));
        }
    }
}
