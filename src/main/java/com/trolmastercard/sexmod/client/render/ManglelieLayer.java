package com.trolmastercard.sexmod.client.render;

import com.geckolib.animation.state.BoneSnapshot;
import com.geckolib.cache.model.GeoBone;
import com.geckolib.renderer.base.BoneSnapshots;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.entity.manglelie.ManglelieEntity;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.ARGB;
import org.joml.Vector3f;

/**
 * Manglelie's clothes (1.12.2 {@code ManglelieNpcModel.animateModel} and {@code ManglelieRenderer}),
 * as worn outside the threesome:
 * <ul>
 *   <li>the skirted hips show; the bare cheeks and sides under the skirt are hidden;</li>
 *   <li>the skirt is no model part but 40 cloth strips drawn between the {@code skirt_<strip>_<0-2>}
 *   bones, alternating light and dark purple. The strips follow her body: strips 17-34 rise with
 *   the cheek on their side (0.01 per degree it is raised), and the middle point of strips 1-10
 *   takes the pitch of the leg on its side;</li>
 *   <li>three purple cloth bands span her chest, from each {@code clothBoob*conStart} bone to its
 *   {@code conEnd} bone.</li>
 * </ul>
 * The strips and bands are flat-coloured, as 1.12.2 drew them.
 */
public class ManglelieLayer extends GeoRenderLayer<ManglelieEntity, Void, LivingEntityRenderState> {
    private static final List<String> HIDDEN = List.of("cheekRBelowSkirt", "cheekLBelowSkirt", "sideRNoSkirt", "sideLNoSkirt");
    private static final int STRIPS = 40;
    private static final int SKIRT_LIGHT = ARGB.color(255, 79, 74, 188);
    private static final int SKIRT_DARK = ARGB.color(255, 63, 59, 150);
    private static final int CLOTH = ARGB.color(255, 115, 108, 188);
    /** The chest bands, with the half-sizes of their two ends in blocks (1.12.2 {@code OFFSET_BODY}, {@code OFFSET_ARM}, {@code OFFSET_LEG}). */
    private static final Band[] BANDS = {
        new Band("clothBoobLconStart", "clothBoobLconEnd", new Vector3f(0.05F, 0.04F, 0.0F), new Vector3f(0.0F, 0.065F, 0.0F)),
        new Band("clothBoobRconStart", "clothBoobRconEnd", new Vector3f(0.05F, 0.04F, 0.0F), new Vector3f(0.0F, 0.065F, 0.0F)),
        new Band("clothBoobMidconStart", "clothBoobMidconEnd", new Vector3f(0.0F, 0.03F, 0.03F), new Vector3f(0.0F, 0.03F, 0.03F)),
    };

    private record Band(String start, String end, Vector3f startSize, Vector3f endSize) {}

    public ManglelieLayer(GeoRenderer<ManglelieEntity, Void, LivingEntityRenderState> renderer) {
        super(renderer);
    }

    @Override
    public void preRender(RenderPassInfo<LivingEntityRenderState> pass, SubmitNodeCollector renderTasks) {
        pass.addBoneUpdater((info, snapshots) -> {
            for (String name : HIDDEN) {
                info.model().getBone(name).ifPresent(bone -> snapshots.get(bone).skipRender(true).skipChildrenRender(true));
            }
            for (int strip = 17; strip < 35; strip++) {
                float cheek = (float) Math.toDegrees(pitch(info, snapshots, strip < 26 ? "cheekL" : "cheekR"));
                if (cheek < 0.0F) continue;
                for (int piece = 0; piece < 3; piece++) {
                    info.model().getBone("skirt_" + strip + "_" + piece).ifPresent(bone -> {
                        BoneSnapshot snapshot = snapshots.get(bone);
                        snapshot.setTranslateY(snapshot.getTranslateY() + cheek * 0.01F);
                    });
                }
            }
            for (int strip = 1; strip < 11; strip++) {
                float leg = pitch(info, snapshots, strip < 6 ? "legR" : "legL");
                if (leg < 0.0F) continue;
                // 1.12.2 replaced the bone's whole pitch; a GeckoLib 5 snapshot adds to the model's
                info.model().getBone("skirt_" + strip + "_1").ifPresent(bone -> snapshots.get(bone)
                    .setRotX(leg - bone.baseRotX())
                    .setTranslateY((float) Math.toRadians(Math.toDegrees(leg) * 0.03)));
            }
        });
    }

    /** A bone's whole pitch (model pose plus animation) in radians, or -1 if the model lacks it. */
    private static float pitch(RenderPassInfo<LivingEntityRenderState> info, BoneSnapshots snapshots, String name) {
        Optional<GeoBone> bone = info.model().getBone(name);
        return bone.map(b -> b.baseRotX() + snapshots.get(b).getRotX()).orElse(-1.0F);
    }

    @Override
    public void submitRenderTask(RenderPassInfo<LivingEntityRenderState> pass, SubmitNodeCollector renderTasks) {
        renderTasks.submitCustomGeometry(pass.poseStack(), RenderTypes.debugQuads(), (pose, buffer) -> {
            PoseStack poseStack = pass.poseStack();
            poseStack.pushPose();
            poseStack.last().set(pose);
            pass.renderPosed(() -> {
                renderSkirt(pass, poseStack, buffer);
                for (Band band : BANDS) renderBand(pass, poseStack, buffer, band);
            });
            poseStack.popPose();
        });
    }

    private static void renderSkirt(RenderPassInfo<LivingEntityRenderState> pass, PoseStack poseStack, VertexConsumer buffer) {
        Vector3f[][] points = new Vector3f[STRIPS][3];
        for (int strip = 0; strip < STRIPS; strip++) {
            for (int piece = 0; piece < 3; piece++) {
                Vector3f point = bonePosition(pass, poseStack, "skirt_" + strip + "_" + piece);
                if (point == null) return;
                points[strip][piece] = point;
            }
        }
        for (int strip = 0; strip < STRIPS; strip++) {
            Vector3f[] from = points[strip];
            Vector3f[] to = points[(strip + 1) % STRIPS];
            int color = strip % 2 == 0 ? SKIRT_LIGHT : SKIRT_DARK;
            quad(buffer, color, from[0], from[1], to[1], to[0]);
            quad(buffer, color, from[1], to[1], to[2], from[2]);
        }
    }

    /** A box from one bone to another, sized by the band's half-sizes in her own axes. */
    private static void renderBand(RenderPassInfo<LivingEntityRenderState> pass, PoseStack poseStack, VertexConsumer buffer, Band band) {
        Vector3f start = bonePosition(pass, poseStack, band.start());
        Vector3f end = bonePosition(pass, poseStack, band.end());
        if (start == null || end == null) return;
        Vector3f[] c = new Vector3f[8];
        corners(poseStack, start, band.startSize(), c, 0);
        corners(poseStack, end, band.endSize(), c, 4);
        quad(buffer, CLOTH, c[0], c[1], c[2], c[3]);
        quad(buffer, CLOTH, c[4], c[5], c[6], c[7]);
        quad(buffer, CLOTH, c[1], c[2], c[6], c[5]);
        quad(buffer, CLOTH, c[3], c[7], c[4], c[0]);
        quad(buffer, CLOTH, c[1], c[0], c[4], c[5]);
        quad(buffer, CLOTH, c[2], c[3], c[7], c[6]);
    }

    /** The four corners of a band end: a cross-section in her X/Y plane, or her Y/Z plane when it has no width. */
    private static void corners(PoseStack poseStack, Vector3f at, Vector3f size, Vector3f[] out, int index) {
        float[][] offsets = size.x == 0.0F
            ? new float[][]{{0, size.y, size.z}, {0, -size.y, size.z}, {0, -size.y, -size.z}, {0, size.y, -size.z}}
            : new float[][]{{size.x, size.y, 0}, {-size.x, size.y, 0}, {-size.x, -size.y, 0}, {size.x, -size.y, 0}};
        for (int i = 0; i < 4; i++) {
            Vector3f offset = poseStack.last().pose().transformDirection(new Vector3f(offsets[i][0], offsets[i][1], offsets[i][2]));
            out[index + i] = new Vector3f(at).add(offset);
        }
    }

    /** Where a bone sits this frame, in the space the buffer draws in; null if the model lacks it. */
    private static Vector3f bonePosition(RenderPassInfo<LivingEntityRenderState> pass, PoseStack poseStack, String name) {
        GeoBone bone = pass.model().getBone(name).orElse(null);
        if (bone == null) return null;
        poseStack.pushPose();
        RenderUtil.transformToBone(poseStack, bone); // ends on the bone's pivot, where 1.12.2 cached it
        Vector3f position = poseStack.last().pose().transformPosition(new Vector3f());
        poseStack.popPose();
        return position;
    }

    private static void quad(VertexConsumer buffer, int color, Vector3f a, Vector3f b, Vector3f c, Vector3f d) {
        for (Vector3f v : new Vector3f[]{a, b, c, d}) buffer.addVertex(v.x, v.y, v.z).setColor(color);
    }
}
