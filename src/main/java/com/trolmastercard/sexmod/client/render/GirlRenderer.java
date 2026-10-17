package com.trolmastercard.sexmod.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.trolmastercard.sexmod.entity.GirlEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.jspecify.annotations.Nullable;

/** Renders any girl with her {@link GirlModel}. */
public class GirlRenderer<T extends GirlEntity> extends GeoEntityRenderer<T, LivingEntityRenderState> {
    public GirlRenderer(EntityRendererProvider.Context context, GirlModel<T> model, HeldWeaponLayer.WeaponPose weaponPose, ArmorLayer.Bones armorBones) {
        super(context, model);
        withRenderLayer(PartnerBodyLayer::new);
        withRenderLayer(BoyCamLayer::new);
        withRenderLayer(renderer -> new ArmorLayer<>(renderer, armorBones));
        withRenderLayer(renderer -> new PaymentItemLayer<>(context, renderer));
        withRenderLayer(renderer -> new HeldWeaponLayer<>(context, renderer, weaponPose));
    }

    @Override
    public void addRenderData(T girl, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
        renderState.addGeckolibData(GirlRenderData.DRESSED, girl.isDressed());
    }
}
