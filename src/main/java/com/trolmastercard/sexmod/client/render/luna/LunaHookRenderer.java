package com.trolmastercard.sexmod.client.render.luna;

import com.geckolib.util.RenderUtil;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.trolmastercard.sexmod.entity.luna.LunaEntity;
import com.trolmastercard.sexmod.entity.luna.LunaHookEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Luna's bobber and line (1.12.2 {@code SexEntityRenderer}): the vanilla bobber sprite with a line
 * to her rod hand. Once she has reeled something in, the catch flies from the bobber to her over
 * half a second (no line meanwhile); when it arrives her own renderer takes it over.
 */
public class LunaHookRenderer extends EntityRenderer<LunaHookEntity, LunaHookRenderer.State> {
    private static final Identifier TEXTURE = Identifier.withDefaultNamespace("textures/entity/fishing/fishing_hook.png");
    private static final RenderType RENDER_TYPE = RenderTypes.entityCutoutCull(TEXTURE);

    private final ItemModelResolver itemModelResolver;

    public LunaHookRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
    }

    public static class State extends EntityRenderState {
        boolean visible;
        /** Where the catch is drawn instead of the bobber, relative to the bobber; zero without a catch. */
        Vec3 catchOffset = Vec3.ZERO;
        @Nullable ItemStackRenderState caught;
        /** Line from the bobber to her hand, or null while a catch flies. */
        @Nullable Vec3 lineOffset;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(LunaHookEntity hook, State state, float partialTicks) {
        super.extractRenderState(hook, state, partialTicks);
        LunaEntity luna = hook.getOwner();
        state.visible = luna != null && luna.catchFlight() < 1.0F;
        state.caught = null;
        state.catchOffset = Vec3.ZERO;
        state.lineOffset = null;
        if (!state.visible) return;
        Vec3 hookPos = hook.getPosition(partialTicks);
        ItemStack caught = luna.getCatch();
        if (!caught.isEmpty()) {
            // half a second on the way: 1.12.2 advanced it by 2 / fps each frame
            int fps = Math.max(1, Minecraft.getInstance().getFps());
            luna.setCatchFlight(Math.min(1.0F, luna.catchFlight() + 2.0F / fps));
            Vec3 target = luna.getPosition(partialTicks).add(0.0, 0.875, 0.0);
            state.catchOffset = target.subtract(hookPos).scale(luna.catchFlight());
            state.caught = RenderUtil.createRenderStateForItem(caught, this.itemModelResolver, ItemDisplayContext.THIRD_PERSON_RIGHT_HAND, luna);
            return;
        }
        luna.setCatchFlight(0.0F);
        // her rod hand, from where she stands fishing and the way she faces
        float yaw = luna.getYRot();
        double sin = Mth.sin(yaw * Mth.DEG_TO_RAD);
        double cos = Mth.cos(yaw * Mth.DEG_TO_RAD);
        double side = 0.35;
        Vec3 pos = luna.position();
        Vec3 hand = new Vec3(pos.x - cos * side - sin * 0.8, pos.y + luna.getEyeHeight() - 0.45, pos.z - sin * side + cos * 0.8);
        // the line leaves the bobber a little off its centre
        Vec3 knot = hookPos.add(
            -Math.sin((yaw + 90.0F) * Mth.DEG_TO_RAD) * 0.1896224320030116 - sin * 0.08742380916962415,
            -0.25,
            Math.cos((yaw + 90.0F) * Mth.DEG_TO_RAD) * 0.1896224320030116 + cos * 0.08742380916962415);
        state.lineOffset = hand.subtract(knot);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (!state.visible) return;
        poseStack.pushPose();
        poseStack.translate(state.catchOffset);
        poseStack.pushPose();
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.rotate(camera.orientation);
        if (state.caught != null && !state.caught.isEmpty()) {
            poseStack.pushPose();
            poseStack.scale(2.0F, 2.0F, 2.0F);
            poseStack.translate(0.0F, -0.2F, 0.0F);
            state.caught.submit(poseStack, collector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
        collector.submitCustomGeometry(poseStack, RENDER_TYPE, (pose, buffer) -> {
            vertex(buffer, pose, state.lightCoords, 0.0F, 0, 0, 1);
            vertex(buffer, pose, state.lightCoords, 1.0F, 0, 1, 1);
            vertex(buffer, pose, state.lightCoords, 1.0F, 1, 1, 0);
            vertex(buffer, pose, state.lightCoords, 0.0F, 1, 0, 0);
        });
        poseStack.popPose();
        Vec3 line = state.lineOffset;
        if (line != null) {
            float width = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
            float xa = (float) line.x;
            float ya = (float) line.y;
            float za = (float) line.z;
            collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
                for (int i = 0; i < 16; i++) {
                    float a0 = i / 16.0F;
                    float a1 = (i + 1) / 16.0F;
                    stringVertex(xa, ya, za, buffer, pose, a0, a1, width);
                    stringVertex(xa, ya, za, buffer, pose, a1, a0, width);
                }
            });
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, int light, float x, int y, int u, int v) {
        buffer.addVertex(pose, x - 0.5F, y - 0.5F, 0.0F)
            .setColor(-1)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    /** A sagging line: y follows (a² + a) / 2 of the drop, raised 0.25 (as vanilla and 1.12.2). */
    private static void stringVertex(float xa, float ya, float za, VertexConsumer buffer, PoseStack.Pose pose, float a, float next, float width) {
        float x = xa * a;
        float y = ya * (a * a + a) * 0.5F + 0.25F;
        float z = za * a;
        float nx = xa * next - x;
        float ny = ya * (next * next + next) * 0.5F + 0.25F - y;
        float nz = za * next - z;
        float length = Mth.sqrt(nx * nx + ny * ny + nz * nz);
        buffer.addVertex(pose, x, y, z).setColor(0xFF000000).setNormal(pose, nx / length, ny / length, nz / length).setLineWidth(width);
    }

    @Override
    protected boolean affectedByCulling(LunaHookEntity hook) {
        return false;
    }
}
