package com.trolmastercard.sexmod.client.render;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.GeoRenderLayer;
import com.trolmastercard.sexmod.entity.GirlAction;
import com.trolmastercard.sexmod.entity.bee.BeeEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.jspecify.annotations.Nullable;

/**
 * The bee's chest (1.12.2 {@code BeeNpcModel.setLivingAnimations}): her {@code chest} bone shows
 * only while her idle-with-chest animation plays, so never during a scene or before she has one.
 */
public class BeeLayer extends GeoRenderLayer<BeeEntity, Void, LivingEntityRenderState> {
    private static final DataTicket<Boolean> SHOW_CHEST = DataTicket.create("sexmod_bee_chest", Boolean.class);

    public BeeLayer(GeoRenderer<BeeEntity, Void, LivingEntityRenderState> renderer) {
        super(renderer);
    }

    @Override
    public void addRenderData(BeeEntity bee, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
        renderState.addGeckolibData(SHOW_CHEST, bee.hasChest() && bee.getAction() == GirlAction.NULL);
    }

    @Override
    public void preRender(RenderPassInfo<LivingEntityRenderState> pass, SubmitNodeCollector renderTasks) {
        if (pass.renderState().getOrDefaultGeckolibData(SHOW_CHEST, false)) return;
        pass.addBoneUpdater((info, snapshots) -> info.model().getBone("chest")
            .ifPresent(bone -> snapshots.get(bone).skipRender(true).skipChildrenRender(true)));
    }
}
