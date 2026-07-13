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

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import org.geysermc.rainbow.CodecUtil;
import org.geysermc.rainbow.PackConstants;

import java.util.List;
import java.util.UUID;

// TODO metadata
public record PackManifest(Header header, List<Module> modules) {

    public static final Codec<PackManifest> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CodecUtil.unitVerifyCodec(Codec.INT, "format_version", 2),
                    Header.CODEC.fieldOf("header").forGetter(PackManifest::header),
                    Module.CODEC.listOf().fieldOf("modules").forGetter(PackManifest::modules)
            ).apply(instance, (formatVersion, header, modules) -> new PackManifest(header, modules))
    );

    public PackManifest increment() {
        return new PackManifest(header.increment(), modules.stream().map(Module::increment).toList());
    }

    public record Header(String name, String description, UUID uuid, BedrockVersion version, BedrockVersion minEngineVersion) {
        public static final MapCodec<Header> MAP_CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.STRING.fieldOf("name").forGetter(Header::name),
                        Codec.STRING.fieldOf("description").forGetter(Header::description),
                        UUIDUtil.STRING_CODEC.fieldOf("uuid").forGetter(Header::uuid),
                        BedrockVersion.CODEC.fieldOf("version").forGetter(Header::version),
                        BedrockVersion.CODEC.fieldOf("min_engine_version").forGetter(Header::minEngineVersion)
                ).apply(instance, Header::new)
        );
        public static final Codec<Header> CODEC = MAP_CODEC.codec();

        public Header increment() {
            return new Header(name, description, uuid, version.increment(), minEngineVersion);
        }
    }

    public record Module(String name, String description, UUID uuid, BedrockVersion version) {
        public static final Codec<Module> CODEC = RecordCodecBuilder.create(instance ->
                instance.group(
                        CodecUtil.unitVerifyCodec(Codec.STRING, "type", "resources"),
                        Codec.STRING.fieldOf("name").forGetter(Module::name),
                        Codec.STRING.fieldOf("description").forGetter(Module::description),
                        UUIDUtil.STRING_CODEC.fieldOf("uuid").forGetter(Module::uuid),
                        BedrockVersion.CODEC.fieldOf("version").forGetter(Module::version)
                ).apply(instance, (type, name, description, uuid, version) -> new Module(name, description, uuid, version))
        );

        public Module increment() {
            return new Module(name, description, uuid, version.increment());
        }
    }

    public static PackManifest create(String name, String description, UUID headerUUID, UUID moduleUUID, BedrockVersion version) {
        return new PackManifest(new PackManifest.Header(name, description, headerUUID, version, PackConstants.ENGINE_VERSION),
                List.of(new PackManifest.Module(name, description, moduleUUID, version)));
    }
}
