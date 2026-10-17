package com.trolmastercard.sexmod.client.scene;

import static com.trolmastercard.sexmod.client.scene.SceneEvents.play;

import com.trolmastercard.sexmod.entity.GirlAction;
import com.trolmastercard.sexmod.entity.slime.SlimeEntity;
import com.trolmastercard.sexmod.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;

/**
 * The slime's animation keyframe events (1.12.2 {@code SlimeEntity.registerControllers} sound
 * listener): vanilla slime sounds for her hops and scenes. Undressing and getting down on all
 * fours are timed on the server.
 * <p>
 * Left out, as in Jenny's port: the {@code *Ready} events (1.12.2 only restarted the loop while
 * a key was held). The hop animations' {@code SVS...} markers were never handled in 1.12.2.
 */
public final class SlimeSceneEvents {
    /** Fast doggy beats so far; every other one squelches (1.12.2 {@code blinkTicks}). */
    private static int fastBeats;

    private SlimeSceneEvents() {
    }

    public static void handle(SlimeEntity slime, String event) {
        Minecraft mc = Minecraft.getInstance();
        boolean partner = mc.player != null && slime.isPartner(mc.player);
        switch (event) {
            // hopping
            case "jumpStart" -> vanilla(slime, SoundEvents.SLIME_JUMP, 1.0F);
            case "jumpStartDone" -> slime.setHop(SlimeEntity.Hop.AIR);
            case "jumpEndSound" -> vanilla(slime, SoundEvents.SLIME_SQUISH, 1.0F);
            case "jumpEndDone" -> slime.setHop(SlimeEntity.Hop.IDLE);

            case "sexUiOn" -> {
                if (partner) HornyMeter.show();
            }

            // blowjob
            case "bjiMSG11" -> {
                vanilla(slime, SoundEvents.SLIME_SQUISH, 0.5F);
                if (partner) HornyMeter.add(0.02);
            }
            case "bjiMSG12" -> {
                if (slime.getRandom().nextInt(5) == 0) vanilla(slime, SoundEvents.SLIME_JUMP, 0.5F);
                vanilla(slime, SoundEvents.SLIME_SQUISH, 0.5F);
                if (partner) HornyMeter.add(0.02);
            }
            case "bjtMSG1" -> {
                vanilla(slime, SoundEvents.SLIME_BLOCK_HIT, 1.0F);
                vanilla(slime, SoundEvents.SLIME_DEATH, 1.0F);
                if (partner) HornyMeter.add(0.04);
            }
            case "bjtDone" -> {
                if (partner) SceneControls.endFastLoop(slime, GirlAction.THRUSTBLOWJOB, GirlAction.SUCKBLOWJOB);
            }
            case "bjcMSG1" -> vanilla(slime, SoundEvents.SLIME_JUMP, 1.0F);
            case "bjcMSG2" -> {
                vanilla(slime, SoundEvents.SLIME_JUMP, 1.0F);
                if (partner) HornyMeter.hide();
            }
            case "bjcBlackScreen" -> {
                if (partner) TransitionOverlay.start();
            }
            case "bjcDone", "doggyCumDone" -> {
                if (partner) {
                    HornyMeter.reset();
                    SceneControls.requestStep(slime, slime.getAction(), GirlAction.NULL);
                }
            }

            // doggy
            case "doggyGoOnBedMSG1" -> vanilla(slime, SoundEvents.SLIME_SQUISH, 1.0F);
            case "doggystartMSG1" -> play(slime, ModSounds.MISC_TOUCH.variant(0));
            case "doggystartMSG2" -> play(slime, ModSounds.MISC_TOUCH.variant(1));
            case "doggystartMSG3" -> vanilla(slime, SoundEvents.SLIME_SQUISH, 0.25F);
            case "doggystartMSG4" -> play(slime, ModSounds.MISC_SMALLINSERTS, 1.5F);
            case "doggystartMSG5" -> {
                play(slime, ModSounds.MISC_POUNDING, 0.33F);
                vanilla(slime, SoundEvents.SLIME_BLOCK_HIT, 1.0F);
            }
            case "doggystartDone" -> {
                if (partner) {
                    SceneControls.requestStep(slime, GirlAction.DOGGYSTART, GirlAction.DOGGYSLOW);
                    HornyMeter.show();
                }
            }
            case "doggyslowMSG1" -> {
                play(slime, ModSounds.MISC_POUNDING, 0.33F);
                if (slime.getRandom().nextInt(4) == 0) {
                    vanilla(slime, slime.getRandom().nextBoolean() ? SoundEvents.SLIME_JUMP : SoundEvents.SLIME_SQUISH, 1.0F);
                } else {
                    vanilla(slime, SoundEvents.SLIME_BLOCK_HIT, 1.0F);
                }
                if (partner) HornyMeter.add(0.02);
            }
            case "doggyslowMSG2" -> vanilla(slime, SoundEvents.SLIME_BLOCK_HIT, 1.0F);
            case "doggyfastMSG1" -> {
                play(slime, ModSounds.MISC_POUNDING, 0.75F);
                if (partner) HornyMeter.add(0.04);
                if (++fastBeats % 2 == 0) {
                    vanilla(slime, slime.getRandom().nextBoolean() ? SoundEvents.SLIME_JUMP : SoundEvents.SLIME_SQUISH, 1.0F);
                } else {
                    vanilla(slime, SoundEvents.SLIME_BLOCK_HIT, 1.0F);
                }
            }
            case "doggyfastDone" -> {
                if (partner) SceneControls.endFastLoop(slime, GirlAction.DOGGYFAST, GirlAction.DOGGYSLOW);
            }
            case "doggycumMSG1" -> {
                play(slime, ModSounds.MISC_CUMINFLATION.variant(0), 4.0F);
                play(slime, ModSounds.MISC_POUNDING, 2.0F);
                vanilla(slime, SoundEvents.SLIME_DEATH, 1.0F);
            }
            default -> SceneEvents.handleShared(slime, event);
        }
    }

    private static void vanilla(SlimeEntity slime, Holder<SoundEvent> sound, float volume) {
        play(slime, sound, volume);
    }

    private static void vanilla(SlimeEntity slime, SoundEvent sound, float volume) {
        slime.level().playLocalSound(slime, sound, slime.getSoundSource(), volume, 1.0F);
    }
}
