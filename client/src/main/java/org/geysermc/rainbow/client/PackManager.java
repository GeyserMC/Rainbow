package org.geysermc.rainbow.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.SplashRenderer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringUtil;
import org.geysermc.rainbow.CodecUtil;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.client.mixin.SplashRendererAccessor;
import org.geysermc.rainbow.client.render.MinecraftGeometryRenderer;
import org.geysermc.rainbow.mapping.AssetCacheStats;
import org.geysermc.rainbow.pack.BedrockItem;
import org.geysermc.rainbow.pack.BedrockPack;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

public final class PackManager {
    private static final List<String> PACK_SUMMARY_COMMENTS = List.of("Use the custom item API v2 build!", "bugrock moment", "RORY",
            "use !!plshelp", "*message was deleted*", "welcome to the internet!", "beep beep. boop boop?", "FROG", "it is frog day", "it is cat day!",
            "eclipse will hear about this.", "you must now say the word 'frog' in the #general channel", "You Just Lost The Game", "you are now breathing manually",
            "you are now blinking manually", "you're eligible for a free hug token! <3", "don't mind me!", "hissss", "Gayser and Floodgayte, my favourite plugins.",
            "meow", "we'll be done here soon™", "got anything else to say?", "we're done now!", "this will be fixed by v6053", "expect it to be done within 180 business days!",
            "any colour you like", "someone tell Mojang about this", "you can't unbake baked models, so we'll store the unbaked models", "soon fully datagen ready",
            "packconverter when", "codecs ftw");
    private static final RandomSource RANDOM = RandomSource.create();

    private static final Path EXPORT_DIRECTORY = FabricLoader.getInstance().getGameDir().resolve(Rainbow.MOD_ID);
    private static final Path PACK_DIRECTORY = Path.of("pack");
    private static final Path MAPPINGS_FILE = Path.of("geyser_mappings.json");
    private static final Path PACK_ZIP_FILE = Path.of("pack.zip");
    private static final Path PACK_LANG_FOLDER = Path.of("lang");
    private static final Path REPORT_FILE = Path.of("report.txt");

    private final ClientPackSerializer packSerializer = new ClientPackSerializer();
    private Optional<BedrockPack> currentPack = Optional.empty();

    public void startPack(String name) throws IOException {
        if (currentPack.isPresent()) {
            throw new IllegalStateException("Already started a pack (" + currentPack.get().name() + ")");
        }
        packSerializer.prepare(Objects.requireNonNull(Minecraft.getInstance().level).registryAccess());

        Path packDirectory = createPackDirectory(name);
        BedrockPack pack = BedrockPack.builder(name, packDirectory.resolve(MAPPINGS_FILE), packDirectory.resolve(PACK_DIRECTORY), packSerializer,
                        new ClientAssetResolver(Minecraft.getInstance()))
                .withPackZipFile(packDirectory.resolve(PACK_ZIP_FILE))
                .withLanguageFolder(packDirectory.resolve(PACK_LANG_FOLDER))
                .withGeometryRenderer(MinecraftGeometryRenderer.INSTANCE)
                .reportSuccesses()
                .build();
        currentPack = Optional.of(pack);
    }

    public void run(Consumer<BedrockPack> consumer) {
        currentPack.ifPresent(consumer);
    }

    public void runOrElse(Consumer<BedrockPack> consumer, Runnable runnable) {
        currentPack.ifPresentOrElse(consumer, runnable);
    }

    public Optional<Path> getExportPath() {
        return currentPack.map(pack -> EXPORT_DIRECTORY.resolve(pack.name()));
    }

    public boolean finish(Runnable onFinish) {
        currentPack.ifPresent(pack -> {
            Path reportPath = EXPORT_DIRECTORY.resolve(pack.name()).resolve(REPORT_FILE);
            pack.save().thenRun(() -> {
                RainbowIO.safeIO(() -> Files.writeString(reportPath, createPackSummary(pack, packSerializer)));
                onFinish.run();
            });
        });
        boolean wasPresent = currentPack.isPresent();
        currentPack = Optional.empty();
        return wasPresent;
    }

    private static String createPackSummary(BedrockPack pack, ClientPackSerializer packSerializer) {
        String problems = ((ProblemReporter.Collector) pack.getReporter()).getTreeReport();
        if (StringUtil.isBlank(problems)) {
            problems = "Well that's odd... there's nothing here!";
        }

        Set<BedrockItem> bedrockItems = pack.getBedrockItems();
        long geometries = bedrockItems.stream().filter(item -> item.geometryContext().geometry().isPresent()).count();
        long animations = bedrockItems.stream().filter(item -> item.geometryContext().animation().isPresent()).count();
        AssetCacheStats cacheStats = pack.cacheStats();

        return """
#### READ THIS FIRST ####
What do I do now?

In this folder, you'll find 3 important files/folders along with this one:

- geyser_mappings.json: put this in the "custom_mappings" folder in Geyser's config folder. These are the generated item mappings.
- pack.zip: put this in the "packs" folder in Geyser's config folder. This is the generated bedrock resourcepack.
- lang: put all files in this folder in the "locales/overrides" folder in Geyser's config folder. These are the exported custom translation strings.
  - The folder can be empty or non-existent if no language files are found. This is usually not an issue!

Once you have taken those steps, restart your server. If everything went right, bedrock players should download
the generated pack and see your custom items.

IF YOU EXPERIENCE ANY ISSUES, please go to our Discord (https://discord.gg/geysermc) for support.
Use the #custom-resource-packs channel, and make sure to include this report file.

You can also open an issue report over at our issue tracker (https://github.com/GeyserMC/Rainbow/issues).
Again, be sure to include this report file, and please make sure your issue is not already reported!
If it is, you can help out by adding details to the existing report.

Below, you'll find some statistics about the generated pack, and a mapping report,
which will list any models converted, and any problems that occurred during mapping.
#########################

-- PACK GENERATION REPORT --
// %s

Generated pack: %s
Mappings written: %d

Item texture atlas size: %d
Geometries exported: %d
Animations exported: %d

JSON-files written: %d
Textures exported: %d

-- ASSET CACHE STATS --

Geometry cache: %d written, %d cache hits
Texture cache: %d written, %d cache hits

-- MAPPING TREE REPORT --
%s
""".formatted(randomSummaryComment(), pack.name(), pack.getMappings(), pack.getItemTextureAtlasSize(),
                geometries, animations, packSerializer.jsonExported(), packSerializer.texturesExported(),
                cacheStats.geometry().size(), cacheStats.geometry().hits(),
                cacheStats.texture().size(), cacheStats.texture().hits(), problems);
    }

    private static String randomSummaryComment() {
        if (RANDOM.nextDouble() < 0.5) {
            SplashRenderer splash = Minecraft.getInstance().getSplashManager().getSplash();
            if (splash == null) {
                return "Undefined Undefined :(";
            }
            return ((SplashRendererAccessor) splash).getSplash().getString();
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
}
