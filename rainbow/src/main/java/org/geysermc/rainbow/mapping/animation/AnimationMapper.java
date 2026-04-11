package org.geysermc.rainbow.mapping.animation;

import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import org.geysermc.rainbow.pack.animation.BedrockAnimation;
import org.joml.Vector3f;
import org.joml.Vector3fc;

// TODO these offset values are still not entirely right, I think
public class AnimationMapper {
    private static final Vector3fc FIRST_PERSON_POSITION_OFFSET = new Vector3f(-7.0F, 22.5F, -7.0F);
    private static final Vector3fc FIRST_PERSON_ROTATION_OFFSET = new Vector3f(-22.5F, 50.0F, -32.5F);

    // These transformations aren't perfect... but I spent over 4 hours trying to get these. It's good enough for me.
    public static BedrockAnimationContext mapAnimation(String identifier, String bone, ItemTransforms transforms) {
        // Note that translations are multiplied by 0.0625 after reading on Java, so we have to divide by that here

        // I don't think it's possible to display separate animations for left- and right hands
        ItemTransform firstPerson = transforms.firstPersonRightHand();
        Vector3f firstPersonPosition = firstPerson.translation().div(0.0625F, new Vector3f()).add(FIRST_PERSON_POSITION_OFFSET);
        Vector3f firstPersonRotation = FIRST_PERSON_ROTATION_OFFSET.add(firstPerson.rotation(), new Vector3f());
        Vector3f firstPersonScale = new Vector3f(firstPerson.scale());

        ItemTransform thirdPerson = transforms.thirdPersonRightHand();
        // Translation Y/Z axes are swapped on bedrock, bedrock displays the model lower than Java does, and the X/Y axes (Java) is inverted on bedrock
        // Also the model appears lower on bedrock so we need to add 10 on the Y axis here
        Vector3f thirdPersonTranslation = thirdPerson.translation().div(0.0625F, new Vector3f());
        Vector3f thirdPersonPosition = new Vector3f(-thirdPersonTranslation.x(), 10.0F + thirdPersonTranslation.z(), -thirdPersonTranslation.y());
        // Rotation X/Y axes are inverted on bedrock, bedrock needs a +90-degree rotation on the X axis, and I couldn't figure out how the Z axis works
        Vector3f thirdPersonRotation = new Vector3f(-thirdPerson.rotation().x() + 90.0F, -thirdPerson.rotation().y(), 0.0F);
        Vector3f thirdPersonScale = new Vector3f(thirdPerson.scale());

        // Head translation + scale is scaled by around 0.655 (not perfect but close enough)
        ItemTransform head = transforms.head();
        // Add a base translation of 20 on the Y axis as bedrock displays the item at the player's feet
        // Translation is inverted on the X axis
        Vector3f headPosition = head.translation().div(0.0625F, new Vector3f()).mul(-0.655F, 0.655F, 0.655F).add(0.0F, 20.0F, 0.0F);
        // Rotation is inverted on the X and Y axis
        Vector3f headRotation = new Vector3f(-head.rotation().x(), -head.rotation().y(), head.rotation().z());
        Vector3f headScale = head.scale().mul(0.655F, new Vector3f());

        // Note that for items marked as equippable, the 3D model only shows up when having the item equipped on the head, and the icon is used when holding the item in hand
        // Interestingly when an item is NOT marked equippable (so the player can't equip it normally), the 3D model does show up properly both in hand and on head
        // I think this is a bedrock bug and not something we can fix
        return new BedrockAnimationContext(BedrockAnimation.builder()
                .withAnimation(identifier + ".hold_first_person", BedrockAnimation.animation()
                        .withLoopMode(BedrockAnimation.LoopMode.LOOP)
                        .withBone(bone, firstPersonPosition, firstPersonRotation, firstPersonScale))
                .withAnimation(identifier + ".hold_third_person", BedrockAnimation.animation()
                        .withLoopMode(BedrockAnimation.LoopMode.LOOP)
                        .withBone(bone, thirdPersonPosition, thirdPersonRotation, thirdPersonScale))
                .withAnimation(identifier + ".head", BedrockAnimation.animation()
                        .withLoopMode(BedrockAnimation.LoopMode.LOOP)
                        .withBone(bone, headPosition, headRotation, headScale))
                .build(), "animation." + identifier + ".hold_first_person", "animation." + identifier + ".hold_third_person", "animation." + identifier + ".head");
    }
}
