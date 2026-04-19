package org.geysermc.rainbow.mapping.animation;

import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import org.geysermc.rainbow.pack.animation.BedrockAnimation;
import org.joml.Vector3f;

// Note to self: if rotation issues are reported, check the rotation values for each one to see if they're inverted
// in blockbench or not
public class AnimationMapper {

    // These transformations aren't perfect... but I think it's near perfect now. or at least, I hope.
    // I think the only thing left is the X rotation of the third person view, which is weird on bedrock
    public static BedrockAnimationContext mapAnimation(String identifier, String bone, ItemTransforms transforms) {
        // Note that translations are multiplied by 0.0625 after reading on Java, so we have to divide by that here (ItemTransform.Deserializer)

        // I don't think it's possible to display separate animations for left- and right hands
        ItemTransform firstPerson = transforms.firstPersonRightHand();
        // Coordinate space differences for first person:
        // - Java: X right, Y up, Z inward
        // - Bedrock: X up, Y inward, Z right
        // Java to bedrock: X -> Z, Y -> X, Z -> Y
        // A base rotation of -90 is applied on the X axis, a base translation of 12.5 on the Y axis
        // Z (Java) rotation, Y (Java) translation is inverted
        Vector3f firstPersonRotation = new Vector3f(-90.0F + firstPerson.rotation().y(), -firstPerson.rotation().z(), firstPerson.rotation().x());
        Vector3f firstPersonTranslation = firstPerson.translation().div(0.0625F, new Vector3f());
        Vector3f firstPersonPosition = new Vector3f(-firstPersonTranslation.y(), 12.5F + firstPersonTranslation.z(), firstPersonTranslation.x());
        Vector3f firstPersonScale = new Vector3f(firstPerson.scale());

        ItemTransform thirdPerson = transforms.thirdPersonRightHand();
        // Coordinate space differences for third person:
        // - Java: X left, Y forward, Z up
        // - Bedrock: X left, Y up, Z backward
        // Java to bedrock: X -> X, Y -> Z, Z -> Y
        // A base rotation of 90 is applied on the X axis, a base translation of 12.5 on the Y axis
        // Y, Z (Java) rotations, X, Y (Java) translations are inverted
        // TODO fix X rotation
        Vector3f thirdPersonRotation = new Vector3f(90.0F, -thirdPerson.rotation().z(), -thirdPerson.rotation().y());
        Vector3f thirdPersonTranslation = thirdPerson.translation().div(0.0625F, new Vector3f());
        Vector3f thirdPersonPosition = new Vector3f(-thirdPersonTranslation.x(), 12.5F + thirdPersonTranslation.z(), -thirdPersonTranslation.y());
        Vector3f thirdPersonScale = new Vector3f(thirdPerson.scale());

        // Head translation + scale is scaled by around 0.655 (not perfect but close enough)
        // Coordinate space is the same
        // Add a base translation of (6, 29, -1)
        // X translation is inverted
        ItemTransform head = transforms.head();
        Vector3f headPosition = head.translation().div(0.0625F, new Vector3f()).mul(-0.655F, 0.655F, 0.655F).add(6.0F, 29.0F, -1.0F);
        Vector3f headRotation = new Vector3f(head.rotation());
        Vector3f headScale = head.scale().mul(0.655F, new Vector3f());

        // Note that for items marked as equippable, the 3D model only shows up when having the item equipped on the head, and the icon is used when holding the item in hand
        // Interestingly when an item is NOT marked equippable (so the player can't equip it normally), the 3D model does show up properly both in hand and on head
        // I think this is a bedrock bug and not something we can fix
        return new BedrockAnimationContext(identifier, BedrockAnimation.builder()
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
