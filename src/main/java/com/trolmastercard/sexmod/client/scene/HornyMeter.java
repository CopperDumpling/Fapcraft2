package com.trolmastercard.sexmod.client.scene;

import com.trolmastercard.sexmod.Fapcraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * The partner's scene meter (1.12.2 {@code HornyMeterHud}): animation events fill it; when full,
 * jump triggers the climax. Client-local, like 1.12.
 * <ul>
 *   <li>The meter slides down from the top left over 25 ticks, with a SHIFT prompt sliding in
 *   below it (pressed while sneak is held).</li>
 *   <li>The fill rises smoothly, and the two small boxes beside it drain.</li>
 *   <li>Full, a JUMP prompt appears; jumping presses it, hides SHIFT and the fill shoots off the
 *   top of the screen.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Fapcraft.MODID, value = Dist.CLIENT)
public final class HornyMeter {
    private static final Identifier BUTTONS = Fapcraft.id("textures/gui/buttons.png");
    private static final Identifier METER = Fapcraft.id("textures/gui/hornymeter.png");
    private static final float MILLIS_PER_TICK = 50.0F;

    private static double value;
    private static boolean visible;
    /** The drawn fill, easing towards {@link #value}. */
    private static double shown;
    private static float slideIn;
    private static float slideOut;
    /** Jump was pressed on a full meter: the climax is under way. */
    private static boolean released;
    private static long lastFrame;

    private HornyMeter() {
    }

    /** Shows the meter, sliding in empty, unless it is already showing. */
    public static void show() {
        if (visible) return;
        reset();
        visible = true;
    }

    public static void hide() {
        reset();
        visible = false;
    }

    /** Empties the meter; it slides in again (as in 1.12.2). */
    public static void reset() {
        value = 0.0;
        shown = 0.0;
        slideIn = 0.0F;
        slideOut = 0.0F;
        released = false;
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
        long now = Util.getMillis();
        float ticks = Math.min(now - lastFrame, 1000L) / MILLIS_PER_TICK;
        lastFrame = now;
        Minecraft mc = Minecraft.getInstance();
        if (!visible || mc.gui.hud.isHidden()) return;
        slideIn = Math.min(1.0F, slideIn + ticks / 25.0F);

        graphics.pose().pushMatrix();
        graphics.pose().scale(0.35F, 0.35F);
        if (isFull()) {
            if (mc.options.keyJump.isDown()) released = true;
            graphics.blit(RenderPipelines.GUI_TEXTURED, BUTTONS, 240, 160, 0, released ? 162 : 108, 256, 52, 256, 256);
        }
        if (!released) {
            int x = (int) Mth.lerp(slideIn, -200.0F, 98.0F);
            graphics.blit(RenderPipelines.GUI_TEXTURED, BUTTONS, x, 405, 0, mc.options.keyShift.isDown() ? 54 : 0, 158, 54, 256, 256);
        }
        graphics.pose().popMatrix();

        graphics.pose().pushMatrix();
        graphics.pose().scale(0.75F, 0.75F);
        graphics.blit(RenderPipelines.GUI_TEXTURED, METER, 10, (int) Mth.lerp(slideIn, -200.0F, 10.0F), 0, 0, 146, 175, 256, 256);
        shown = Mth.lerp(Math.min(1.0F, ticks), shown, value);
        if (!released) {
            int fillHeight = (int) Mth.lerp(shown, 0.0, 160.0);
            int fillV = (int) Mth.lerp(shown, 167.0, 8.0);
            int fillY = (int) Mth.lerp(slideIn, -45.0, Mth.lerp(shown, 178.0, 18.0));
            graphics.blit(RenderPipelines.GUI_TEXTURED, METER, 67, fillY, 159, fillV, 32, fillHeight, 256, 256);
            double drained = 1.0 - shown;
            int boxY = (int) Mth.lerp(slideIn, -58.0, Mth.lerp(drained, 178.0, 149.0));
            int boxV = (int) Mth.lerp(drained, 169.0, 141.0);
            int boxHeight = (int) Mth.lerp(drained, 1.0, 29.0);
            graphics.blit(RenderPipelines.GUI_TEXTURED, METER, 120, boxY, 212, boxV, 28, boxHeight, 256, 256);
            graphics.blit(RenderPipelines.GUI_TEXTURED, METER, 18, boxY, 212, boxV, 28, boxHeight, 256, 256);
        } else {
            slideOut += ticks / 15.0F;
            graphics.blit(RenderPipelines.GUI_TEXTURED, METER, 67, (int) Mth.lerp(slideOut, 18.0F, -300.0F), 159, 8, 32, 160, 256, 256);
        }
        graphics.pose().popMatrix();
    }
}
