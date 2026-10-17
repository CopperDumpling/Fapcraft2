package com.trolmastercard.sexmod.client.scene;

import static com.trolmastercard.sexmod.client.scene.SceneEvents.play;
import static com.trolmastercard.sexmod.client.scene.SceneEvents.say;

import com.trolmastercard.sexmod.Fapcraft;
import com.trolmastercard.sexmod.client.ClientHooks;
import com.trolmastercard.sexmod.entity.GirlAction;
import com.trolmastercard.sexmod.entity.bia.BiaEntity;
import com.trolmastercard.sexmod.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Bia's animation keyframe events (1.12.2 {@code BiaEntity.registerControllers} sound listener).
 * The strip's outfit swap and end, the talk answer's end, the bed preparation, and the timed doggy
 * stretches are run by the server instead.
 * <p>
 * Every client tick: a player stepping up to her on the bed sees the black transition, and her
 * partner's meter shows during the doggy intro once the transition is over (1.12.2 {@code onUpdate}).
 * <p>
 * Left out, as in 1.12.2: {@code openSexUI} (1.12.2 listened for {@code sexUiOn}), {@code idle},
 * {@code startStrip}, and the talk lines the shorter talk animation no longer fires.
 */
@EventBusSubscriber(modid = Fapcraft.MODID, value = Dist.CLIENT)
public final class BiaSceneEvents {
    private BiaSceneEvents() {
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        for (BiaEntity bia : player.level().getEntitiesOfClass(BiaEntity.class, player.getBoundingBox().inflate(2.0))) {
            GirlAction action = bia.getAction();
            if ((action == GirlAction.ANAL_WAIT || action == GirlAction.SITDOWNIDLE) && bia.distanceTo(player) <= 1.0F) TransitionOverlay.start();
            if (action == GirlAction.PRONE_DOGGY_INTRO && bia.isPartner(player) && !TransitionOverlay.isActive()) HornyMeter.show();
        }
    }

    public static void handle(BiaEntity bia, String event) {
        Minecraft mc = Minecraft.getInstance();
        boolean partner = mc.player != null && bia.isPartner(mc.player);
        boolean sneaking = partner && mc.options.keyShift.isDown();
        switch (event) {
            case "stripMSG1" -> {
                say(bia, partner, "hihi");
                play(bia, ModSounds.GIRLS_BIA_GIGGLE);
            }

            // talk
            case "talk_hornyMSG1" -> {
                say(bia, partner, "heya");
                play(bia, ModSounds.GIRLS_BIA_HEY);
            }
            case "talk_hornyMSG4" -> {
                say(bia, partner, "fun");
                play(bia, ModSounds.GIRLS_BIA_HUH.variant(0));
            }
            case "talk_hornyDone" -> {
                if (partner) {
                    SceneControls.requestStep(bia, GirlAction.TALK_HORNY, GirlAction.TALK_IDLE);
                    ClientHooks.openActionMenu(bia);
                }
            }
            case "talk_responseMSG1" -> {
                say(bia, partner, "huh");
                play(bia, ModSounds.GIRLS_BIA_HUH.variant(2));
            }
            case "talk_responseMSG2" -> {
                say(bia, partner, "iuhm");
                play(bia, ModSounds.GIRLS_BIA_BREATH.variant(1));
            }
            case "talk_responseMSG3" -> {
                say(bia, partner, "yes");
                play(bia, ModSounds.GIRLS_BIA_GIGGLE.variant(0));
            }

            // headpat
            case "headpatMSG1" -> {
                say(bia, partner, "headpats");
                play(bia, ModSounds.GIRLS_BIA_BREATH.variant(0));
            }
            case "headpatMSG2" -> {
                say(bia, partner, "hmm");
                play(bia, ModSounds.GIRLS_BIA_MMM.variant(0));
            }
            case "headpatMSG3" -> {
                say(bia, partner, "huh2");
                play(bia, ModSounds.GIRLS_BIA_HUH.variant(0));
            }
            case "headpatMSG4" -> {
                say(bia, partner, "thankyou");
                play(bia, ModSounds.GIRLS_BIA_GIGGLE.variant(1));
            }
            case "headpatDone" -> {
                if (partner) SceneControls.requestStep(bia, GirlAction.HEAD_PAT, GirlAction.NULL);
            }

            // on the bed
            case "anal_prepareMSG1" -> play(bia, ModSounds.MISC_PLOB.variant(0));
            case "anal_prepareMSG2" -> play(bia, ModSounds.MISC_BEDRUSTLE.variant(0));
            case "sitdownMSG1" -> play(bia, ModSounds.GIRLS_BIA_BREATH);

            // anal
            case "anal_startMSG1" -> {
                play(bia, ModSounds.GIRLS_BIA_MMM.variant(3));
                play(bia, ModSounds.MISC_POUNDING.variant(34));
            }
            case "anal_startMSG2", "anal_slowMSG1", "anal_fastMSG1" -> {
                // 1.12.2's fast beat fell through into the slow one: it fills the meter twice
                if (partner) HornyMeter.add("anal_fastMSG1".equals(event) ? 0.04 : 0.02);
                play(bia, ModSounds.MISC_POUNDING, 0.5F);
                play(bia, ModSounds.GIRLS_BIA_AHH);
            }
            case "anal_startDone" -> {
                if (partner) {
                    SceneControls.requestStep(bia, GirlAction.ANAL_START, GirlAction.ANAL_SLOW);
                    HornyMeter.show();
                }
            }
            case "anal_fastDone" -> {
                if (partner) SceneControls.endFastLoop(bia, GirlAction.ANAL_FAST, GirlAction.ANAL_SLOW);
            }
            case "anal_cumMSG2" -> play(bia, ModSounds.GIRLS_BIA_AHH);
            case "anal_cumBlackScreen", "blackScreen" -> {
                if (partner) TransitionOverlay.start();
            }
            case "anal_cumDone", "doggy_cumDone" -> {
                if (partner) {
                    HornyMeter.reset();
                    SceneControls.requestStep(bia, bia.getAction(), GirlAction.NULL);
                }
            }

            // prone doggy
            case "slide" -> {
                play(bia, ModSounds.MISC_SLIDE);
                if (partner) HornyMeter.add(0.005);
            }
            case "pound" -> play(bia, ModSounds.MISC_POUNDING);
            case "doggyMoan" -> {
                play(bia, bia.getRandom().nextBoolean() ? ModSounds.GIRLS_BIA_AHH : ModSounds.GIRLS_BIA_MMM);
                if (partner) HornyMeter.add(0.04);
            }
            case "doggySwitch" -> {
                if (sneaking) SceneControls.requestStep(bia, GirlAction.PRONE_DOGGY_SOFT, GirlAction.PRONE_DOGGY_HARD);
            }
            case "doggyReset" -> {
                // holding sneak keeps restarting the hard stretch's opening thrusts
                if (sneaking) bia.restartActionAnimation();
            }
            case "cum" -> play(bia, ModSounds.MISC_INSERTS, 6.0F);
            case "orgasm1" -> play(bia, ModSounds.GIRLS_BIA_MMM.variant(6));
            case "orgasm2" -> play(bia, ModSounds.GIRLS_BIA_MMM.variant(7));
            default -> SceneEvents.handleShared(bia, event);
        }
    }
}
