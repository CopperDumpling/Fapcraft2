package com.trolmastercard.sexmod.client.scene;

import static com.trolmastercard.sexmod.client.scene.SceneEvents.play;
import static com.trolmastercard.sexmod.client.scene.SceneEvents.say;
import static com.trolmastercard.sexmod.client.scene.SceneEvents.sayAsPartner;

import com.trolmastercard.sexmod.entity.GirlAction;
import com.trolmastercard.sexmod.entity.jenny.JennyEntity;
import com.trolmastercard.sexmod.network.ScenePosePayload;
import com.trolmastercard.sexmod.sound.ModSounds;
import net.minecraft.client.Minecraft;
import com.trolmastercard.sexmod.entity.SceneOption;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Jenny's animation keyframe events (1.12.2 {@code JennyEntity.registerControllers} sound
 * listener), for the scenes ported so far.
 * <p>
 * Every client that renders her plays the sounds. Only the partner's client changes the meter,
 * asks for steps and poses, and broadcasts dialogue (once, through the server).
 * <p>
 * Left out on purpose: {@code drawItems}/{@code removeItems} (the animation fires them but
 * 1.12.2 never handled them, so no items were ever drawn) and the {@code *Ready} events, which
 * only reset the animation offset in 1.12.2 so a held key restarts the loop.
 */
public final class JennySceneEvents {
    private JennySceneEvents() {
    }

    /** Fast-stretch beats so far; her lines alternate every other beat (1.12.2 {@code ag}). */
    private static int fastBeats;

    public static void handle(JennyEntity jenny, String event) {
        Minecraft mc = Minecraft.getInstance();
        boolean partner = mc.player != null && jenny.isPartner(mc.player);
        switch (event) {
            // payment
            case "paymentMSG1" -> {
                say(jenny, partner, "huh");
                play(jenny, ModSounds.GIRLS_JENNY_HUH.variant(1));
            }
            case "paymentMSG2" -> {
                play(jenny, ModSounds.MISC_PLOB.variant(0), 0.5F);
                SceneOption option = jenny.currentOption();
                if (option != null) sayAsPartner(jenny, partner, option.requestLine());
            }
            case "paymentMSG3" -> {
                say(jenny, partner, "hehe");
                play(jenny, ModSounds.GIRLS_JENNY_GIGGLE);
            }
            case "paymentMSG4" -> play(jenny, ModSounds.MISC_PLOB.variant(0), 0.25F);
            case "paymentDone" -> {
                // the server moves on to the option she was asked for; `to` is ignored there
                if (partner) SceneControls.requestStep(jenny, GirlAction.PAYMENT, GirlAction.PAYMENT);
            }
            case "sexUiOn" -> {
                if (partner) HornyMeter.show();
            }

            // strip (the outfit change and the end are timed on the server)
            case "stripMSG1" -> {
                say(jenny, partner, "hihi");
                play(jenny, ModSounds.GIRLS_JENNY_GIGGLE);
            }

            // blowjob intro
            case "bjiMSG1" -> {
                say(jenny, partner, "blowjobtext1");
                play(jenny, ModSounds.GIRLS_JENNY_MMM.variant(8));
                if (partner) HornyMeter.reset();
            }
            case "bjiMSG2" -> {
                say(jenny, partner, "blowjobtext2");
                play(jenny, ModSounds.GIRLS_JENNY_LIGHTBREATHING.variant(8));
            }
            case "bjiMSG3" -> {
                say(jenny, partner, "blowjobtext3");
                play(jenny, ModSounds.GIRLS_JENNY_AFTERSESSIONMOAN.variant(0));
            }
            case "bjiMSG4" -> play(jenny, ModSounds.MISC_BELLJINGLE.variant(0));
            case "bjiMSG5" -> {
                say(jenny, partner, "blowjobtext4");
                play(jenny, ModSounds.GIRLS_JENNY_HMPH.variant(1), 0.5F);
                if (partner) HornyMeter.reset();
            }
            case "bjiMSG6" -> {
                say(jenny, partner, "blowjobtext5");
                play(jenny, ModSounds.GIRLS_JENNY_LIGHTBREATHING.variant(8));
            }
            case "bjiMSG7" -> {
                say(jenny, partner, "blowjobtext6");
                play(jenny, ModSounds.GIRLS_JENNY_GIGGLE.variant(4));
            }
            case "bjiMSG8" -> {
                sayAsPartner(jenny, partner, "blowjobtext7");
                play(jenny, ModSounds.MISC_PLOB.variant(0), 0.5F);
            }
            case "bjiMSG9" -> {
                say(jenny, partner, "blowjobtext8");
                play(jenny, ModSounds.GIRLS_JENNY_GIGGLE.variant(2));
            }
            case "bjiMSG10" -> {
                if (partner) ClientPacketDistributor.sendToServer(new ScenePosePayload(jenny.getId(), JennyEntity.POSE_BLOWJOB));
            }
            case "bjiDone" -> {
                if (partner) {
                    SceneControls.requestStep(jenny, GirlAction.STARTBLOWJOB, GirlAction.SUCKBLOWJOB);
                    HornyMeter.show();
                }
            }

            // suck / thrust loops
            case "bjiMSG11" -> {
                play(jenny, ModSounds.GIRLS_JENNY_LIPSOUND);
                if (partner) HornyMeter.add(0.02);
            }
            case "bjiMSG12" -> {
                if (jenny.getRandom().nextInt(5) == 0) play(jenny, ModSounds.GIRLS_JENNY_BJMOAN);
                play(jenny, ModSounds.GIRLS_JENNY_LIPSOUND);
                if (partner) HornyMeter.add(0.02);
            }
            case "bjtMSG1" -> {
                play(jenny, ModSounds.GIRLS_JENNY_MMM);
                play(jenny, ModSounds.GIRLS_JENNY_LIPSOUND);
                if (partner) HornyMeter.add(0.04);
            }
            case "bjtDone" -> {
                if (partner) SceneControls.endFastLoop(jenny, GirlAction.THRUSTBLOWJOB, GirlAction.SUCKBLOWJOB);
            }

            // climax
            case "bjcBlackScreen" -> {
                if (partner) TransitionOverlay.start();
            }
            case "bjcMSG1" -> play(jenny, ModSounds.GIRLS_JENNY_BJMOAN.variant(1));
            case "bjcMSG2" -> {
                play(jenny, ModSounds.GIRLS_JENNY_BJMOAN.variant(7));
                if (partner) HornyMeter.hide();
            }
            case "bjcMSG3" -> play(jenny, ModSounds.GIRLS_JENNY_AFTERSESSIONMOAN.variant(1));
            case "bjcMSG4" -> play(jenny, ModSounds.GIRLS_JENNY_LIGHTBREATHING.variant(0));
            case "bjcMSG5" -> play(jenny, ModSounds.GIRLS_JENNY_LIGHTBREATHING.variant(1));
            case "bjcMSG6" -> play(jenny, ModSounds.GIRLS_JENNY_LIGHTBREATHING.variant(2));
            case "bjcMSG7" -> play(jenny, ModSounds.GIRLS_JENNY_LIGHTBREATHING.variant(3));
            case "bjcDone" -> {
                if (partner) {
                    HornyMeter.reset();
                    SceneControls.requestStep(jenny, jenny.getAction(), GirlAction.NULL);
                }
            }

            // doggy: go on the bed (the server times her lines and the wait), start, slow, fast, cum
            case "doggyGoOnBedMSG1" -> play(jenny, ModSounds.MISC_BEDRUSTLE.variant(0));
            case "doggyGoOnBedMSG2" -> play(jenny, ModSounds.GIRLS_JENNY_LIGHTBREATHING.variant(9));
            case "doggyGoOnBedMSG3" -> play(jenny, ModSounds.GIRLS_JENNY_GIGGLE.variant(0));
            case "doggyGoOnBedMSG4" -> play(jenny, ModSounds.MISC_SLAP.variant(0), 0.75F);
            case "doggystartMSG1" -> play(jenny, ModSounds.MISC_TOUCH.variant(0));
            case "doggystartMSG2" -> play(jenny, ModSounds.MISC_TOUCH.variant(1));
            case "doggystartMSG3" -> play(jenny, ModSounds.MISC_BEDRUSTLE.variant(1), 0.5F);
            case "doggystartMSG4" -> {
                play(jenny, ModSounds.MISC_SMALLINSERTS);
                play(jenny, ModSounds.GIRLS_JENNY_MMM.variant(1));
                if (partner) HornyMeter.reset();
            }
            case "doggystartMSG5" -> {
                play(jenny, ModSounds.MISC_POUNDING.random(jenny.getRandom()), 0.33F);
                play(jenny, ModSounds.GIRLS_JENNY_MOAN);
            }
            case "doggystartDone" -> {
                if (partner) {
                    SceneControls.requestStep(jenny, GirlAction.DOGGYSTART, GirlAction.DOGGYSLOW);
                    HornyMeter.show();
                }
            }
            case "doggyslowMSG1" -> {
                play(jenny, ModSounds.MISC_POUNDING.random(jenny.getRandom()), 0.33F);
                int choice = jenny.getRandom().nextInt(4);
                if (choice == 0) {
                    play(jenny, jenny.getRandom().nextBoolean() ? ModSounds.GIRLS_JENNY_MMM : ModSounds.GIRLS_JENNY_MOAN);
                } else {
                    play(jenny, ModSounds.GIRLS_JENNY_HEAVYBREATHING);
                }
                if (partner) HornyMeter.add(0.00666);
            }
            case "doggyslowMSG2" -> play(jenny, ModSounds.GIRLS_JENNY_LIGHTBREATHING.random(jenny.getRandom()), 0.5F);
            case "doggyfastReady" -> {
                if (partner && Minecraft.getInstance().options.keyJump.isDown()) {
                    ClientPacketDistributor.sendToServer(new ScenePosePayload(jenny.getId(), JennyEntity.REQUEST_HARD));
                }
            }
            case "doggyfastMSG1" -> {
                play(jenny, ModSounds.MISC_POUNDING.random(jenny.getRandom()), 0.75F);
                if (partner) HornyMeter.add(0.02);
                if (++fastBeats % 2 == 0) {
                    play(jenny, jenny.getRandom().nextBoolean() ? ModSounds.GIRLS_JENNY_MOAN : ModSounds.GIRLS_JENNY_HEAVYBREATHING);
                } else {
                    play(jenny, ModSounds.GIRLS_JENNY_AHH);
                }
            }
            case "doggyfastDone" -> {
                if (partner) SceneControls.endFastLoop(jenny, GirlAction.DOGGYFAST, GirlAction.DOGGYSLOW);
            }
            case "doggycumMSG1" -> {
                play(jenny, ModSounds.MISC_CUMINFLATION.variant(0), 2.0F);
                play(jenny, ModSounds.MISC_POUNDING.random(jenny.getRandom()), 2.0F);
                play(jenny, ModSounds.GIRLS_JENNY_MOAN);
            }
            case "doggycumMSG2" -> play(jenny, ModSounds.GIRLS_JENNY_HEAVYBREATHING.variant(4));
            case "doggycumMSG3" -> play(jenny, ModSounds.GIRLS_JENNY_HEAVYBREATHING.variant(5));
            case "doggycumMSG4" -> play(jenny, ModSounds.GIRLS_JENNY_HEAVYBREATHING.variant(6));
            case "doggycumMSG5" -> play(jenny, ModSounds.GIRLS_JENNY_HEAVYBREATHING.variant(7));
            case "doggyCumDone" -> {
                if (partner) {
                    HornyMeter.reset();
                    SceneControls.requestStep(jenny, jenny.getAction(), GirlAction.NULL);
                }
            }

            // boobjob
            case "paizuri_startStep" -> {
                BlockPos below = jenny.blockPosition().below();
                BlockState state = jenny.level().getBlockState(below);
                jenny.level().playLocalSound(jenny, state.getSoundType().getStepSound(), jenny.getSoundSource(), 1.0F, 1.0F);
            }
            case "boobjob_camera" -> {
                if (partner) ClientPacketDistributor.sendToServer(new ScenePosePayload(jenny.getId(), JennyEntity.POSE_BOOBJOB));
            }
            case "paizuriStartMSG1", "paizuriSlowMSG1" -> {
                play(jenny, ModSounds.MISC_POUNDING);
                if (partner) HornyMeter.add(0.02);
            }
            case "paizuriFastMSG1" -> {
                play(jenny, ModSounds.MISC_POUNDING);
                play(jenny, jenny.getRandom().nextBoolean() ? ModSounds.GIRLS_JENNY_MMM : ModSounds.GIRLS_JENNY_AHH);
                if (partner) HornyMeter.add(0.04);
            }
            case "paizuri_startDone" -> {
                if (partner) {
                    HornyMeter.reset();
                    HornyMeter.show();
                    SceneControls.requestStep(jenny, GirlAction.PAIZURI_START, GirlAction.PAIZURI_SLOW);
                }
            }
            case "paizuri_fastDone" -> {
                if (partner) SceneControls.endFastLoop(jenny, GirlAction.PAIZURI_FAST, GirlAction.PAIZURI_SLOW);
            }
            default -> {
                SceneEvents.handleShared(jenny, event);
                // others: events of scenes not ported yet
            }
        }
    }
}
