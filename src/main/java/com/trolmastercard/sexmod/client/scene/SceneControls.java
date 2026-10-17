package com.trolmastercard.sexmod.client.scene;

import com.mojang.blaze3d.platform.InputConstants;
import com.trolmastercard.sexmod.Fapcraft;
import com.trolmastercard.sexmod.entity.GirlAction;
import com.trolmastercard.sexmod.entity.GirlEntity;
import com.trolmastercard.sexmod.network.LeaveScenePayload;
import com.trolmastercard.sexmod.network.SceneStepPayload;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Input;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.MovementInputUpdateEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * The local player's side of a scene (1.12.2 {@code HandlePlayerMovement} + the leave key):
 * <ul>
 *   <li>movement is frozen while the player is a girl's partner;</li>
 *   <li>holding sneak asks for her next step (e.g. suck → thrust);</li>
 *   <li>jump with a full meter asks for the climax;</li>
 *   <li>the leave key (right shift) ends the scene;</li>
 *   <li>partners aren't drawn as players (nor their first-person hand) during scene steps: her
 *   model draws their body.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Fapcraft.MODID, value = Dist.CLIENT)
public final class SceneControls {
    public static final KeyMapping.Category CATEGORY = new KeyMapping.Category(Fapcraft.id("sexmod"));
    public static final KeyMapping LEAVE_SCENE = new KeyMapping("key.sexmod.leave_scene", InputConstants.KEY_RSHIFT, CATEGORY);

    /** The step last requested for the girl's current step, so a held key sends it once. */
    private static @Nullable GirlAction requestedFrom;
    private static boolean wasInScene;

    private SceneControls() {
    }

    @EventBusSubscriber(modid = Fapcraft.MODID, value = Dist.CLIENT)
    static final class Registration {
        @SubscribeEvent
        static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            event.registerCategory(CATEGORY);
            event.register(LEAVE_SCENE);
        }
    }

    /** The girl the local player is currently in a scene with, if any. */
    public static @Nullable GirlEntity currentGirl() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return null;
        for (GirlEntity girl : player.level().getEntitiesOfClass(GirlEntity.class, player.getBoundingBox().inflate(8.0))) {
            if (girl.isPartner(player)) return girl;
        }
        return null;
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        GirlEntity girl = currentGirl();
        if (girl == null) {
            if (wasInScene) {
                // scene ended (naturally, left, or cancelled on the server)
                HornyMeter.hide();
                HornyMeter.reset();
                wasInScene = false;
            }
            requestedFrom = null;
            while (LEAVE_SCENE.consumeClick()) {
                // nothing to leave
            }
            return;
        }
        wasInScene = true;
        while (LEAVE_SCENE.consumeClick()) {
            ClientPacketDistributor.sendToServer(LeaveScenePayload.INSTANCE);
        }
        GirlAction action = girl.getAction();
        if (requestedFrom != null && requestedFrom != action) requestedFrom = null;
        if (requestedFrom != null || mc.gui.screen() != null) return;
        GirlAction next = null;
        if (mc.options.keyShift.isDown()) next = girl.sneakStep(action);
        if (next == null && mc.options.keyJump.isDown() && HornyMeter.isFull()) next = girl.climaxStep(action);
        if (next != null) requestStep(girl, action, next);
    }

    /**
     * A fast loop's {@code *Done} event: back to the slow step, unless sneak is still held. 1.12.2
     * dropped to the slow step and straight back on the client within a tick; through the server
     * that round trip showed the slow step for a moment every loop, so the fast loop just carries on.
     */
    public static void endFastLoop(GirlEntity girl, GirlAction fast, GirlAction slow) {
        if (Minecraft.getInstance().options.keyShift.isDown() && girl.sneakStep(slow) == fast) return;
        requestStep(girl, fast, slow);
    }

    /** Sends a step request once per step (also used by animation events). */
    public static void requestStep(GirlEntity girl, GirlAction from, GirlAction to) {
        if (requestedFrom == from) return;
        requestedFrom = from;
        ClientPacketDistributor.sendToServer(new SceneStepPayload(girl.getId(), from, to));
    }

    @SubscribeEvent
    static void onMovementInput(MovementInputUpdateEvent event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        GirlEntity current = currentGirl();
        boolean frozen = current != null && !current.isSolo();
        if (frozen && !(player.input instanceof FrozenInput)) {
            // aiStep reads the field after this event, so the swap applies this tick. Sneak and
            // jump are read straight from the key mappings in onClientTick.
            player.input = new FrozenInput();
        } else if (!frozen && player.input instanceof FrozenInput) {
            player.input = new KeyboardInput(Minecraft.getInstance().options);
        }
        if (frozen) {
            event.getInput().keyPresses = Input.EMPTY;
            // momentum from before the freeze (walking or sprinting in) would otherwise slide the
            // partner off the spot the scene placed them on
            player.setSprinting(false);
            player.setDeltaMovement(0.0, player.getDeltaMovement().y, 0.0);
        }
    }

    /** An input with no keys and no movement (the base {@code ClientInput} never updates). */
    private static final class FrozenInput extends ClientInput {
    }

    /** The first-person hand and held item are hidden too while her model draws the partner. */
    @SubscribeEvent
    static void onRenderHand(RenderHandEvent event) {
        GirlEntity girl = currentGirl();
        if (girl != null && girl.getAction().hasPlayer) event.setCanceled(true);
    }

    @SubscribeEvent
    static void onRenderPlayer(RenderPlayerEvent.Pre<?> event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(event.getRenderState().id) instanceof net.minecraft.world.entity.player.Player player)) return;
        for (GirlEntity girl : mc.level.getEntitiesOfClass(GirlEntity.class, player.getBoundingBox().inflate(8.0))) {
            if (girl.isPartner(player) && girl.getAction().hasPlayer) {
                event.setCanceled(true);
                return;
            }
        }
    }
}
