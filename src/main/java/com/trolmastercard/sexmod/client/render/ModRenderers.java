package com.trolmastercard.sexmod.client.render;

import com.trolmastercard.sexmod.Fapcraft;
import com.trolmastercard.sexmod.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import org.joml.Vector3f;

@EventBusSubscriber(modid = Fapcraft.MODID, value = Dist.CLIENT)
public final class ModRenderers {
    /** 1.12.2 {@code JennyEntity} constructor values. */
    private static final HeldWeaponLayer.WeaponPose JENNY_WEAPON = new HeldWeaponLayer.WeaponPose(140.0F, 50.0F, new Vector3f(0.0F, -0.03F, -0.2F), 140.0F);
    private ModRenderers() {
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.JENNY.get(), ctx -> new GirlRenderer<>(ctx, "jenny", JENNY_WEAPON));
    }
}
