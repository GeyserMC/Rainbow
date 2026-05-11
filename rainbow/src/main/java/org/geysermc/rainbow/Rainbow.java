package org.geysermc.rainbow;

import com.mojang.logging.LogUtils;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public class Rainbow {

    public static final String MOD_ID = "rainbow";
    public static final String MOD_NAME = "Rainbow";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static Identifier getModdedIdentifier(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    public static String bedrockSafeIdentifier(Identifier identifier) {
        return identifier.toString().replace(':', '.').replace('/', '_');
    }

    public static Identifier decorateIdentifier(Identifier identifier, String type, String extension) {
        return identifier.withPath(path -> type + "/" + path + "." + extension);
    }

    public static Identifier decorateTextureIdentifier(Identifier identifier) {
        return decorateIdentifier(identifier, "textures", "png");
    }

    public static Identifier getModelIdentifier(ResolvedModel model) {
        // debugName() returns the resource location of the model as a string
        return Identifier.parse(model.debugName());
    }
}
