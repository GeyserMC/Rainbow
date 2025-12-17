package org.geysermc.rainbow.mapping.geometry;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockElementRotation;
import net.minecraft.client.renderer.block.model.SimpleUnbakedGeometry;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.UnbakedGeometry;
import net.minecraft.core.Direction;
import org.geysermc.rainbow.mapping.texture.StitchedTextures;
import org.geysermc.rainbow.mixin.FaceBakeryAccessor;
import org.geysermc.rainbow.pack.geometry.BedrockGeometry;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.Map;

public class GeometryMapper {
    private static final Vector3fc CENTRE_OFFSET = new Vector3f(8.0F, 0.0F, 8.0F);

    public static BedrockGeometry mapGeometry(String identifier, String boneName, ResolvedModel model, StitchedTextures textures) {
        UnbakedGeometry top = model.getTopGeometry();
        if (top == UnbakedGeometry.EMPTY) {
            return BedrockGeometry.EMPTY;
        }

        BedrockGeometry.Builder builder = BedrockGeometry.builder(identifier);
        // Blockbench seems to always use these values TODO that's wrong
        builder.withVisibleBoundsWidth(4.0F);
        builder.withVisibleBoundsHeight(4.0F);
        builder.withVisibleBoundsOffset(new Vector3f(0.0F, 0.75F, 0.0F));

        builder.withTextureWidth(textures.width());
        builder.withTextureHeight(textures.height());

        BedrockGeometry.Bone.Builder bone = BedrockGeometry.bone(boneName);

        Vector3f min = new Vector3f(Float.MAX_VALUE);
        Vector3f max = new Vector3f(Float.MIN_VALUE);

        SimpleUnbakedGeometry geometry = (SimpleUnbakedGeometry) top;
        for (BlockElement element : geometry.elements()) {
            BedrockGeometry.Cube cube = mapBlockElement(element, textures).build();
            bone.withCube(cube);
            min.min(cube.origin());
            max.max(cube.origin().add(cube.size(), new Vector3f()));
        }

        // Calculate the pivot to be at the centre of the bone
        // This is important for animations later, display animations rotate around the centre on Java
        bone.withPivot(min.add(max.sub(min).div(2.0F)));

        // Bind to the bone of the current item slot
        bone.withBinding("q.item_slot_to_bone_name(context.item_slot)");
        return builder.withBone(bone).build();
    }

    // After hours of painfully suffering and 40 test builds of Rainbow, I finally got the right formula together and somehow made this mess of a code
    // work properly, or at least, I think it is. I physically jumped in the air and cheered as I saw my models convert properly.
    // Now, make sure you are ready to witness my deformed creation
    private static BedrockGeometry.Cube.Builder mapBlockElement(BlockElement element, StitchedTextures textures) {
        // For some reason the X axis is inverted on bedrock (thanks Blockbench!!)

        // The centre of the model is back by 8 in the X and Z direction on bedrock, so start by move the from and to points of the cube, and later the pivot, like that
        Vector3f from = element.from().sub(CENTRE_OFFSET, new Vector3f());
        Vector3f to = element.to().sub(CENTRE_OFFSET, new Vector3f());

        // Bedrock wants the origin to be the smallest X/Y/Z of the cube, so we do a min here
        Vector3f origin = from.min(to, new Vector3f());
        // Bedrock also wants a cube size instead of a second cube, so calculate the max and subtract the min/origin
        Vector3fc size = from.max(to, new Vector3f()).sub(origin);

        // The X-axis is inverted for some reason on bedrock, so we have to do this (thanks Blockbench!!)
        origin.x = -(origin.x + size.x());

        BedrockGeometry.Cube.Builder builder = BedrockGeometry.cube(origin, size);

        for (Map.Entry<Direction, BlockElementFace> faceEntry : element.faces().entrySet()) {
            Direction direction = faceEntry.getKey();
            BlockElementFace face = faceEntry.getValue();

            Vector2f uvOrigin;
            Vector2f uvSize;
            BlockElementFace.UVs uvs = face.uvs();
            if (uvs == null) {
                // Java defaults to a set of UV values determined by the position of the face if no UV values were specified
                uvs = FaceBakeryAccessor.invokeDefaultFaceUV(element.from(), element.to(), direction);
            }

            // Up and down faces are special and have their UVs flipped
            if (direction.getAxis() == Direction.Axis.Y) {
                uvOrigin = new Vector2f(uvs.maxU(), uvs.maxV());
                uvSize = new Vector2f(uvs.minU() - uvs.maxU(), uvs.minV() - uvs.maxV());
            } else {
                uvOrigin = new Vector2f(uvs.minU(), uvs.minV());
                uvSize = new Vector2f(uvs.maxU() - uvs.minU(), uvs.maxV() - uvs.minV());
            }

            // UV values on Java are always in the [0;16] range, so if the texture was stitched (which it should have been, unless it doesn't exist),
            // adjust the values properly to the texture size, and offset the UVs by the texture's starting UV
            textures.getSprite(face.texture()).ifPresent(sprite -> {
                float widthMultiplier = sprite.contents().width() / 16.0F;
                float heightMultiplier = sprite.contents().height() / 16.0F;
                uvOrigin.mul(widthMultiplier, heightMultiplier);
                uvSize.mul(widthMultiplier, heightMultiplier);
                uvOrigin.add(sprite.getX(), sprite.getY());
            });
            builder.withFace(direction, uvOrigin, uvSize, face.rotation());
        }

        BlockElementRotation rotation = element.rotation();
        if (rotation != null) {
            // MC multiplies model origin by 0.0625 when loading rotation origin

            // Same as above for inverting the X axis (thanks again Blockbench!!)
            builder.withPivot(rotation.origin().div(0.0625F, new Vector3f()).sub(CENTRE_OFFSET).mul(-1.0F, 1.0F, 1.0F));
            builder.withRotation(getBedrockRotation(rotation.value()));
            // TODO translate rescale property?
        }

        return builder;
    }

    private static Vector3fc getBedrockRotation(BlockElementRotation.RotationValue rotation) {
        // Same as in the method above, but for some reason the Z axis too: X and Z axes have to be inverted (thanks again, so much, Blockbench!!!)
        return switch (rotation) {
            case BlockElementRotation.EulerXYZRotation(float x, float y, float z) -> new Vector3f(-x, y, -z); // TODO check if these angle transformations are right, they should be
            case BlockElementRotation.SingleAxisRotation(Direction.Axis axis, float angle) -> switch (axis) {
                case X -> new Vector3f(-angle, 0.0F, 0.0F);
                case Y -> new Vector3f(0.0F, angle, 0.0F);
                case Z -> new Vector3f(0.0F, 0.0F, -angle);
            };
            default -> throw new IllegalArgumentException("Don't know how to transform rotation of type " + rotation.getClass() + " to bedrock rotation");
        };
    }
}
