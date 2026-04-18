package org.geysermc.rainbow.pack;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.CustomModelData;
import org.geysermc.rainbow.CodecUtil;
import org.geysermc.rainbow.PackConstants;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.mapping.AssetResolver;
import org.geysermc.rainbow.mapping.BedrockItemMapper;
import org.geysermc.rainbow.mapping.PackContext;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;
import org.geysermc.rainbow.mapping.geometry.GeometryRenderer;
import org.geysermc.rainbow.definition.GeyserMappings;
import org.jspecify.annotations.Nullable;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.UnaryOperator;

public class BedrockPack implements PackSerializer.Serializable {
    private final String name;
    private final Optional<PackManifest> manifest;
    private final PackPaths paths;
    private final PackSerializer serializer;

    private final BedrockTextures.Builder itemTextures = BedrockTextures.builder();
    private final Set<BedrockItem> bedrockItems = new HashSet<>();
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
        this.context = new PackContext(new GeyserMappings(), paths, item -> {
            itemTextures.withItemTexture(item);
            bedrockItems.add(item);
        }, assetResolver, geometryRenderer, reportSuccesses);
        this.reporter = reporter;
    }

    public String name() {
        return name;
    }

    public MappingResult map(ItemStackTemplate stack) {
        AtomicBoolean problems = new AtomicBoolean();
        ProblemReporter mapReporter = new ProblemReporter() {

            @Override
            public ProblemReporter forChild(PathElement child) {
                return reporter.forChild(child);
            }

            @Override
            public void report(Problem problem) {
                problems.set(true);
                reporter.report(problem);
            }
        };

        if (!stack.components().split().added().has(DataComponents.ITEM_MODEL)) {
            CustomModelData customModelData = stack.components().split().added().get(DataComponents.CUSTOM_MODEL_DATA);
            Float firstNumber;
            if (customModelData == null || (firstNumber = customModelData.getFloat(0)) == null
                    || !customModelDataMapped.add(Pair.of(stack.item(), firstNumber.intValue()))) {
                return MappingResult.NONE_MAPPED;
            }

            BedrockItemMapper.tryMapStack(stack, firstNumber.intValue(), mapReporter, context);
        } else {
            Identifier model = stack.components().split().added().get(DataComponents.ITEM_MODEL);
            assert model != null;
            if (!modelsMapped.add(model)) {
                return MappingResult.NONE_MAPPED;
            }

            BedrockItemMapper.tryMapStack(stack, model, mapReporter, context);
        }

        return problems.get() ? MappingResult.PROBLEMS_OCCURRED : MappingResult.MAPPED_SUCCESSFULLY;
    }

    public MappingResult map(Holder<Item> item, DataComponentPatch patch) {
        return map(new ItemStackTemplate(item, 1, patch));
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
        return PackSerializer.Serializable.wrapCodec(GeyserMappings.CODEC, context.mappings(), PackPaths::mappings)
                .with(PackSerializer.Serializable.wrapOptionalCodec(PackManifest.CODEC, manifest, PackPaths::manifest))
                .with(PackSerializer.Serializable.wrapCodec(BedrockTextureAtlas.ITEM_ATLAS_CODEC, BedrockTextureAtlas.itemAtlas(name, itemTextures), PackPaths::itemAtlas))
                .with(bedrockItems)
                .with(paths.languageOutput().map(languageFolder -> context -> LanguageUtil.saveLanguages(context, languageFolder)))
                .save(serializingContext);
    }

    public int getMappings() {
        return context.mappings().size();
    }

    public Set<BedrockItem> getBedrockItems() {
        return Set.copyOf(bedrockItems);
    }

    public int getItemTextureAtlasSize() {
        return itemTextures.build().size();
    }

    public ProblemReporter getReporter() {
        return reporter;
    }

    private PackSerializingContext createSerializingContext() {
        return new PackSerializingContext(context.assetResolver(), serializer, paths, reporter);
    }

    public static Builder builder(String name, Path mappingsPath, Path packRootPath, PackSerializer packSerializer, AssetResolver assetResolver) {
        return new Builder(name, mappingsPath, packRootPath, packSerializer, assetResolver);
    }

    public static class Builder {
        private static final Path ATTACHABLES_DIRECTORY = Path.of("attachables");
        private static final Path GEOMETRY_DIRECTORY = Path.of("models/entity");
        private static final Path ANIMATION_DIRECTORY = Path.of("animations");
        private static final Path RENDER_CONTROLLERS_DIRECTORY = Path.of("render_controllers");

        private static final Path MANIFEST_FILE = Path.of("manifest.json");
        private static final Path ITEM_ATLAS_FILE = Path.of("textures/item_texture.json");

        private final String name;
        private final Path mappingsPath;
        private final Path packRootPath;
        private final PackSerializer packSerializer;
        private final AssetResolver assetResolver;
        private @Nullable PackManifest manifest;
        private UnaryOperator<Path> attachablesPath = resolve(ATTACHABLES_DIRECTORY);
        private UnaryOperator<Path> geometryPath = resolve(GEOMETRY_DIRECTORY);
        private UnaryOperator<Path> animationPath = resolve(ANIMATION_DIRECTORY);
        private UnaryOperator<Path> renderControllersPath = resolve(RENDER_CONTROLLERS_DIRECTORY);
        private UnaryOperator<Path> manifestPath = resolve(MANIFEST_FILE);
        private UnaryOperator<Path> itemAtlasPath = resolve(ITEM_ATLAS_FILE);
        private @Nullable Path packZipFile = null;
        private @Nullable Path languageFolder = null;
        private @Nullable GeometryRenderer geometryRenderer = null;
        private Function<ProblemReporter.PathElement, ProblemReporter> reporter;
        private boolean reportSuccesses = false;

        public Builder(String name, Path mappingsPath, Path packRootPath, PackSerializer packSerializer, AssetResolver assetResolver) {
            this.name = name;
            this.mappingsPath = mappingsPath;
            this.packRootPath = packRootPath;
            this.reporter = ProblemReporter.Collector::new;
            this.packSerializer = packSerializer;
            this.assetResolver = assetResolver;
            manifest = defaultManifest(name);
        }

        public Builder withManifest(@Nullable PackManifest manifest) {
            this.manifest = manifest;
            return this;
        }

        public Builder withAttachablesPath(Path absolute) {
            return withAttachablesPath(_ -> absolute);
        }

        public Builder withAttachablesPath(UnaryOperator<Path> path) {
            attachablesPath = path;
            return this;
        }

        public Builder withGeometryPath(Path absolute) {
            return withGeometryPath(_ -> absolute);
        }

        public Builder withGeometryPath(UnaryOperator<Path> path) {
            geometryPath = path;
            return this;
        }

        public Builder withAnimationPath(Path absolute) {
            return withAnimationPath(_ -> absolute);
        }

        public Builder withAnimationPath(UnaryOperator<Path> path) {
            animationPath = path;
            return this;
        }

        public Builder withRenderControllersPath(Path absolute) {
            return withRenderControllersPath(_ -> absolute);
        }

        public Builder withRenderControllersPath(UnaryOperator<Path> path) {
            renderControllersPath = path;
            return this;
        }

        public Builder withManifestPath(Path absolute) {
            return withManifestPath(_ -> absolute);
        }

        public Builder withManifestPath(UnaryOperator<Path> path) {
            manifestPath = path;
            return this;
        }

        public Builder withItemAtlasPath(Path absolute) {
            return withItemAtlasPath(_ -> absolute);
        }

        public Builder withItemAtlasPath(UnaryOperator<Path> path) {
            itemAtlasPath = path;
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
            PackPaths paths = new PackPaths(mappingsPath, packRootPath, attachablesPath.apply(packRootPath),
                    geometryPath.apply(packRootPath), animationPath.apply(packRootPath), renderControllersPath.apply(packRootPath),
                    manifestPath.apply(packRootPath), itemAtlasPath.apply(packRootPath),
                    Optional.ofNullable(packZipFile), Optional.ofNullable(languageFolder));
            return new BedrockPack(name, Optional.ofNullable(manifest), paths, packSerializer, assetResolver, Optional.ofNullable(geometryRenderer),
                    reporter.apply(() -> "Bedrock pack " + name + " "), reportSuccesses);
        }

        private static UnaryOperator<Path> resolve(Path child) {
            return root -> root.resolve(child);
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
}
