package com.trolmastercard.sexmod.client.scene;

import static com.trolmastercard.sexmod.client.scene.SceneEvents.play;
import static com.trolmastercard.sexmod.client.scene.SceneEvents.say;
import static com.trolmastercard.sexmod.client.scene.SceneEvents.sayAsPartner;

import com.trolmastercard.sexmod.Fapcraft;
import com.trolmastercard.sexmod.entity.GirlAction;
import com.trolmastercard.sexmod.entity.luna.LunaEntity;
import com.trolmastercard.sexmod.sound.ModSounds;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Luna's animation keyframe events (1.12.2 {@code LunaEntity.registerControllers} sound listener).
 * Her fishing (cast, reel, eat, throw), the bed walk and the wait are timed on the server; here
 * they only play sounds and size the catch she eats.
 * <p>
 * Every client tick: a player stepping up to her by the bed sees the black transition, and once
 * they are taken the camera turns to face them (1.12.2 {@code setFishingLevelFor}).
 */
@EventBusSubscriber(modid = Fapcraft.MODID, value = Dist.CLIENT)
public final class LunaSceneEvents {
    private static final int[] CUTE_NYAS = {1, 7, 10, 11};
    /** The Lunas the local player was already sat down with, so the camera turns only once. */
    private static final Map<LunaEntity, Boolean> seated = new WeakHashMap<>();

    private LunaSceneEvents() {
    }

    @SubscribeEvent
    static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return;
        for (LunaEntity luna : player.level().getEntitiesOfClass(LunaEntity.class, player.getBoundingBox().inflate(2.0))) {
            GirlAction action = luna.getAction();
            if (action == GirlAction.WAIT_CAT && luna.distanceTo(player) <= 1.25F) TransitionOverlay.start();
            boolean sitting = action == GirlAction.COWGIRL_SITTING_INTRO && luna.isPartner(player);
            if (sitting && !seated.getOrDefault(luna, false)) mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
            seated.put(luna, sitting);
        }
    }

    public static void handle(LunaEntity luna, String event) {
        Minecraft mc = Minecraft.getInstance();
        boolean partner = mc.player != null && luna.isPartner(mc.player);
        RandomSource random = luna.getRandom();
        switch (event) {
            // payment: she eats the fish she was given
            case "paymentMSG1" -> {
                sayAsPartner(luna, partner, "payment");
                play(luna, ModSounds.MISC_PLOB.variant(0));
            }
            case "paymentMSG2" -> {
                say(luna, partner, "huh");
                play(luna, ModSounds.GIRLS_LUNA_HUH);
            }
            case "paymentMSG3" -> {
                say(luna, partner, "nya");
                play(luna, ModSounds.GIRLS_LUNA_CUTENYA.variant(CUTE_NYAS[random.nextInt(CUTE_NYAS.length)]));
            }
            case "paymentMSG4" -> {
                say(luna, partner, "thanks");
                play(luna, ModSounds.GIRLS_LUNA_OWO);
            }
            case "eatPay" -> {
                eatSound(luna);
                luna.bitePayment();
            }
            case "paymentDone" -> {
                // the server moves on to the option she was asked for; `to` is ignored there
                if (partner) SceneControls.requestStep(luna, GirlAction.PAYMENT, GirlAction.PAYMENT);
                luna.resetPaymentScale();
            }

            // fishing (cast, eat and throw are timed on the server)
            case "renderItem" -> luna.setCatchFlight(1.0F);
            case "eat" -> {
                eatSound(luna);
                luna.biteCatch();
            }
            case "burp" -> luna.level().playLocalSound(luna, SoundEvents.PLAYER_BURP, luna.getSoundSource(), 0.5F, random.nextFloat() * 0.1F + 0.9F);
            case "eatingDone", "throw_away" -> luna.resetCatchView();

            // her sounds
            case "breath", "rod_breath" -> play(luna, ModSounds.GIRLS_LUNA_LIGHTBREATHING);
            case "happyOh" -> play(luna, ModSounds.GIRLS_LUNA_HAPPYOH);
            case "cutenya3" -> play(luna, ModSounds.GIRLS_LUNA_CUTENYA.variant(3));
            case "cutenya2" -> play(luna, ModSounds.GIRLS_LUNA_CUTENYA.variant(2));
            case "huh" -> play(luna, ModSounds.GIRLS_LUNA_HUH);
            case "hmph" -> play(luna, ModSounds.GIRLS_LUNA_HMPH);
            case "hehe", "giggle" -> play(luna, ModSounds.GIRLS_LUNA_GIGGLE);
            case "singing" -> play(luna, ModSounds.GIRLS_LUNA_SINGING);
            case "touch" -> play(luna, ModSounds.MISC_TOUCH);
            case "jump" -> play(luna, ModSounds.MISC_JUMP.variant(0), 0.2F);
            case "horninya" -> play(luna, ModSounds.GIRLS_LUNA_HORNINYA);
            case "horninya2", "touch_boobs_cumMSG3", "sitting_cumMSG1" -> {
                play(luna, ModSounds.GIRLS_LUNA_HORNINYA.variant(1));
                play(luna, ModSounds.MISC_CUMINFLATION.variant(0), 5.0F);
            }
            case "moan" -> play(luna, ModSounds.GIRLS_LUNA_MOAN);
            case "moanOrNya" -> play(luna, random.nextBoolean() ? ModSounds.GIRLS_LUNA_MOAN : ModSounds.GIRLS_LUNA_HORNINYA);
            case "pounding" -> play(luna, ModSounds.MISC_POUNDING);

            // touch boobs
            case "touch_boobsMSG1" -> {
                say(luna, partner, "touchme");
                play(luna, ModSounds.GIRLS_LUNA_GIGGLE);
            }
            case "touch_boobs_introDone" -> {
                if (partner) {
                    SceneControls.requestStep(luna, GirlAction.TOUCH_BOOBS_INTRO, GirlAction.TOUCH_BOOBS_SLOW);
                    HornyMeter.reset();
                    HornyMeter.show();
                }
            }
            case "touch_boobs_slowDone" -> luna.rollSlowVariant();
            case "addCumSlow" -> {
                if (partner) HornyMeter.add(0.02);
            }
            case "addCumFast" -> {
                if (partner) HornyMeter.add(0.04);
            }
            case "fastDone" -> {
                if (partner) SceneControls.endFastLoop(luna, GirlAction.TOUCH_BOOBS_FAST, GirlAction.TOUCH_BOOBS_SLOW);
            }
            case "touch_boobs_cumMSG1" -> play(luna, ModSounds.GIRLS_LUNA_HORNINYA.variant(3));
            case "touch_boobs_cumMSG2" -> play(luna, ModSounds.GIRLS_LUNA_HORNINYA.variant(9));
            case "blackScreen" -> {
                if (partner) TransitionOverlay.start();
            }
            case "touch_boobs_cumDone" -> {
                // both climaxes end on this event
                if (partner) {
                    HornyMeter.reset();
                    SceneControls.requestStep(luna, luna.getAction(), GirlAction.NULL);
                }
            }

            // sitting cowgirl (the partner's seat for each stretch is set by the server)
            case "sitting_introDone" -> {
                if (partner) {
                    SceneControls.requestStep(luna, GirlAction.COWGIRL_SITTING_INTRO, GirlAction.COWGIRL_SITTING_SLOW);
                    HornyMeter.reset();
                    HornyMeter.show();
                }
            }
            case "sitting_slowMSG1" -> {
                if (random.nextBoolean()) {
                    play(luna, random.nextBoolean() ? ModSounds.GIRLS_LUNA_HORNINYA : ModSounds.GIRLS_LUNA_MOAN);
                } else {
                    play(luna, ModSounds.GIRLS_LUNA_LIGHTBREATHING);
                }
                if (partner) HornyMeter.add(0.02);
            }
            case "sitting_fastMSG1" -> {
                play(luna, random.nextBoolean() ? ModSounds.GIRLS_LUNA_HORNINYA : ModSounds.GIRLS_LUNA_MOAN);
                if (partner) HornyMeter.add(0.04);
            }
            case "sitting_fastDone" -> {
                if (partner) SceneControls.endFastLoop(luna, GirlAction.COWGIRL_SITTING_FAST, GirlAction.COWGIRL_SITTING_SLOW);
            }

            // headpat
            case "headpatMSG1" -> {
                say(luna, partner, "huh2");
                play(luna, ModSounds.GIRLS_LUNA_HUH);
            }
            case "headpatMSG2" -> play(luna, ModSounds.GIRLS_LUNA_MMM);
            case "headpatMSG3" -> {
                say(luna, partner, "nya2");
                play(luna, ModSounds.GIRLS_LUNA_HORNINYA.variant(0));
            }
            case "resetGirl" -> {
                if (partner) SceneControls.requestStep(luna, GirlAction.HEAD_PAT, GirlAction.NULL);
            }
            default -> SceneEvents.handleShared(luna, event);
        }
    }

    /** 1.12.2: an eating sound at half or full volume, pitch around 1. */
    private static void eatSound(LunaEntity luna) {
        RandomSource random = luna.getRandom();
        float volume = 0.5F + 0.5F * random.nextInt(2);
        float pitch = (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F;
        luna.level().playLocalSound(luna, ModSounds.MISC_EAT.random(random).value(), luna.getSoundSource(), volume, pitch);
    }
}
