package com.trolmastercard.sexmod.client.scene;

import static com.trolmastercard.sexmod.client.scene.SceneEvents.play;

import com.trolmastercard.sexmod.entity.GirlAction;
import com.trolmastercard.sexmod.entity.bee.BeeEntity;
import com.trolmastercard.sexmod.sound.ModSounds;
import net.minecraft.client.Minecraft;

/**
 * The bee's animation keyframe events (1.12.2 {@code BeeEntity.registerControllers} sound
 * listener). Her pearl home is timed on the server.
 * <p>
 * Left out, as in Jenny's port: {@code sex_fastReady} (1.12.2 only restarted the loop while a
 * key was held).
 */
public final class BeeSceneEvents {
    private BeeSceneEvents() {
    }

    public static void handle(BeeEntity bee, String event) {
        Minecraft mc = Minecraft.getInstance();
        boolean partner = mc.player != null && bee.isPartner(mc.player);
        switch (event) {
            case "resetCumPercentage" -> {
                if (partner) HornyMeter.reset();
            }
            case "sex_startMSG1" -> {
                play(bee, ModSounds.MISC_POUNDING);
                if (partner) HornyMeter.add(0.02);
            }
            case "sex_startDone" -> {
                if (partner) {
                    SceneControls.requestStep(bee, GirlAction.CITIZEN_START, GirlAction.CITIZEN_SLOW);
                    HornyMeter.show();
                }
            }
            case "sex_fastMSG1" -> {
                play(bee, ModSounds.MISC_POUNDING);
                if (partner) HornyMeter.add(0.04);
            }
            case "sex_fastDone" -> {
                if (partner) SceneControls.endFastLoop(bee, GirlAction.CITIZEN_FAST, GirlAction.CITIZEN_SLOW);
            }
            case "sex_cumMSG1" -> {
                play(bee, ModSounds.MISC_CUMINFLATION, 2.0F);
                play(bee, ModSounds.MISC_POUNDING);
            }
            case "blackscreen" -> {
                if (partner) TransitionOverlay.start();
            }
            case "sex_cumDone" -> {
                if (partner) {
                    HornyMeter.reset();
                    SceneControls.requestStep(bee, GirlAction.CITIZEN_CUM, GirlAction.NULL);
                }
            }
            default -> SceneEvents.handleShared(bee, event);
        }
    }
}
