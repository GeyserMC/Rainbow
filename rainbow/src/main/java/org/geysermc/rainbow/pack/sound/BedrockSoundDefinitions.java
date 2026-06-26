package org.geysermc.rainbow.pack.sound;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundEventRegistration;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;
import net.minecraft.util.valueproviders.SampledFloat;
import org.geysermc.rainbow.CodecUtil;
import org.geysermc.rainbow.Rainbow;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;
import org.geysermc.rainbow.pack.BedrockVersion;
import org.geysermc.rainbow.pack.PackPaths;
import org.geysermc.rainbow.stats.PackStats;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public record BedrockSoundDefinitions(Map<Identifier, SoundEventRegistration> definitions) implements PackSerializer.Serializable, PackStats.Holder {
    public static final BedrockVersion FORMAT_VERSION = BedrockVersion.of(1, 20, 20);
    public static final Codec<Sound> BEDROCK_SOUND_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.fieldOf("name").forGetter(sound -> "sounds/" + sound.getLocation().getPath()),
                    Codec.BOOL.optionalFieldOf("stream", false).forGetter(Sound::shouldStream),
                    Codec.BOOL.optionalFieldOf("is3D", false).forGetter(_ -> true),
                    createOptionalStaticSampledFloatCodec("volume", 1.0F).forGetter(Sound::getVolume),
                    createOptionalStaticSampledFloatCodec("pitch", 1.0F).forGetter(Sound::getPitch),
                    Codec.INT.optionalFieldOf("weight", 1).forGetter(Sound::getWeight)
            ).apply(instance, (name, stream, _, volume, pitch, weight) ->
                    new Sound(Identifier.withDefaultNamespace(name.replaceFirst("^sounds/", "")), volume, pitch, weight,
                            Sound.Type.FILE, stream, false, 16))
    );
    public static final Codec<SoundEventRegistration> BEDROCK_REGISTRATION_CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    Codec.STRING.optionalFieldOf("category", "sound").forGetter(_ -> "sound"),
                    Codec.FLOAT.optionalFieldOf("min_distance", 0.0F).forGetter(registration -> Float.valueOf(registration.getSounds().stream().map(Sound::getAttenuationDistance).findFirst().orElse(0))),
                    Codec.FLOAT.optionalFieldOf("max_distance", 0.0F).forGetter(registration -> Float.valueOf(registration.getSounds().stream().map(Sound::getAttenuationDistance).findFirst().orElse(0)) * 2.0F),
                    BEDROCK_SOUND_CODEC.listOf().fieldOf("sounds").forGetter(SoundEventRegistration::getSounds),
                    Codec.STRING.optionalFieldOf("subtitle", null).forGetter(SoundEventRegistration::getSubtitle)
            ).apply(instance, (_, _, _, sounds, subtitle) -> new SoundEventRegistration(sounds, false, subtitle))
    );
    public static final Codec<BedrockSoundDefinitions> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    CodecUtil.unitVerifyCodec(BedrockVersion.STRING_CODEC, "format_version", FORMAT_VERSION),
                    Codec.compoundList(Identifier.CODEC, BEDROCK_REGISTRATION_CODEC)
                            .xmap(pairs -> pairs.stream().collect(Pair.toMap()),
                                    registrations -> registrations.entrySet().stream().map(entry -> Pair.of(entry.getKey(), entry.getValue())).toList())
                            .fieldOf("sound_definitions").forGetter(BedrockSoundDefinitions::definitions)
            ).apply(instance, (_, definitions) -> new BedrockSoundDefinitions(definitions))
    );

    public int size() {
        return definitions.size();
    }

    @Override
    public int stat() {
        return size();
    }

    public Stream<Sound> flatten() {
        return definitions.values().stream()
                .flatMap(registration -> registration.getSounds().stream());
    }

    @Override
    public CompletableFuture<?> save(PackSerializingContext context) {
        return PackSerializer.Serializable.wrapCodec(CODEC, this, PackPaths::soundDefinitions)
                .with(flatten()
                        .map(BedrockSoundDefinitions::serializableSound)
                        .toList())
                .save(context);
    }

    private static PackSerializer.Serializable serializableSound(Sound sound) {
        return context -> context.assetResolver().getSound(sound.getLocation())
                .map(resource -> context.serializer().saveResource(resource, context.paths().sound(sound)))
                .orElseGet(PackSerializer::noop);
    }

    private static MapCodec<SampledFloat> createOptionalStaticSampledFloatCodec(String field, float defaultValue) {
        return Codec.FLOAT.optionalFieldOf(field, defaultValue).xmap(ConstantFloat::of, f -> f.sample(RandomSource.create(0L)));
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private final Map<Identifier, SoundEventRegistration> definitions = new Object2ObjectOpenHashMap<>();

        public Builder withRegistrations(String namespace, Map<String, SoundEventRegistration> registrations) {
            for (Map.Entry<String, SoundEventRegistration> registration : registrations.entrySet()) {
                definitions.put(Identifier.fromNamespaceAndPath(namespace, registration.getKey()), registration.getValue());
            }
            return this;
        }

        public Builder withRegistrations(Map<String, Map<String, SoundEventRegistration>> registrations) {
            for (String namespace : registrations.keySet()) {
                if (Rainbow.isVanilla(namespace)) {
                    continue;
                }

                withRegistrations(namespace, registrations.get(namespace));
            }
            return this;
        }

        public BedrockSoundDefinitions build() {
            return new BedrockSoundDefinitions(Map.copyOf(definitions));
        }
    }
}
