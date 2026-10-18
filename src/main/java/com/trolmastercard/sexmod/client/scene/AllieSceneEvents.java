package com.trolmastercard.sexmod.client.scene;

import static com.trolmastercard.sexmod.client.scene.SceneEvents.play;
import static com.trolmastercard.sexmod.client.scene.SceneEvents.say;

import com.trolmastercard.sexmod.entity.GirlAction;
import com.trolmastercard.sexmod.entity.allie.AllieEntity;
import com.trolmastercard.sexmod.network.ScenePosePayload;
import com.trolmastercard.sexmod.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/**
 * Allie's animation keyframe events (1.12.2 {@code AllieEntity.registerControllers} sound
 * listener). The steps that hand over without her summoner (summon done, the wish, preparing,
 * vanishing, the timed reverse cowgirl steps) run on the server.
 */
public final class AllieSceneEvents {
    /** 1.12.2 {@code slide}: these variants only. */
    private static final int[] SLIDES = {0, 1, 4, 6};

    private AllieSceneEvents() {
    }

    public static void handle(AllieEntity allie, String event) {
        Minecraft mc = Minecraft.getInstance();
        boolean partner = mc.player != null && allie.isPartner(mc.player);
        boolean sneaking = partner && mc.options.keyShift.isDown();
        switch (event) {
            // summon
            case "summonMSG1" -> {
                say(allie, partner, "summon1");
                play(allie, ModSounds.GIRLS_ALLIE_SCAWY.variant(0), 0.5F);
            }
            case "summonMSG2" -> {
                say(allie, partner, "summon2");
                play(allie, ModSounds.GIRLS_ALLIE_GIGGLE.variant(allie.getRandom().nextInt(4)));
            }
            case "summonMSG3" -> say(allie, partner, "summon3");
            case "summonMSG4" -> {
                say(allie, partner, "summon4");
                play(allie, ModSounds.GIRLS_ALLIE_LIGHTBREATHING.variant(2));
            }
            case "summonMSG5" -> {
                say(allie, partner, "summon5");
                play(allie, ModSounds.GIRLS_ALLIE_HMPH.variant(4));
            }
            case "summonMSG6" -> {
                say(allie, partner, "summon6");
                play(allie, ModSounds.GIRLS_ALLIE_GIGGLE.variant(3));
            }
            case "summonMSG8" -> {
                say(allie, partner, "summon8");
                play(allie, ModSounds.GIRLS_ALLIE_HUH);
                if (partner) allie.openWishMenu();
            }
            case "summon_normalMSG1" -> {
                say(allie, partner, "sup");
                play(allie, ModSounds.GIRLS_ALLIE_GIGGLE.variant(allie.getRandom().nextInt(4)));
            }
            case "summon_normalMSG2" -> say(allie, partner, "youhave");
            case "summon_normalMSG3" -> {
                say(allie, partner, allie.wishesLeft() == 2 ? "2wishes" : "1wish");
                play(allie, ModSounds.GIRLS_ALLIE_HMPH.variant(4));
            }
            case "summon_normalMSG4" -> say(allie, partner, "so");
            case "summon_normalMSG5" -> {
                say(allie, partner, "tellme");
                play(allie, ModSounds.GIRLS_ALLIE_HUH);
            }
            case "summon_normalDone" -> {
                if (partner) allie.openWishMenu();
            }
            case "summon_sandMSG1" -> {
                say(allie, partner, "nooo");
                play(allie, ModSounds.GIRLS_ALLIE_SCAWY.variant(2));
            }
            case "scream" -> play(allie, ModSounds.MISC_SCREAM);

            // the wish
            case "rich_MSG1" -> {
                say(allie, partner, "wishgranted");
                play(allie, ModSounds.MISC_PLOB);
            }

            // preparing
            case "deepthroat_prepareMSG1" -> {
                say(allie, partner, "hihi");
                play(allie, ModSounds.GIRLS_ALLIE_GIGGLE);
            }
            case "deepthroat_prepareMSG2" -> {
                say(allie, partner, "boys");
                play(allie, ModSounds.GIRLS_ALLIE_SIGH.variant(0));
            }
            case "deepthroat_normal_prepareMSG1" -> {
                say(allie, partner, "alright");
                play(allie, ModSounds.GIRLS_ALLIE_GIGGLE);
            }
            case "blackscreen" -> {
                if (partner) TransitionOverlay.start();
            }
            case "deepthroat_prepareDone" -> {
                if (partner) HornyMeter.reset();
            }

            // deepthroat
            case "deepthroat_startDone" -> {
                if (partner) SceneControls.requestStep(allie, GirlAction.DEEPTHROAT_START, GirlAction.DEEPTHROAT_SLOW);
            }
            case "deepthroat_slowMSG1" -> {
                play(allie, allie.getRandom().nextFloat() > 0.33F ? ModSounds.GIRLS_ALLIE_LIPSOUND : ModSounds.GIRLS_ALLIE_BJMOAN);
                if (partner) {
                    HornyMeter.show();
                    HornyMeter.add(0.02);
                }
            }
            case "deepthroat_fastMSG1" -> {
                play(allie, ModSounds.GIRLS_ALLIE_BJMOAN);
                if (partner) {
                    HornyMeter.show();
                    HornyMeter.add(0.04);
                }
            }
            case "deepthroat_fastDone" -> {
                if (partner) SceneControls.endFastLoop(allie, GirlAction.DEEPTHROAT_FAST, GirlAction.DEEPTHROAT_SLOW);
            }
            case "deepthroat_cumMSG1" -> {
                play(allie, ModSounds.GIRLS_ALLIE_MOAN);
                play(allie, ModSounds.GIRLS_ALLIE_LIPSOUND);
                play(allie, ModSounds.MISC_CUMINFLATION, 1.5F);
            }
            case "deepthroat_cumDone" -> {
                if (partner) SceneControls.requestStep(allie, GirlAction.DEEPTHROAT_CUM, GirlAction.NULL);
            }

            // reverse cowgirl
            case "giggle" -> play(allie, ModSounds.GIRLS_ALLIE_GIGGLE);
            case "pounding" -> play(allie, ModSounds.MISC_POUNDING);
            case "moan" -> play(allie, ModSounds.GIRLS_ALLIE_MOAN);
            case "mmm" -> play(allie, ModSounds.GIRLS_ALLIE_MMM);
            case "slide" -> play(allie, ModSounds.MISC_SLIDE.variant(SLIDES[allie.getRandom().nextInt(SLIDES.length)]));
            case "openSexUi" -> {
                if (partner) HornyMeter.show();
            }
            case "slowMoan" -> {
                if (allie.getRandom().nextBoolean()) play(allie, ModSounds.GIRLS_ALLIE_AHH);
                if (partner) HornyMeter.add(0.02);
            }
            case "cowgirlSlowDone" -> allie.rerollSlowVariant();
            case "fastMoan" -> {
                if (partner) HornyMeter.add(0.04);
                if (allie.nextFastMoanSounds()) play(allie, ModSounds.GIRLS_ALLIE_MOAN);
            }
            case "fastSwitch" -> {
                if (!sneaking) return;
                if (allie.getAction() == GirlAction.REVERSE_COWGIRL_FAST_START) {
                    SceneControls.requestStep(allie, GirlAction.REVERSE_COWGIRL_FAST_START, GirlAction.REVERSE_COWGIRL_FAST_CONTINUES);
                } else {
                    ClientPacketDistributor.sendToServer(new ScenePosePayload(allie.getId(), AllieEntity.REQUEST_FAST_SWITCH));
                }
            }
            case "cum" -> play(allie, ModSounds.MISC_INSERTS, 6.0F);
            case "aftermoan" -> play(allie, ModSounds.GIRLS_ALLIE_AFTERSESSIONMOAN);
            case "cowgirl_cumDone" -> {
                if (partner) SceneControls.requestStep(allie, GirlAction.REVERSE_COWGIRL_CUM, GirlAction.NULL);
            }
            default -> SceneEvents.handleShared(allie, event);
        }
    }
}
