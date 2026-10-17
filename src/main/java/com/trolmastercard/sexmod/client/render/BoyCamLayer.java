package com.trolmastercard.sexmod.client.render;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.trolmastercard.sexmod.client.scene.BoyCamera;
import com.trolmastercard.sexmod.entity.GirlEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.jspecify.annotations.Nullable;

/** Reports where her {@code boyCam} bone is drawn while she carries the local player ({@link BoyCamera}). Draws nothing. */
public class BoyCamLayer<T extends GirlEntity> extends GeoRenderLayer<T, Void, LivingEntityRenderState> {
    private static final DataTicket<Integer> GIRL = DataTicket.create("sexmod_boy_cam_girl", Integer.class);

    public BoyCamLayer(GeoRenderer<T, Void, LivingEntityRenderState> renderer) {
        super(renderer);
    }

    @Override
    public void addRenderData(T girl, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
        if (BoyCamera.wanted(girl)) renderState.addGeckolibData(GIRL, girl.getId());
    }

    @Override
    public void preRender(RenderPassInfo<LivingEntityRenderState> pass, SubmitNodeCollector renderTasks) {
        Integer id = pass.renderState().getGeckolibData(GIRL);
        if (id == null) return;
        pass.addBonePositionListener(BoyCamera.BONE, (worldPos, modelPos, localPos) -> {
            if (worldPos != null) BoyCamera.report(id, worldPos);
        });
    }
}
