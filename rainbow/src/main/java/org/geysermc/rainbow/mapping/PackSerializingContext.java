package org.geysermc.rainbow.mapping;

import net.minecraft.util.ProblemReporter;
import org.geysermc.rainbow.pack.PackPaths;

public record PackSerializingContext(AssetResolver assetResolver, PackSerializer serializer, PackPaths paths, ProblemReporter reporter) {}
