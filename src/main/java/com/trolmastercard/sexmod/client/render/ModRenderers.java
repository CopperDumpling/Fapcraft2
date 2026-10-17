package com.trolmastercard.sexmod.client.render;

import com.trolmastercard.sexmod.Fapcraft;
import com.trolmastercard.sexmod.registry.ModEntities;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

@EventBusSubscriber(modid = Fapcraft.MODID, value = Dist.CLIENT)
public final class ModRenderers {
    private ModRenderers() {
    }

    @SubscribeEvent
    static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.JENNY.get(), ctx -> new GirlRenderer<>(ctx, "jenny"));
    }
}
