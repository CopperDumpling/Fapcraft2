package com.trolmastercard.sexmod.entity;

/**
 * A girl's current activity: idle ({@link #NULL}) or one step of a scene. Synced to clients,
 * where each girl maps it to an animation.
 * <p>
 * Names match 1.12.2's {@code Action} so ported code and keyframe events line up. Unlike
 * 1.12, constants hold no mutable state (that version kept a tick counter on each constant,
 * shared by every girl). Constants are added as girls are ported.
 *
 * @param transitionTicks GeckoLib blend time into this step's animation
 * @param hasPlayer       the scene partner takes part (movement locked, camera positioned)
 * @param autoBlink       the eyes controller keeps blinking during this step
 */
public enum GirlAction {
    NULL(0, false, true),
    PAYMENT(5, true, false),
    STARTBLOWJOB(2, true, false),
    SUCKBLOWJOB(2, true, false),
    THRUSTBLOWJOB(2, true, false),
    CUMBLOWJOB(0, true, false),
    STRIP(5, false, false),
    PAIZURI_START(0, true, false),
    PAIZURI_SLOW(0, true, true),
    PAIZURI_FAST(0, true, false),
    PAIZURI_CUM(0, true, false),
    /** Server-set while she walks to the bed; plays the normal walk/idle animations. */
    WALK_TO_BED(0, false, true),
    STARTDOGGY(2, false, false),
    WAITDOGGY(0, false, true),
    DOGGYSTART(0, true, false),
    DOGGYSLOW(2, true, false),
    DOGGYFAST(2, true, false),
    DOGGYCUM(2, true, false);

    private static final GirlAction[] VALUES = values();

    public final int transitionTicks;
    public final boolean hasPlayer;
    public final boolean autoBlink;

    GirlAction(int transitionTicks, boolean hasPlayer, boolean autoBlink) {
        this.transitionTicks = transitionTicks;
        this.hasPlayer = hasPlayer;
        this.autoBlink = autoBlink;
    }

    public static GirlAction byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : NULL;
    }
}
