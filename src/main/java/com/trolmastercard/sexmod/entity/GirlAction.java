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
 * @param busy            she can't take orders (the menu says she is busy) and her own AI pauses.
 *                        True for scene steps; false for what a companion does along the way
 * @param boyCam          in first person the partner's camera follows her model's {@code boyCam}
 *                        bone (1.12.2 {@code useBoyCam}: the scenes where she carries the partner)
 */
public enum GirlAction {
    NULL(0, false, true, false),
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
    DOGGYCUM(2, true, false),
    // Ellie: carried ("face fuck")
    CARRY_INTRO(0, true, false, true, true),
    CARRY_SLOW(0, true, false, true, true),
    CARRY_FAST(0, true, false, true, true),
    CARRY_CUM(0, true, false, true, true),
    // Ellie: horny potion (dash, hug, choice, bed) and the scenes on the bed
    DASH(2, false, false),
    HUG(2, true, false),
    HUGIDLE(0, true, true),
    HUGSELECTED(0, true, false),
    SITDOWN(2, false, false),
    SITDOWNIDLE(0, false, true),
    COWGIRLSTART(0, true, false),
    COWGIRLSLOW(10, true, false),
    COWGIRLFAST(10, true, false),
    COWGIRLCUM(2, true, false),
    MISSIONARY_START(0, true, false),
    MISSIONARY_SLOW(2, true, false),
    MISSIONARY_FAST(2, true, false),
    MISSIONARY_CUM(2, true, false),
    // Slime: undresses on her own (pregnancy progress or the potion)
    UNDRESS(2, false, true),
    // Bia: talk, headpat, and anal / prone doggy on a bed
    TALK_HORNY(5, true, false),
    TALK_IDLE(0, true, true),
    TALK_RESPONSE(2, true, false),
    HEAD_PAT(0, true, false),
    ANAL_PREPARE(5, false, false),
    ANAL_WAIT(0, false, true),
    ANAL_START(0, true, false),
    ANAL_SLOW(2, true, true),
    ANAL_FAST(0, true, false),
    ANAL_CUM(2, true, false),
    PRONE_DOGGY_INTRO(0, true, false, true, true),
    PRONE_DOGGY_INSERT(2, true, false, true, true),
    PRONE_DOGGY_SOFT(0, true, false, true, true),
    PRONE_DOGGY_HARD(0, true, false, true, true),
    PRONE_DOGGY_CUM(0, true, false, true, true),
    // Luna: fishing on her own (not scene steps: she still takes orders), touch boobs, and the
    // sitting cowgirl after she waits by a bed
    FISHING_START(5, false, false, false),
    FISHING_IDLE(0, false, true, false),
    FISHING_EAT(0, false, false, false),
    FISHING_THROW_AWAY(0, false, false, false),
    TOUCH_BOOBS_INTRO(0, true, false),
    TOUCH_BOOBS_SLOW(2, true, false),
    TOUCH_BOOBS_FAST(2, true, false),
    TOUCH_BOOBS_CUM(2, true, false),
    WAIT_CAT(0, false, false),
    COWGIRL_SITTING_INTRO(0, true, false),
    COWGIRL_SITTING_SLOW(5, true, false),
    COWGIRL_SITTING_FAST(5, true, false),
    COWGIRL_SITTING_CUM(5, true, false),
    // Bee: the wild bee's scene with whoever she reaches
    CITIZEN_START(0, true, false),
    CITIZEN_SLOW(0, true, false),
    CITIZEN_FAST(0, true, false),
    CITIZEN_CUM(2, true, false),
    // Allie: summoned by her lamp; the wish menu, the wish, and her two scenes
    SUMMON(0, false, false),
    SUMMON_WAIT(0, false, false),
    SUMMON_NORMAL(0, false, false),
    SUMMON_NORMAL_WAIT(2, false, false),
    SUMMON_SAND(0, false, false),
    RICH_FIRST_TIME(0, false, false),
    RICH_NORMAL(0, false, false),
    ALLIE_PREPARE_FIRST_TIME(0, false, false),
    ALLIE_PREPARE_NORMAL(2, false, false),
    DEEPTHROAT_START(0, true, false, true, true),
    DEEPTHROAT_SLOW(2, true, false, true, true),
    DEEPTHROAT_FAST(2, true, false, true, true),
    DEEPTHROAT_CUM(2, true, false, true, true),
    REVERSE_COWGIRL_START(0, true, false, true, true),
    REVERSE_COWGIRL_SLOW(0, true, false, true, true),
    REVERSE_COWGIRL_FAST_START(0, true, false, true, true),
    REVERSE_COWGIRL_FAST_CONTINUES(0, true, false, true, true),
    REVERSE_COWGIRL_CUM(0, true, false, true, true),
    // companion (1.12.2 GirlFollowGoal + the pearl home)
    ATTACK(0, false, true, false),
    RIDE(0, false, true, false),
    SIT(0, false, true, false),
    THROW_PEARL(0, false, false),
    DOWNED(7, false, true);

    private static final GirlAction[] VALUES = values();

    public final int transitionTicks;
    public final boolean hasPlayer;
    public final boolean autoBlink;
    public final boolean busy;
    public final boolean boyCam;

    GirlAction(int transitionTicks, boolean hasPlayer, boolean autoBlink) {
        this(transitionTicks, hasPlayer, autoBlink, true);
    }

    GirlAction(int transitionTicks, boolean hasPlayer, boolean autoBlink, boolean busy) {
        this(transitionTicks, hasPlayer, autoBlink, busy, false);
    }

    GirlAction(int transitionTicks, boolean hasPlayer, boolean autoBlink, boolean busy, boolean boyCam) {
        this.transitionTicks = transitionTicks;
        this.hasPlayer = hasPlayer;
        this.autoBlink = autoBlink;
        this.busy = busy;
        this.boyCam = boyCam;
    }

    public static GirlAction byId(int id) {
        return id >= 0 && id < VALUES.length ? VALUES[id] : NULL;
    }
}
