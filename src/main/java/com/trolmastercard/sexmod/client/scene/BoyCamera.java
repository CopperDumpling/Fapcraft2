package com.trolmastercard.sexmod.client.scene;

import com.trolmastercard.sexmod.entity.GirlEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The first-person camera of a carried partner (1.12.2 {@code PositionData}): during the steps
 * that use it ({@link com.trolmastercard.sexmod.entity.GirlAction#boyCam}) the view sits on her
 * model's {@code boyCam} bone instead of at the player's eyes. Her renderer reports where the
 * bone was drawn; the camera uses the latest report (one frame old, as in 1.12.2).
 */
public final class BoyCamera {
    public static final String BONE = "boyCam";
    /** Reports older than this many frames are ignored (she went out of view). */
    private static final int MAX_AGE_FRAMES = 5;

    private static int girlId = -1;
    private static @Nullable Vec3 bonePosition;
    private static int age;

    private BoyCamera() {
    }

    /** Called by her renderer with where it drew the bone this frame. */
    public static void report(int id, Vec3 position) {
        girlId = id;
        bonePosition = position;
        age = 0;
    }

    /** Whether her renderer should report the bone this frame: the local player is her carried partner. */
    public static boolean wanted(GirlEntity girl) {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && girl.isPartner(mc.player) && girl.getAction().boyCam;
    }

    /** Where the first-person camera goes this frame, or null to leave it at the player's eyes. */
    public static @Nullable Vec3 position() {
        Minecraft mc = Minecraft.getInstance();
        Vec3 position = bonePosition;
        if (position == null || age++ > MAX_AGE_FRAMES || !mc.options.getCameraType().isFirstPerson()) return null;
        GirlEntity girl = SceneControls.currentGirl();
        if (girl == null || girl.getId() != girlId || !girl.getAction().boyCam) {
            bonePosition = null;
            return null;
        }
        return position;
    }
}
