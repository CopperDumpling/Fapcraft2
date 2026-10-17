package com.trolmastercard.sexmod.client.scene;

import com.trolmastercard.sexmod.Fapcraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * The partner's scene meter (1.12.2 {@code HornyMeterHud}): animation events fill it; when full,
 * jump triggers the climax. Client-local, like 1.12. A plain bar for now; the 1.12 artwork and
 * slide-in animation come with Phase 3.
 */
@EventBusSubscriber(modid = Fapcraft.MODID, value = Dist.CLIENT)
public final class HornyMeter {
    private static double value;
    private static boolean visible;

    private HornyMeter() {
    }

    public static void show() {
        visible = true;
    }

    public static void hide() {
        visible = false;
    }

    public static void reset() {
        value = 0.0;
    }

    public static void add(double amount) {
        value = Math.min(1.0, value + amount);
    }

    public static boolean isFull() {
        return value >= 1.0;
    }

    @SubscribeEvent
    static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Fapcraft.id("horny_meter"), HornyMeter::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (!visible || Minecraft.getInstance().gui.hud.isHidden()) return;
        int x = 12;
        int top = 20;
        int height = 120;
        int filled = (int) Math.round(height * value);
        graphics.fill(x - 1, top - 1, x + 13, top + height + 1, 0xC0000000);
        graphics.fill(x, top + height - filled, x + 12, top + height, isFull() ? 0xFFFF5FA0 : 0xFFE0407A);
        if (isFull()) {
            graphics.text(Minecraft.getInstance().font, Component.translatable("sexmod.scene.climax_hint"), x + 18, top + height - 8, 0xFFFFFFFF);
        }
    }
}
