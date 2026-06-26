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

package org.geysermc.rainbow.mapping.texture;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import org.geysermc.rainbow.RainbowIO;
import org.geysermc.rainbow.image.NativeImageUtil;
import org.geysermc.rainbow.mapping.AssetResolver;

import java.util.Optional;
import java.util.function.Supplier;

public class CustomTextureHolder extends TextureHolder {
    private final Supplier<NativeImage> supplier;

    public CustomTextureHolder(Identifier destination, Supplier<NativeImage> supplier) {
        super(destination);
        this.supplier = supplier;
    }

    @Override
    public Optional<TextureResource> load(AssetResolver assetResolver, ProblemReporter reporter) {
        NativeImage texture;
        try {
            texture = supplier.get();
        } catch (Exception exception) {
            reporter.report(() -> "unable to get texture for " + destination + "; please provide it manually");
            return Optional.empty();
        }
        return Optional.of(TextureResource.createNonAnimated(texture));
    }

    @Override
    protected boolean shouldReportMissingWhenAbsent() {
        return false;
    }
}
