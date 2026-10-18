package com.trolmastercard.sexmod.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.trolmastercard.sexmod.entity.GirlEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** Renders any girl with her {@link GirlModel}. */
public class GirlRenderer<T extends GirlEntity> extends GeoEntityRenderer<T, LivingEntityRenderState> {
    public GirlRenderer(EntityRendererProvider.Context context, GirlModel<T> model, HeldWeaponLayer.WeaponPose weaponPose, ArmorLayer.Bones armorBones) {
        super(context, model);
        withRenderLayer(HeadLookLayer::new);
        withRenderLayer(PartnerBodyLayer::new);
        withRenderLayer(BoyCamLayer::new);
        withRenderLayer(renderer -> new ArmorLayer<>(renderer, armorBones));
        withRenderLayer(renderer -> new PaymentItemLayer<>(context, renderer));
        withRenderLayer(renderer -> new HeldWeaponLayer<>(context, renderer, weaponPose));
    }

    /**
     * Back faces culled, as 1.12.2 drew the girls (its renderer kept culling on): their models
     * overlap cubes a hair apart, and with both sides drawn the inner faces flicker through.
     */
    @Override
    public @Nullable RenderType getRenderType(LivingEntityRenderState renderState, Identifier texture) {
        return RenderTypes.entityCutoutCull(texture);
    }

    @Override
    public void addRenderData(T girl, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
        renderState.addGeckolibData(GirlRenderData.DRESSED, girl.isDressed());
    }
}
