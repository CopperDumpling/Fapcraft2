package com.trolmastercard.sexmod.client.render;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.trolmastercard.sexmod.entity.GirlAction;
import com.trolmastercard.sexmod.entity.GirlEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * While idle or attacking, a girl looks where she faces (1.12.2 {@code GirlModel.handleAnimationEvent}):
 * her neck takes half her head's yaw, her head the whole yaw and the pitch, and her body is held
 * straight. Scene steps leave the head to their animations. Draws nothing.
 */
public class HeadLookLayer<T extends GirlEntity> extends GeoRenderLayer<T, Void, LivingEntityRenderState> {
    private static final DataTicket<Boolean> LOOKS = DataTicket.create("sexmod_head_look", Boolean.class);

    public HeadLookLayer(GeoRenderer<T, Void, LivingEntityRenderState> renderer) {
        super(renderer);
    }

    @Override
    public void addRenderData(T girl, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
        GirlAction action = girl.getAction();
        renderState.addGeckolibData(LOOKS, action == GirlAction.NULL || action == GirlAction.ATTACK);
    }

    @Override
    public void preRender(RenderPassInfo<LivingEntityRenderState> pass, SubmitNodeCollector renderTasks) {
        if (!pass.renderState().getOrDefaultGeckolibData(LOOKS, false)) return;
        float yaw = pass.renderState().yRot * Mth.DEG_TO_RAD;
        float restPitch = getGeoModel() instanceof GirlModel<?> model ? model.headRestPitch() : 0.0F;
        float pitch = restPitch + pass.renderState().xRot * Mth.DEG_TO_RAD;
        // 1.12.2 set the bones' whole rotation; a GeckoLib 5 snapshot adds to the model's own
        pass.addBoneUpdater((info, snapshots) -> {
            info.model().getBone("neck").ifPresent(bone -> snapshots.get(bone).setRotY(yaw * 0.5F - bone.baseRotY()));
            info.model().getBone("head").ifPresent(bone -> snapshots.get(bone).setRotY(yaw - bone.baseRotY()).setRotX(pitch - bone.baseRotX()));
            info.model().getBone("body").or(() -> info.model().getBone("dd")).ifPresent(bone -> snapshots.get(bone).setRotY(-bone.baseRotY()));
        });
    }
}
