package org.geysermc.rainbow.mapping.attachable;

import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.equipment.Equippable;
import org.geysermc.rainbow.mapping.PackContext;
import org.geysermc.rainbow.mapping.PackSerializer;
import org.geysermc.rainbow.mapping.PackSerializingContext;
import org.geysermc.rainbow.mapping.geometry.BedrockGeometryContext;
import org.geysermc.rainbow.mapping.geometry.MappedGeometry;
import org.geysermc.rainbow.mapping.texture.ModelTextures;
import org.geysermc.rainbow.mapping.texture.TextureHolder;
import org.geysermc.rainbow.pack.PackPaths;
import org.geysermc.rainbow.pack.attachable.BedrockAttachable;
import org.geysermc.rainbow.pack.attachable.VanillaGeometries;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record BedrockAttachableContext(Optional<BedrockAttachable> attachable, Optional<TextureHolder> equipmentTexture) implements PackSerializer.Serializable {
    public static final BedrockAttachableContext EMPTY = new BedrockAttachableContext(Optional.empty(), Optional.empty());

    public BedrockAttachableContext(BedrockAttachable attachable, TextureHolder equipmentTexture) {
        this(Optional.of(attachable), Optional.of(equipmentTexture));
    }

    public BedrockAttachableContext(BedrockAttachable attachable) {
        this(Optional.of(attachable), Optional.empty());
    }

    @Override
    public CompletableFuture<?> save(PackSerializingContext context) {
        return PackSerializer.Serializable.wrapOptionalCodec(BedrockAttachable.CODEC, attachable, PackPaths::attachablePath)
                .with(equipmentTexture)
                .save(context);
    }

    public static BedrockAttachableContext create(Identifier identifier, ItemStackTemplate stack, BedrockGeometryContext geometryContext, ModelTextures textures, PackContext context) {
        // Prefer equippable over animation or geometry attachable, since when an item is equippable, it shows its 2D icon in first and third person (see notes in AnimationMapper)
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null) {
            EquipmentSlot slot = equippable.slot();
            Optional<EquipmentClientInfo> equipmentInfo = equippable.assetId()
                    .flatMap(asset -> context.assetResolver().getEquipmentInfo(asset))
                    .filter(info -> info != EquipmentAssetManager.MISSING);
            if (equipmentInfo.isPresent()) {
                boolean glider = stack.get(DataComponents.GLIDER) != null;
                EquipmentClientInfo.LayerType layer = getEquipmentLayer(slot, glider);
                Identifier equipmentTexture = getEquipmentTexture(equipmentInfo.get().getLayers(layer), layer);
                return new BedrockAttachableContext(BedrockAttachable.equipment(identifier, slot, equipmentTexture.getPath(), glider).build(),
                        TextureHolder.createBuiltIn(equipmentTexture));
            }
        }
        // Fall-through if no equipment info
        if (textures.requiresAttachable() || geometryContext.geometry().isPresent()) {
            BedrockAttachable.Builder attachable = BedrockAttachable.geometry(identifier, geometryContext.geometry().map(MappedGeometry::identifier).orElse(VanillaGeometries.ITEM_SPRITE));
            textures.applyToAttachable(attachable);
            geometryContext.animation().ifPresent(animation -> {
                attachable.withAnimation("first_person", animation.firstPerson());
                attachable.withAnimation("third_person", animation.thirdPerson());
                attachable.withAnimation("head", animation.head());
                attachable.withScript("animate", "first_person", "context.is_first_person == 1.0");
                attachable.withScript("animate", "third_person", "context.is_first_person == 0.0 && (context.item_slot == 'main_hand' || context.item_slot == 'off_hand')");
                attachable.withScript("animate", "head", "context.is_first_person == 0.0 && context.item_slot == 'head'");
            });
            return new BedrockAttachableContext(attachable.build());
        }
        return EMPTY;
    }

    private static EquipmentClientInfo.LayerType getEquipmentLayer(EquipmentSlot slot, boolean glider) {
        return glider ? EquipmentClientInfo.LayerType.WINGS : slot == EquipmentSlot.LEGS ? EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS : EquipmentClientInfo.LayerType.HUMANOID;
    }

    private static Identifier getEquipmentTexture(List<EquipmentClientInfo.Layer> layers, EquipmentClientInfo.LayerType layerType) {
        return layers.getFirst().textureId().withPath(path -> "entity/equipment/" + layerType.getSerializedName() + "/" + path);
    }
}
