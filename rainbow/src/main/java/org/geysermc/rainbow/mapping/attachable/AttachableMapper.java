package org.geysermc.rainbow.mapping.attachable;

import com.mojang.datafixers.util.Pair;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.equipment.Equippable;
import org.geysermc.rainbow.mapping.AssetResolver;
import org.geysermc.rainbow.mapping.geometry.BedrockGeometryContext;
import org.geysermc.rainbow.mapping.texture.TextureHolder;
import org.geysermc.rainbow.pack.attachable.BedrockAttachable;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class AttachableMapper {

    public static AttachableCreator mapItem(AssetResolver assetResolver, BedrockGeometryContext geometryContext, DataComponentPatch components) {
        // Crazy optional statement
        // Unfortunately we can't have both equippables and custom models, so we prefer the latter :(
        return (bedrockIdentifier, textureConsumer) -> geometryContext.geometry()
                .map(geometry -> BedrockAttachable.geometry(bedrockIdentifier, geometry))
                .or(() -> Optional.ofNullable(components.get(DataComponents.EQUIPPABLE))
                        .flatMap(optional -> (Optional<Equippable>) optional)
                        .flatMap(equippable -> equippable.assetId().flatMap(assetResolver::getEquipmentInfo).map(info -> Pair.of(equippable.slot(), info)))
                        .filter(assetInfo -> assetInfo.getSecond() != EquipmentAssetManager.MISSING)
                        .map(assetInfo -> assetInfo
                                .mapSecond(info -> info.getLayers(getLayer(assetInfo.getFirst()))))
                        .filter(assetInfo -> !assetInfo.getSecond().isEmpty())
                        .map(assetInfo -> {
                            Identifier equipmentTexture = getTexture(assetInfo.getSecond(), getLayer(assetInfo.getFirst()));
                            textureConsumer.accept(TextureHolder.createBuiltIn(null, equipmentTexture));
                            return BedrockAttachable.equipment(bedrockIdentifier, assetInfo.getFirst(), equipmentTexture.getPath());
                        }))
                .map(attachable -> {
                    geometryContext.animation().ifPresent(animation -> {
                        attachable.withAnimation("first_person", animation.firstPerson());
                        attachable.withAnimation("third_person", animation.thirdPerson());
                        attachable.withAnimation("head", animation.head());
                        attachable.withScript("animate", "first_person", "context.is_first_person == 1.0");
                        attachable.withScript("animate", "third_person", "context.is_first_person == 0.0 && context.item_slot != 'head'");
                        attachable.withScript("animate", "head", "context.is_first_person == 0.0 && context.item_slot == 'head'");
                    });
                    return attachable.build();
                });
    }

    private static EquipmentClientInfo.LayerType getLayer(EquipmentSlot slot) {
        return slot == EquipmentSlot.LEGS ? EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS : EquipmentClientInfo.LayerType.HUMANOID;
    }

    private static Identifier getTexture(List<EquipmentClientInfo.Layer> info, EquipmentClientInfo.LayerType layer) {
        return info.getFirst().textureId().withPath(path -> "entity/equipment/" + layer.getSerializedName() + "/" + path);
    }

    @FunctionalInterface
    public interface AttachableCreator {

        Optional<BedrockAttachable> create(Identifier bedrockIdentifier, Consumer<TextureHolder> textureConsumer);
    }
}
