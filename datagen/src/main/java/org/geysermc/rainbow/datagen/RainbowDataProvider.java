package org.geysermc.rainbow.datagen;

import com.google.common.hash.Hashing;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.client.data.models.model.ModelInstance;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.client.resources.model.cuboid.CuboidModel;
import net.minecraft.client.resources.model.cuboid.ItemModelGenerator;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
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
import org.geysermc.rainbow.datagen.mixin.DataComponentInitializersAccessor;
import org.geysermc.rainbow.datagen.mixin.FabricLanguageProviderAccessor;
import org.geysermc.rainbow.datagen.accessor.LanguageProviderDataAccessor;
import org.geysermc.rainbow.datagen.accessor.ModelProviderDataAccessor;
import org.geysermc.rainbow.mapping.AssetResolver;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.texture.TextureResource;
import org.geysermc.rainbow.pack.BedrockPack;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

public abstract class RainbowDataProvider implements DataProvider {
    private static final Logger PROBLEM_LOGGER = LoggerFactory.getLogger(Rainbow.MOD_ID);

    private final CompletableFuture<HolderLookup.Provider> registries;
    private final String packName;
    private final Providers providers;
    private final Paths paths;

    private @Nullable Map<ResourceKey<?>, DataComponentMap.Builder> initializedItemComponents;

    protected RainbowDataProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries, String packName,
                                  Identifier outputRoot, Providers providers, Paths paths) {
        this.registries = registries;
        this.packName = packName;
        this.providers = providers;

        Path computedOutputRoot = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, outputRoot.getPath())
                .file(outputRoot, "").getParent(); // getParent() returns root -> assets/<output namespace>/<output path>
        this.paths = paths.resolveFrom(computedOutputRoot);
    }

    protected RainbowDataProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries, String packName,
                                  Identifier outputRoot, Providers providers) {
        this(output, registries, packName, outputRoot, providers, Paths.DEFAULT);
    }

    protected RainbowDataProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries, String packName,
                                  Providers providers) {
        this(output, registries, packName, Identifier.withDefaultNamespace("bedrock"), providers);
    }

    protected RainbowDataProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries, String packName,
                                  FabricModelProvider modelProvider) {
        this(output, registries, packName, new Providers(modelProvider, Optional.empty(), Map.of()));
    }

    protected RainbowDataProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries, FabricModelProvider modelProvider) {
        this(output, registries, Rainbow.MOD_ID + "-generated", modelProvider);
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        CompletableFuture<?> models = providers.models.run(output);
        CompletableFuture<?> languages = providers.languages
                .map(providers -> providers.stream()
                        .map(provider -> provider.run(output))
                        .collect(() -> CompletableFuture.completedFuture(null), CompletableFuture::allOf, CompletableFuture::allOf))
                .orElseGet(() -> CompletableFuture.completedFuture(null));

        CompletableFuture<BedrockPack> bedrockPack = ClientPackLoader.openClientResources()
                .thenCompose(resourceManager -> registries.thenApply(registries -> {
                    // You're not really supposed to do this, but Rainbow *needs* the initialised components to function properly
                    initializedItemComponents = ((DataComponentInitializersAccessor) BuiltInRegistries.DATA_COMPONENT_INITIALIZERS).invokeRunInitializers(registries);

                    try (resourceManager) {
                        BedrockPack pack = createBedrockPack(new Serializer(output, registries), new DatagenResolver(resourceManager, providers)).build();

                        Set<Item> sortedItemInfos = new TreeSet<>(Comparator.comparing(item -> item.builtInRegistryHolder().key().identifier()));
                        sortedItemInfos.addAll(providers.getItemInfos().keySet());
                        for (Item item : sortedItemInfos) {
                            pack.map(getVanillaItem(item).builtInRegistryHolder(), getVanillaDataComponentPatch(item));
                        }
                        return pack;
                    }
                }));

        return CompletableFuture.allOf(models, languages, bedrockPack.thenCompose(BedrockPack::save));
    }

    protected BedrockPack.Builder createBedrockPack(PackSerializer serializer, AssetResolver resolver) {
        return BedrockPack.builder(packName, paths.geyserMappingsPath, paths.packPath, serializer, resolver)
                .withLanguageFolder(paths.langPath)
                .withReporter(path -> new ProblemReporter.ScopedCollector(path, PROBLEM_LOGGER));
    }

    protected abstract Item getVanillaItem(Item modded);

    protected DataComponentPatch getVanillaDataComponentPatch(Item modded) {
        if (initializedItemComponents == null) {
            throw new IllegalStateException("initializedItemComponents may not be null");
        }
        DataComponentPatch.Builder builder = DataComponentPatch.builder();
        initializedItemComponents.get(modded.builtInRegistryHolder().key()).build().forEach(builder::set);
        return builder.build();
    }

    public static class Providers {
        private final FabricModelProvider models;
        private final Optional<List<FabricLanguageProvider>> languages;
        private final Map<ResourceKey<EquipmentAsset>, EquipmentClientInfo> equipmentInfos;

        public Providers(FabricModelProvider models, Optional<List<FabricLanguageProvider>> languages,
                         Map<ResourceKey<EquipmentAsset>, EquipmentClientInfo> equipmentInfos) {
            this.models = models;
            this.languages = languages;
            this.equipmentInfos = equipmentInfos;
        }

        public static Providers create(FabricModelProvider models, Optional<FabricLanguageProvider> languages,
                                       Map<ResourceKey<EquipmentAsset>, EquipmentClientInfo> equipmentInfos) {
            return new Providers(models, languages.map(List::of), equipmentInfos);
        }

        private Map<Item, ClientItem> getItemInfos() {
            return ((ModelProviderDataAccessor) models).rainbow$getItemInfos();
        }

        private Map<Identifier, ModelInstance> getModels() {
            return ((ModelProviderDataAccessor) models).rainbow$getModels();
        }
    }

    public record Paths(Path geyserMappingsPath, Path packPath, Path langPath) {
        // TODO reduce default duplication with PackManager in client
        public static final Paths DEFAULT = new Paths(Path.of("geyser_mappings.json"), Path.of("pack"), Path.of("lang"));

        public Paths resolveFrom(Path root) {
            return new Paths(root.resolve(geyserMappingsPath), root.resolve(packPath), root.resolve(langPath));
        }
    }

    private static class Serializer implements PackSerializer {
        private final CachedOutput output;
        private final HolderLookup.Provider registries;

        private Serializer(CachedOutput output, HolderLookup.Provider registries) {
            this.output = output;
            this.registries = registries;
        }

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
        private final Providers providers;

        private final Map<Identifier, Optional<ResolvedModel>> resolvedModelCache = new Object2ObjectOpenHashMap<>();
        private final Map<Identifier, ClientItem> itemInfos;
        private final Map<Identifier, ModelInstance> models;
        private final Map<String, Map<String, String>> foreignLanguages;

        private DatagenResolver(ResourceManager resourceManager, Providers providers) {
            this.resourceManager = resourceManager;
            this.providers = providers;

            this.itemInfos = new Object2ObjectOpenHashMap<>();
            for (Map.Entry<Item, ClientItem> entry : providers.getItemInfos().entrySet()) {
                this.itemInfos.put(entry.getKey().builtInRegistryHolder().key().identifier(), entry.getValue());
            }
            this.models = providers.getModels();

            // FabricLanguageProvider builds translation entries in a CompletableFuture, but that should have been finished by now
            foreignLanguages = new Object2ObjectOpenHashMap<>();
            providers.languages.ifPresent(languages -> languages.forEach(provider -> {
                foreignLanguages.compute(((FabricLanguageProviderAccessor) provider).getLanguageCode(), (_, existingKeys) -> {
                    if (existingKeys == null) {
                        return new Object2ObjectOpenHashMap<>(((LanguageProviderDataAccessor) provider).rainbow$getTranslationEntries());
                    }
                    existingKeys.putAll(((LanguageProviderDataAccessor) provider).rainbow$getTranslationEntries());
                    return existingKeys;
                });
            }));
        }

        @Override
        public Optional<ResolvedModel> getResolvedModel(Identifier identifier) {
            return resolvedModelCache.computeIfAbsent(identifier, _ -> Optional.ofNullable(models.get(identifier))
                    .<UnbakedModel>map(instance -> CuboidModel.fromStream(new StringReader(instance.get().toString())))
                    .or(() -> {
                        if (identifier.equals(ItemModelGenerator.GENERATED_ITEM_MODEL_ID)) {
                            return Optional.of(new ItemModelGenerator());
                        }
                        return Optional.empty();
                    })
                    .or(() -> RainbowIO.safeIO(() -> {
                        try (BufferedReader reader = resourceManager.openAsReader(identifier.withPrefix("models/").withSuffix(".json"))) {
                            return CuboidModel.fromStream(reader);
                        }
                    }))
                    .map(model -> new ResolvedModel() {
                        @Override
                        public UnbakedModel wrapped() {
                            return model;
                        }

                        @Override
                        public @Nullable ResolvedModel parent() {
                            return Optional.ofNullable(model.parent()).flatMap(parent -> getResolvedModel(parent)).orElse(null);
                        }

                        @Override
                        public String debugName() {
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
            return Optional.ofNullable(providers.equipmentInfos.get(key));
        }

        @Override
        public Optional<TextureResource> getTexture(@Nullable Identifier atlas, Identifier identifier) {
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

        @Override
        public Map<String, Map<String, String>> getForeignLanguages() {
            return foreignLanguages;
        }
    }
}
