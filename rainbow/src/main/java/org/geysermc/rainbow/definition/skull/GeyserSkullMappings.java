package org.geysermc.rainbow.definition.skull;

import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.component.ResolvableProfile;
import org.geysermc.rainbow.CodecUtil;
import org.geysermc.rainbow.mixin.ResolvableProfileAccessor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GeyserSkullMappings {
    public static final Codec<GeyserSkullMappings> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CodecUtil.unitVerifyCodec(Codec.INT, "format_version", 1),
                    Codec.unboundedMap(SkullTextureType.CODEC, ExtraCodecs.NON_EMPTY_STRING.listOf()).fieldOf("skulls").forGetter(GeyserSkullMappings::mappings)
            ).apply(instance, (_, mappings) -> new GeyserSkullMappings(mappings))
    );

    private final Map<SkullTextureType, List<String>> mappings = new EnumMap<>(SkullTextureType.class);

    public GeyserSkullMappings() {}

    private GeyserSkullMappings(Map<SkullTextureType, List<String>> mappings) {
        this.mappings.putAll(mappings);
    }

    public boolean withProfile(ResolvableProfile profile) {
        Either<GameProfile, ResolvableProfile.Partial> unpacked = ((ResolvableProfileAccessor) (Object) profile).invokeUnpack();
        return unpacked.left()
                .map(fullProfile -> {
                    Property texture = getTexture(fullProfile);
                    if (texture != null) {
                        return withTexture(SkullTextureType.PROFILE, texture.value());
                    }
                    return withTexture(SkullTextureType.UUID, fullProfile.id().toString());
                })
                .or(() -> unpacked.right()
                        .map(partialProfile -> {
                            Property texture = getTexture(partialProfile.properties());
                            if (texture != null) {
                                return withTexture(SkullTextureType.PROFILE, texture.value());
                            } else {
                                return partialProfile.id()
                                        .map(uuid -> withTexture(SkullTextureType.UUID, uuid.toString()))
                                        .or(() -> partialProfile.name().map(username -> withTexture(SkullTextureType.USERNAME, username)))
                                        .orElse(false);
                            }
                        }))
                .orElseThrow();
    }

    private boolean withTexture(SkullTextureType type, String texture) {
        List<String> textures = mappings.get(type);
        if (textures == null) {
            textures = new ArrayList<>();
            mappings.put(type, textures);
        } else if (textures.contains(texture)) {
            return false;
        }
        textures.add(texture);
        return true;
    }

    public Map<SkullTextureType, List<String>> mappings() {
        return Collections.unmodifiableMap(mappings);
    }

    public int size(SkullTextureType type) {
        return Objects.requireNonNullElseGet(mappings.get(type), List::of).size();
    }

    private static @Nullable Property getTexture(GameProfile profile) {
        return getTexture(profile.properties());
    }

    private static @Nullable Property getTexture(PropertyMap map) {
        return Iterables.getFirst(map.get("textures"), null);
    }

    public enum SkullTextureType implements StringRepresentable {
        USERNAME("username"),
        UUID("uuid"),
        PROFILE("profile"),
        SKIN_HASH("skin_hash");

        public static final Codec<SkullTextureType> CODEC = StringRepresentable.fromEnum(SkullTextureType::values);

        private final String name;

        SkullTextureType(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
