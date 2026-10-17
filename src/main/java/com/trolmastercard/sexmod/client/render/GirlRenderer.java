package com.trolmastercard.sexmod.client.render;

import com.geckolib.renderer.GeoEntityRenderer;
import com.trolmastercard.sexmod.entity.GirlEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.jspecify.annotations.Nullable;

/** Renders any girl with her {@link GirlModel}. */
public class GirlRenderer<T extends GirlEntity> extends GeoEntityRenderer<T, LivingEntityRenderState> {
    public GirlRenderer(EntityRendererProvider.Context context, String name, HeldWeaponLayer.WeaponPose weaponPose) {
        super(context, new GirlModel<>(name));
        withRenderLayer(PartnerBodyLayer::new);
        withRenderLayer(renderer -> new PaymentItemLayer<>(context, renderer));
        withRenderLayer(renderer -> new HeldWeaponLayer<>(context, renderer, weaponPose));
    }

    @Override
    public void addRenderData(T girl, @Nullable Void relatedObject, LivingEntityRenderState renderState, float partialTick) {
        renderState.addGeckolibData(GirlRenderData.DRESSED, girl.isDressed());
    }
}
