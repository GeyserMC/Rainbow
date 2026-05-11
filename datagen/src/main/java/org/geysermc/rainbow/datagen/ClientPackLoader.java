package org.geysermc.rainbow.datagen;

import joptsimple.ArgumentAcceptingOptionSpec;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.resources.ClientPackSource;
import net.minecraft.client.resources.IndexedAssetSource;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.resources.CloseableResourceManager;
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.world.level.validation.DirectoryValidator;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;

class ClientPackLoader {

    static ClientPackSource loadClientPackSource() {
        OptionParser parser = new OptionParser();
        OptionSpec<File> assetsDirSpec = parser.accepts("assetsDir").withRequiredArg().ofType(File.class);
        OptionSpec<String> assetIndexSpec = parser.accepts("assetIndex").withRequiredArg();
        parser.allowsUnrecognizedOptions();
        OptionSet parsed = parser.parse(FabricLoader.getInstance().getLaunchArguments(false));

        return new ClientPackSource(getExternalAssetSource(parseArgument(parsed, assetIndexSpec), parseArgument(parsed, assetsDirSpec)),
                new DirectoryValidator(_ -> false));
    }

    static CompletableFuture<CloseableResourceManager> openClientResources() {
        return CompletableFuture.supplyAsync(() -> {
            ClientPackSource packSource = loadClientPackSource();
            PackRepository repository = new PackRepository(packSource);
            repository.reload();
            return new MultiPackResourceManager(PackType.CLIENT_RESOURCES, repository.getAvailablePacks().stream()
                    .map(Pack::open)
                    .toList());
        });
    }

    private static Path getExternalAssetSource(@Nullable String assetIndex, File assetDirectory) {
        return assetIndex == null ? assetDirectory.toPath() : IndexedAssetSource.createIndexFs(assetDirectory.toPath(), assetIndex);
    }

    // From Mojang's client/Main.java
    private static <T> T parseArgument(OptionSet set, OptionSpec<T> spec) {
        try {
            return set.valueOf(spec);
        } catch (Throwable exception) {
            if (spec instanceof ArgumentAcceptingOptionSpec<T> argumentAccepting) {
                List<T> list = argumentAccepting.defaultValues();
                if (!list.isEmpty()) {
                    return list.getFirst();
                }
            }

            throw exception;
        }
    }
}
