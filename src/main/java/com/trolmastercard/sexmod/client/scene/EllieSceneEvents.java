package com.trolmastercard.sexmod.client.scene;

import static com.trolmastercard.sexmod.client.scene.SceneEvents.play;
import static com.trolmastercard.sexmod.client.scene.SceneEvents.say;

import com.trolmastercard.sexmod.Fapcraft;
import com.trolmastercard.sexmod.client.ClientHooks;
import com.trolmastercard.sexmod.entity.GirlAction;
import com.trolmastercard.sexmod.entity.ellie.EllieEntity;
import com.trolmastercard.sexmod.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Ellie's animation keyframe events (1.12.2 {@code EllieEntity.registerControllers} sound
 * listener). The strip's {@code becomeNude}/{@code stripDone}, the carry intro's lift and loop, and
 * the potion route's timers and {@code sitdownMSG1} line are run by the server instead.
 * <p>
 * Left out, as in 1.12.2: {@code carry_introCam}, {@code carry_introCam2}, {@code stripMSG1},
 * {@code dash*}, {@code sitdownDone} and the {@code *Ready}/{@code cowgirlfastdomDone} events
 * (the animations fire them but the NPC never handled them).
 * <p>
 * While she waits on a bed, a player who steps up to her sees the black transition, which
 * covers the cut into the scene (1.12.2 {@code handleSitIdle}).
 */
@EventBusSubscriber(modid = Fapcraft.MODID, value = Dist.CLIENT)
public final class EllieSceneEvents {
    private EllieSceneEvents() {
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        for (EllieEntity ellie : player.level().getEntitiesOfClass(EllieEntity.class, player.getBoundingBox().inflate(2.0))) {
            if (ellie.getAction() == GirlAction.SITDOWNIDLE && ellie.distanceTo(player) <= 1.5F) TransitionOverlay.start();
        }
    }

    public static void handle(EllieEntity ellie, String event) {
        Minecraft mc = Minecraft.getInstance();
        boolean partner = mc.player != null && ellie.isPartner(mc.player);
        switch (event) {
            // carried
            case "carry_introMSG1" -> {
                say(ellie, partner, "hungry");
                play(ellie, ModSounds.GIRLS_ELLIE_HMPH, 6.0F);
            }
            case "carry_introMSG2" -> {
                say(ellie, partner, "heh");
                play(ellie, ModSounds.GIRLS_ELLIE_GIGGLE.variant(3), 6.0F);
            }
            case "sexUI" -> {
                if (partner) HornyMeter.show();
            }
            case "lipsound" -> {
                // 1.12.2 used Allie's lip sounds here
                play(ellie, ModSounds.GIRLS_ALLIE_LIPSOUND);
                if (partner) HornyMeter.add(0.02);
            }
            case "carry_slowDone" -> ellie.nextCarryVariant();
            case "pound" -> {
                play(ellie, ModSounds.MISC_POUNDING);
                if (partner) HornyMeter.add(0.04);
            }
            case "carry_fastDone" -> {
                if (partner) SceneControls.endFastLoop(ellie, GirlAction.CARRY_FAST, GirlAction.CARRY_SLOW);
            }
            case "cum" -> {
                play(ellie, ModSounds.MISC_INSERTS, 6.0F);
                play(ellie, ModSounds.MISC_POUNDING);
            }
            case "blackScreen" -> {
                if (partner) TransitionOverlay.start();
            }
            case "carry_cumDone" -> {
                if (partner) {
                    HornyMeter.reset();
                    SceneControls.requestStep(ellie, GirlAction.CARRY_CUM, GirlAction.NULL);
                }
            }

            // horny potion: hug and choice
            case "hugMSG2" -> {
                say(ellie, partner, "hmm");
                play(ellie, ModSounds.GIRLS_ELLIE_HMPH.variant(3), 6.0F);
            }
            case "hugMSG3" -> {
                say(ellie, partner, "hey");
                play(ellie, ModSounds.GIRLS_ELLIE_HUH.variant(1));
            }
            case "hugMSG4" -> {
                say(ellie, partner, "mommyhorny");
                play(ellie, ModSounds.GIRLS_ELLIE_MOMMYHORNY, 0.5F);
            }
            case "hugMSG5" -> {
                say(ellie, partner, "whattodo");
                play(ellie, ModSounds.GIRLS_ELLIE_HUH.variant(1), 6.0F);
            }
            case "hugDone" -> {
                if (partner) ClientHooks.openActionMenu(ellie);
            }
            case "hugselectedMSG1" -> {
                say(ellie, partner, "iknow");
                play(ellie, ModSounds.GIRLS_ELLIE_HMPH.variant(3), 6.0F);
            }
            case "hugselectedMSG2" -> {
                say(ellie, partner, "followmedarling");
                play(ellie, ModSounds.GIRLS_ELLIE_GIGGLE.variant(3), 6.0F);
            }
            case "sitdownMSG1" -> play(ellie, ModSounds.GIRLS_ELLIE_COMETOMOMMY, 0.5F);

            // cowgirl
            case "cowgirlStartMSG0" -> play(ellie, ModSounds.GIRLS_ELLIE_GIGGLE.variant(4), 6.0F);
            case "cowgirlStartMSG1" -> {
                say(ellie, partner, "like");
                if (partner) HornyMeter.reset();
            }
            case "cowgirlStartMSG2" -> {
                play(ellie, ModSounds.GIRLS_ELLIE_AHH, 6.0F);
                play(ellie, ModSounds.MISC_POUNDING, 0.75F);
                if (partner) HornyMeter.add(0.02);
            }
            case "cowgirlStartDone" -> {
                if (partner) {
                    SceneControls.requestStep(ellie, GirlAction.COWGIRLSTART, GirlAction.COWGIRLSLOW);
                    HornyMeter.show();
                }
            }
            case "cowgirlfastMSG1" -> {
                play(ellie, ModSounds.GIRLS_ELLIE_AHH, 6.0F);
                play(ellie, ModSounds.MISC_POUNDING, 0.75F);
                if (partner) HornyMeter.add(0.04);
            }
            case "cowgirlfastDone" -> {
                if (partner) SceneControls.endFastLoop(ellie, GirlAction.COWGIRLFAST, GirlAction.COWGIRLSLOW);
            }
            case "cowgirlcumMSG1" -> {
                play(ellie, ModSounds.GIRLS_ELLIE_AHH, 6.0F);
                play(ellie, ModSounds.MISC_POUNDING, 0.75F);
            }
            case "cowgirlcumMSG2" -> {
                play(ellie, ModSounds.GIRLS_ELLIE_MOAN.variant(5), 3.0F);
                play(ellie, ModSounds.MISC_POUNDING, 0.75F);
            }
            case "cowgirlcumMSG3" -> play(ellie, ModSounds.MISC_POUNDING, 0.75F);
            case "cowgirlcumMSG4" -> {
                if (partner) HornyMeter.hide();
            }
            case "cowgirlcumMSG5", "missionary_cumMSG2" -> {
                play(ellie, ModSounds.GIRLS_ELLIE_GOODBOY, 0.5F);
                say(ellie, partner, "goodboy");
            }
            case "cowgirlcumMSG6" -> {
                if (partner) TransitionOverlay.start();
            }
            case "cowgirlcumDone", "missionary_cumDone" -> {
                if (partner) {
                    HornyMeter.reset();
                    SceneControls.requestStep(ellie, ellie.getAction(), GirlAction.NULL);
                }
            }

            // missionary
            case "openSexUi" -> {
                if (partner) HornyMeter.show();
            }
            case "missionary_slowMSG1" -> {
                play(ellie, ModSounds.MISC_POUNDING);
                // a moan one time in four, otherwise "ahh"
                boolean moan = ellie.getRandom().nextBoolean() && ellie.getRandom().nextBoolean();
                play(ellie, moan ? ModSounds.GIRLS_ELLIE_MOAN : ModSounds.GIRLS_ELLIE_AHH, 6.0F);
                if (partner) HornyMeter.add(0.02);
            }
            case "missionary_fastMSG1" -> {
                play(ellie, ModSounds.MISC_POUNDING);
                // "ahh" one time in four, otherwise a moan
                boolean ahh = !ellie.getRandom().nextBoolean() && !ellie.getRandom().nextBoolean();
                play(ellie, ahh ? ModSounds.GIRLS_ELLIE_AHH : ModSounds.GIRLS_ELLIE_MOAN, 6.0F);
                if (partner) HornyMeter.add(0.05);
            }
            case "missionary_startDone" -> {
                if (partner) {
                    SceneControls.requestStep(ellie, GirlAction.MISSIONARY_START, GirlAction.MISSIONARY_SLOW);
                    HornyMeter.show();
                }
            }
            case "missionary_fastDone" -> {
                if (partner) SceneControls.endFastLoop(ellie, GirlAction.MISSIONARY_FAST, GirlAction.MISSIONARY_SLOW);
            }
            case "bedRustle" -> {
                play(ellie, ModSounds.MISC_POUNDING);
                play(ellie, ModSounds.MISC_BEDRUSTLE.variant(0));
            }
            case "bedRustle1" -> play(ellie, ModSounds.MISC_BEDRUSTLE.variant(1));
            case "missionary_cumMSG1" -> play(ellie, ModSounds.GIRLS_ELLIE_AHH, 6.0F);
            default -> SceneEvents.handleShared(ellie, event);
        }
    }
}
