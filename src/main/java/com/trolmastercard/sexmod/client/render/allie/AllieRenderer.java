package com.trolmastercard.sexmod.client.render.allie;

import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.base.RenderPassInfo;
import com.trolmastercard.sexmod.client.render.ArmorLayer;
import com.trolmastercard.sexmod.client.render.GirlModel;
import com.trolmastercard.sexmod.client.render.GirlRenderer;
import com.trolmastercard.sexmod.client.render.HeldWeaponLayer;
import com.trolmastercard.sexmod.entity.allie.AllieEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.jspecify.annotations.Nullable;

/**
 * Allie (1.12.2 {@code AllieRenderer}): one model for every outfit, portal particles off her tail,
 * and when she vanishes she shrinks while rising.
 */
public class AllieRenderer extends GirlRenderer<AllieEntity> {
    private static final DataTicket<Float> SCALE = DataTicket.create("sexmod_allie_scale", Float.class);

    public AllieRenderer(EntityRendererProvider.Context context, HeldWeaponLayer.WeaponPose weaponPose) {
        super(context, new GirlModel<>("allie", "allie", "allie"), weaponPose, ArmorLayer.Bones.NONE);
        withRenderLayer(AllieTailLayer::new);
    }

    @Override
    public void addRenderData(AllieEntity allie, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
        super.addRenderData(allie, relatedObject, renderState, partialTick);
        renderState.addGeckolibData(SCALE, allie.vanishScale(partialTick));
    }

    @Override
    public void scaleModelForRender(RenderPassInfo<LivingEntityRenderState> pass, float widthScale, float heightScale) {
        float scale = pass.renderState().getOrDefaultGeckolibData(SCALE, 1.0F);
        super.scaleModelForRender(pass, widthScale * scale, heightScale * scale);
    }

    @Override
    public void adjustRenderPose(RenderPassInfo<LivingEntityRenderState> pass) {
        super.adjustRenderPose(pass);
        float scale = pass.renderState().getOrDefaultGeckolibData(SCALE, 1.0F);
        // 1.12.2: translate(0, 3 - 3 * scale, 0) after scaling
        if (scale < 1.0F) pass.poseStack().translate(0.0F, 3.0F - scale * 3.0F, 0.0F);
    }
}
