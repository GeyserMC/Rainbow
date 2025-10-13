package org.geysermc.rainbow.pack;

import com.mojang.serialization.JsonOps;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import org.apache.commons.io.IOUtils;
import org.geysermc.rainbow.CodecUtil;
import org.geysermc.rainbow.PackConstants;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.mapping.BedrockItemMapper;
import org.geysermc.rainbow.mapping.PackContext;
import org.geysermc.rainbow.mapping.geyser.GeyserMappings;
import org.geysermc.rainbow.mixin.SplashRendererAccessor;
import org.geysermc.rainbow.packconverter.PackConverterContext;
import org.jetbrains.annotations.NotNull;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public class BedrockPack {
    private static final List<String> PACK_SUMMARY_COMMENTS = List.of("Use the custom item API v2 build!", "bugrock moment", "RORY",
            "use !!plshelp", "rm -rf --no-preserve-root /*", "welcome to the internet!", "beep beep. boop boop?", "FROG", "it is frog day", "it is cat day!",
            "eclipse will hear about this.", "you must now say the word 'frog' in the #general channel", "You Just Lost The Game", "you are now breathing manually",
            "you are now blinking manually", "you're eligible for a free hug token! <3", "don't mind me!", "hissss", "Gayser and Floodgayte, my favourite plugins.",
            "meow", "we'll be done here soon™", "got anything else to say?", "we're done now!", "this will be fixed by v6053", "expect it to be done within 180 business days!",
            "any colour you like", "someone tell Mojang about this", "you can't unbake baked models, so we'll store the unbaked models", "soon fully datagen ready",
            "packconverter when", "codecs ftw");
    private static final RandomSource RANDOM = RandomSource.create();

    private static final Path EXPORT_DIRECTORY = FabricLoader.getInstance().getGameDir().resolve(Rainbow.MOD_ID);
    private static final Path PACK_DIRECTORY = Path.of("pack");
    private static final Path ATTACHABLES_DIRECTORY = Path.of("attachables");
    private static final Path GEOMETRY_DIRECTORY = Path.of("models/entity");
    private static final Path ANIMATION_DIRECTORY = Path.of("animations");

    private static final Path MAPPINGS_FILE = Path.of("geyser_mappings.json");
    private static final Path MANIFEST_FILE = Path.of("manifest.json");
    private static final Path ITEM_ATLAS_FILE = Path.of("textures/item_texture.json");

    private static final Path PACK_ZIP_FILE = Path.of("pack.zip");
    private static final Path REPORT_FILE = Path.of("report.txt");

    private final String name;
    private final Path exportPath;
    private final Path packPath;
    private final PackManifest manifest;
    private final GeyserMappings mappings;
    private final BedrockTextures.Builder itemTextures;

    private final Set<BedrockItem> bedrockItems = new HashSet<>();
    private final Set<ResourceLocation> texturesToExport = new HashSet<>();
    private final Set<ResourceLocation> modelsMapped = new HashSet<>();
    private final IntSet customModelDataMapped = new IntOpenHashSet();

    private final PackConverterContext packConverterContext = PackConverterContext.create(this);

    private final ProblemReporter.Collector reporter;

    public BedrockPack(String name) throws IOException {
        this.name = name;

        // Not reading existing item mappings/texture atlas for now since that doesn't work all that well yet
        exportPath = createPackDirectory(name);
        packPath = exportPath.resolve(PACK_DIRECTORY);
        //mappings = CodecUtil.readOrCompute(GeyserMappings.CODEC, exportPath.resolve(MAPPINGS_FILE), GeyserMappings::new);
        mappings = new GeyserMappings();
        manifest = CodecUtil.readOrCompute(PackManifest.CODEC, packPath.resolve(MANIFEST_FILE), () -> defaultManifest(name)).increment();
        /*itemTextures = CodecUtil.readOrCompute(BedrockTextureAtlas.ITEM_ATLAS_CODEC, packPath.resolve(ITEM_ATLAS_FILE),
                () -> BedrockTextureAtlas.itemAtlas(name, BedrockTextures.builder())).textures().toBuilder();*/
        itemTextures = BedrockTextures.builder();

        reporter = new ProblemReporter.Collector(() -> "Bedrock pack " + name + " ");
    }

    public String name() {
        return name;
    }

    public MappingResult map(ItemStack stack) {
        if (stack.isEmpty()) {
            return MappingResult.NONE_MAPPED;
        }

        AtomicBoolean problems = new AtomicBoolean();
        ProblemReporter mapReporter = new ProblemReporter() {

            @Override
            public @NotNull ProblemReporter forChild(PathElement child) {
                return reporter.forChild(child);
            }

            @Override
            public void report(Problem problem) {
                problems.set(true);
                reporter.report(problem);
            }
        };
        PackContext context = new PackContext(mappings, packPath, item -> {
            itemTextures.withItemTexture(item);
            if (item.exportTexture()) {
                texturesToExport.add(item.texture());
            }
            bedrockItems.add(item);
        }, texturesToExport::add);

        Optional<? extends ResourceLocation> patchedModel = stack.getComponentsPatch().get(DataComponents.ITEM_MODEL);
        //noinspection OptionalAssignedToNull - annoying Mojang
        if (patchedModel == null || patchedModel.isEmpty()) {
            CustomModelData customModelData = stack.get(DataComponents.CUSTOM_MODEL_DATA);
            Float firstNumber;
            if (customModelData == null || (firstNumber = customModelData.getFloat(0)) == null
                    || !customModelDataMapped.add((firstNumber.intValue()))) {
                return MappingResult.NONE_MAPPED;
            }

            BedrockItemMapper.tryMapStack(stack, firstNumber.intValue(), mapReporter, context);
        } else {
            ResourceLocation model = patchedModel.get();
            if (!modelsMapped.add(model)) {
                return MappingResult.NONE_MAPPED;
            }

            BedrockItemMapper.tryMapStack(stack, model, mapReporter, context);
        }

        return problems.get() ? MappingResult.PROBLEMS_OCCURRED : MappingResult.MAPPED_SUCCESSFULLY;
    }

    public boolean save() {
        boolean success = true;

        try {
            CodecUtil.trySaveJson(GeyserMappings.CODEC, mappings, exportPath.resolve(MAPPINGS_FILE), RegistryOps.create(JsonOps.INSTANCE, Minecraft.getInstance().level.registryAccess()));
            CodecUtil.trySaveJson(PackManifest.CODEC, manifest, packPath.resolve(MANIFEST_FILE));
            CodecUtil.trySaveJson(BedrockTextureAtlas.CODEC, BedrockTextureAtlas.itemAtlas(name, itemTextures), packPath.resolve(ITEM_ATLAS_FILE));
        } catch (IOException | NullPointerException exception) {
            reporter.forChild(() -> "saving Geyser mappings, pack manifest, and texture atlas ").report(() -> "failed to save to pack: " + exception);
            success = false;
        }
        for (BedrockItem item : bedrockItems) {
            try {
                item.save(packPath.resolve(ATTACHABLES_DIRECTORY), packPath.resolve(GEOMETRY_DIRECTORY), packPath.resolve(ANIMATION_DIRECTORY));
            } catch (IOException exception) {
                reporter.forChild(() -> "files for bedrock item " + item.identifier() + " ").report(() -> "failed to save to pack: " + exception);
                success = false;
            }
        }

        for (ResourceLocation texture : texturesToExport) {
            texture = texture.withPath(path -> "textures/" + path + ".png");
            try (InputStream inputTexture = Minecraft.getInstance().getResourceManager().open(texture)) {
                Path texturePath = packPath.resolve(texture.getPath());
                CodecUtil.ensureDirectoryExists(texturePath.getParent());
                try (OutputStream outputTexture = new FileOutputStream(texturePath.toFile())) {
                    IOUtils.copy(inputTexture, outputTexture);
                }
            } catch (IOException exception) {
                ResourceLocation finalTexture = texture;
                reporter.forChild(() -> "texture " + finalTexture + " ").report(() -> "failed to save to pack: " + exception);
                success = false;
            }
        }

        try {
            CodecUtil.tryZipDirectory(packPath, exportPath.resolve(PACK_ZIP_FILE));
        } catch (IOException exception) {
            success = false;
        }

        try {
            Files.writeString(exportPath.resolve(REPORT_FILE), createPackSummary());
        } catch (IOException exception) {
            // TODO log
        }
        return success;
    }

    public Path getExportPath() {
        return exportPath;
    }

    private String createPackSummary() {
        String problems = reporter.getTreeReport();
        if (StringUtil.isBlank(problems)) {
            problems = "Well that's odd... there's nothing here!";
        }

        long attachables = bedrockItems.stream().filter(item -> item.attachable().isPresent()).count();
        long geometries = bedrockItems.stream().filter(item -> item.geometry().isPresent()).count();
        long animations = bedrockItems.stream().filter(item -> item.animation().isPresent()).count();

        return """
-- PACK GENERATION REPORT --
// %s

Generated pack: %s
Mappings written: %d
Item texture atlas size: %d
Attachables tried to export: %d
Geometry files tried to export: %d
Animations tried to export: %d
Textures tried to export: %d

-- MAPPING TREE REPORT --
%s
""".formatted(randomSummaryComment(), name, mappings.size(), itemTextures.build().size(),
                attachables, geometries, animations, texturesToExport.size(), problems);
    }

    private static String randomSummaryComment() {
        if (RANDOM.nextDouble() < 0.6) {
            SplashRenderer splash = Minecraft.getInstance().getSplashManager().getSplash();
            if (splash == null) {
                return "Undefined Undefined :(";
            }
            return ((SplashRendererAccessor) splash).getSplash();
        }
        return randomBuiltinSummaryComment();
    }

    private static String randomBuiltinSummaryComment() {
        return PACK_SUMMARY_COMMENTS.get(RANDOM.nextInt(PACK_SUMMARY_COMMENTS.size()));
    }

    private static Path createPackDirectory(String name) throws IOException {
        Path path = EXPORT_DIRECTORY.resolve(name);
        CodecUtil.ensureDirectoryExists(path);
        return path;
    }

    private static PackManifest defaultManifest(String name) {
        return new PackManifest(new PackManifest.Header(name, PackConstants.DEFAULT_PACK_DESCRIPTION, UUID.randomUUID(), BedrockVersion.of(0), PackConstants.ENGINE_VERSION),
                List.of(new PackManifest.Module(name, PackConstants.DEFAULT_PACK_DESCRIPTION, UUID.randomUUID(), BedrockVersion.of(0))));
    }

    public enum MappingResult {
        NONE_MAPPED,
        MAPPED_SUCCESSFULLY,
        PROBLEMS_OCCURRED
    }
}
