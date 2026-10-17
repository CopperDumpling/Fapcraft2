package com.trolmastercard.sexmod.client.render;

import com.geckolib.cache.model.GeoBone;
import com.geckolib.cache.model.GeoQuad;
import com.geckolib.cache.model.GeoVertex;
import com.geckolib.cache.model.cuboid.CuboidGeoBone;
import com.geckolib.cache.model.cuboid.GeoCube;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Consumer;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

/** Draws a bone's cubes by hand, for layers that redraw bones the main pass skipped. */
final class GeoCubes {
    private GeoCubes() {
    }

    /** Whether the bone has cubes of its own to draw. */
    static boolean hasCubes(GeoBone bone) {
        return bone instanceof CuboidGeoBone cuboid && cuboid.cubes.length > 0;
    }

    /** {@code bone} and every bone below it. */
    static void forEachBone(GeoBone bone, Consumer<GeoBone> action) {
        action.accept(bone);
        for (GeoBone child : bone.children()) {
            forEachBone(child, action);
        }
    }

    /**
     * Draws the bone's cubes. {@code poseStack} must be at the bone (as a per-bone render leaves
     * it); {@code uOffset} shifts every U coordinate (0..1 texture space).
     */
    static void renderBone(GeoBone bone, PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color, float uOffset) {
        poseStack.pushPose();
        bone.translateAwayFromPivotPoint(poseStack);
        for (GeoCube cube : ((CuboidGeoBone) bone).cubes) {
            poseStack.pushPose();
            renderCube(cube, poseStack, buffer, light, overlay, color, uOffset);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderCube(GeoCube cube, PoseStack poseStack, VertexConsumer buffer, int light, int overlay, int color, float uOffset) {
        cube.translateToPivotPoint(poseStack);
        cube.rotate(poseStack);
        cube.translateAwayFromPivotPoint(poseStack);
        Matrix3f normalPose = poseStack.last().normal();
        Matrix4f pose = poseStack.last().pose();
        for (GeoQuad quad : cube.quads()) {
            if (quad == null) continue;
            Vector3f normal = normalPose.transform(new Vector3f(quad.normalVec()));
            RenderUtil.fixInvertedFlatCube(cube, normal);
            for (GeoVertex vertex : quad.vertices()) {
                Vector4f p = pose.transform(new Vector4f(vertex.posX(), vertex.posY(), vertex.posZ(), 1));
                buffer.addVertex(p.x(), p.y(), p.z(), color, vertex.texU() + uOffset, vertex.texV(), overlay, light, normal.x(), normal.y(), normal.z());
            }
        }
    }
}
