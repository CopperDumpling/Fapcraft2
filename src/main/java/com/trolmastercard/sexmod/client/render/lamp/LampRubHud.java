package com.trolmastercard.sexmod.client.render.lamp;

import com.trolmastercard.sexmod.Fapcraft;
import com.trolmastercard.sexmod.client.ClientHooks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** The HUD is hidden while the local player rubs an Allies lamp (1.12.2 {@code AlliesLampItem.onPre}). */
@EventBusSubscriber(modid = Fapcraft.MODID, value = Dist.CLIENT)
public final class LampRubHud {
    private LampRubHud() {
    }

    @SubscribeEvent
    static void onRenderGui(RenderGuiEvent.Pre event) {
        if (ClientHooks.isRubbingLamp()) event.setCanceled(true);
    }
}
