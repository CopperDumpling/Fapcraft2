package com.trolmastercard.sexmod.client.scene;

import com.trolmastercard.sexmod.Fapcraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;

/**
 * The slide-across black transition that covers a scene's climax (1.12.2 {@code BeeScreen}, fired
 * by the {@code bjcBlackScreen} keyframe). A patterned edge, a solid black band and its mirror
 * sweep right to left. The meter is hidden once the screen is covered.
 */
@EventBusSubscriber(modid = Fapcraft.MODID, value = Dist.CLIENT)
public final class TransitionOverlay {
    private static final Identifier EDGE = Fapcraft.id("textures/gui/transitionscreen.png");
    private static final Identifier EDGE_MIRRORED = Fapcraft.id("textures/gui/mirroredtransitionscreen.png");
    /**
     * GUI width the 1.12.2 layout fits: its offsets and band widths were tuned per GUI scale
     * setting (x1.5 at scale 2, x2 at scale 1), which comes to about the screen width / 640.
     */
    private static final float DESIGN_WIDTH = 640.0F;
    private static final double SPEED = 0.75;
    private static final double MIDPOINT = 30.0;
    private static final double END = 69.0;

    private static boolean visible;
    private static double timer;

    private TransitionOverlay() {
    }

    /** Starts the transition; ignored while one is playing (as in 1.12.2). */
    public static void start() {
        visible = true;
    }

    public static boolean isActive() {
        return visible;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        if (!visible) return;
        timer += SPEED;
        if (timer > MIDPOINT) HornyMeter.hide();
        if (timer > END) {
            timer = 0.0;
            visible = false;
        }
    }

    @SubscribeEvent
    static void onRegisterLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(Fapcraft.id("scene_transition"), TransitionOverlay::render);
    }

    private static void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (!visible) return;
        float scale = graphics.guiWidth() / DESIGN_WIDTH;
        // advances 0.75 per tick; interpolated so the band moves every frame
        double time = timer + SPEED * delta.getGameTimeDeltaPartialTick(false);
        // lerp(-900, 600, 0.5cos(t/25) + 0.5): starts off to the right, ends off to the left
        double blend = 0.5 * Math.cos(time / 25.0) + 0.5;
        int offset = (int) (-900.0 + (600.0 - -900.0) * blend);
        int scroll = (int) (time * 1.5);
        graphics.pose().pushMatrix();
        graphics.pose().scale(scale, scale);
        // 1.12.2 stacked three tiles; more only if the window is unusually tall
        int rows = Math.max(3, Mth.ceil(graphics.guiHeight() / (256.0F * scale)));
        for (int row = 0; row < rows; row++) {
            int y = row * 256;
            graphics.blit(RenderPipelines.GUI_TEXTURED, EDGE, offset, y, 0, scroll, 256, 256, 256, 256);
            graphics.blit(RenderPipelines.GUI_TEXTURED, EDGE_MIRRORED, offset + 600, y, 0, scroll, 256, 256, 256, 256);
            graphics.fill(offset + 200, y, offset + 600, y + 256, 0xFF000000);
        }
        graphics.pose().popMatrix();
    }
}
