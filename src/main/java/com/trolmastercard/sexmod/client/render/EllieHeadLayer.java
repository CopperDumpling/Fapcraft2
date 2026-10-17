package com.trolmastercard.sexmod.client.render;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.trolmastercard.sexmod.entity.GirlAction;
import com.trolmastercard.sexmod.entity.ellie.EllieEntity;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * While Ellie sits waiting on a bed her head follows the nearest player within 15 blocks
 * (1.12.2 {@code EllieNpcModel.setLivingAnimations}). The arcs are 1.12.2's, per seat facing;
 * past either end of an arc she looks straight ahead again. Draws nothing.
 */
public class EllieHeadLayer extends GeoRenderLayer<EllieEntity, Void, LivingEntityRenderState> {
    private static final DataTicket<float[]> HEAD = DataTicket.create("sexmod_ellie_head", float[].class);
    private static final double RANGE = 15.0;
    /**
     * Per seat yaw: offset added to the angle, then the arc's two ends. 1.12.2 added her yaw in
     * degrees to an angle in radians; these ends were tuned around that, so it is kept.
     */
    private static final Map<Integer, float[]> ARCS = Map.of(
        0, new float[]{0.0F, -1.2F, 1.2F},
        -90, new float[]{2.0F, -71.56F, -68.0F},
        90, new float[]{-2.0F, 68.0F, 70.5F});

    public EllieHeadLayer(GeoRenderer<EllieEntity, Void, LivingEntityRenderState> renderer) {
        super(renderer);
    }

    @Override
    public void addRenderData(EllieEntity ellie, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
        if (ellie.getAction() != GirlAction.SITDOWNIDLE) return;
        Player player = ellie.level().getNearestPlayer(ellie, RANGE);
        if (player == null) return;
        Vec3 toPlayer = ellie.position().subtract(player.position());
        float yaw = Mth.wrapDegrees(ellie.getYRot());
        int facing = Math.round(yaw) == -180 ? 180 : Math.round(yaw);
        float angle = (float) Math.atan2(toPlayer.x, toPlayer.z);
        float headYaw;
        if (facing == 180) {
            headYaw = angle * 1.2F;
            headYaw = headYaw > 0.0F ? Mth.clamp(headYaw, 1.5F, 3.14F) : Mth.clamp(headYaw, -3.14F, -1.5F);
            headYaw = headYaw == 1.5F || headYaw == 3.14F || headYaw == -3.14F || headYaw == -1.5F ? 0.0F : headYaw + 3.0F;
        } else {
            float[] arc = ARCS.get(facing);
            if (arc == null) return;
            headYaw = (angle + arc[0] + yaw) * 0.8F;
            headYaw = Mth.clamp(headYaw, arc[1], arc[2]);
            if (headYaw == arc[1] || headYaw == arc[2]) headYaw = 0.0F;
        }
        float headPitch = headYaw == 0.0F ? 0.0F : Mth.clamp((float) ((player.getY() - ellie.getY()) * 0.5), -0.75F, 0.75F);
        renderState.addGeckolibData(HEAD, new float[]{headPitch, headYaw});
    }

    @Override
    public void preRender(RenderPassInfo<LivingEntityRenderState> pass, SubmitNodeCollector renderTasks) {
        float[] head = pass.renderState().getGeckolibData(HEAD);
        if (head == null) return;
        pass.addBoneUpdater((info, snapshots) -> info.model().getBone("head").ifPresent(bone ->
            snapshots.get(bone).setRotX(head[0]).setRotY(head[1])));
    }
}
