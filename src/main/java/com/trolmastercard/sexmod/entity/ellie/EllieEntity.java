package com.trolmastercard.sexmod.entity.ellie;

import com.geckolib.animation.RawAnimation;
import com.trolmastercard.sexmod.client.scene.EllieSceneEvents;
import com.trolmastercard.sexmod.entity.BedFinder;
import com.trolmastercard.sexmod.entity.GirlAction;
import com.trolmastercard.sexmod.entity.GirlEntity;
import com.trolmastercard.sexmod.entity.SceneOption;
import com.trolmastercard.sexmod.entity.SceneOption.Outfit;
import com.trolmastercard.sexmod.registry.ModEffects;
import com.trolmastercard.sexmod.sound.ModSounds;
import com.trolmastercard.sexmod.util.VectorMath;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Ellie: the tall "mommy" girl (1.12.2 {@code EllieEntity}).
 * <p>
 * <ul>
 *   <li>The companion behaviour every girl shares ({@link GirlEntity}), crouching under low
 *   ceilings, and dress up.</li>
 *   <li>The carried scene ("face fuck"): she lifts the partner up to her face, slow ⇄ fast on
 *   sneak, climax on a full meter + jump.</li>
 *   <li>The horny potion: she dashes at the nearest player and hugs them from behind; they pick
 *   cowgirl or missionary; she walks to a bed, sits on its end and waits. Whoever then steps up to
 *   her gets the chosen scene, and she ends up nude.</li>
 * </ul>
 */
public class EllieEntity extends GirlEntity {
    private static final List<SceneOption> OPTIONS = List.of(
        new SceneOption("facefuck", "action.names.facefuck", null, 0, GirlAction.CARRY_INTRO, Outfit.DRESSED, false, false, false, ""),
        new SceneOption("dressup", "action.names.dressup", null, 0, GirlAction.STRIP, Outfit.NUDE, false, true, false, ""));
    /** Offered once the hug is over (1.12.2 {@code canJoinPlayer(player, true)}). */
    private static final List<SceneOption> HUG_OPTIONS = List.of(
        new SceneOption("cowgirl", "action.names.cowgirl", null, 0, GirlAction.COWGIRLSTART, Outfit.ANY, false, false, false, ""),
        new SceneOption("missionary", "action.names.missionary", null, 0, GirlAction.MISSIONARY_START, Outfit.ANY, false, false, false, ""));

    /** 1.12.2 {@code getEyeHeight}: 1.9, or 1.53 while she crouches under a low ceiling. */
    private static final float EYE_HEIGHT = 1.9F;
    private static final float CROUCHED_EYE_HEIGHT = 1.53F;
    /** The carry intro lifts the partner up to her face 110 ticks in, and loops from 191 (1.12.2 {@code ak}, {@code CARRY_INTRO} length). */
    private static final int CARRY_LIFT_TICKS = 110;
    private static final int CARRY_INTRO_TICKS = 191;
    /** Potion route timings (1.12.2 {@code zFlag}, {@code yFlag}, {@code ai}, {@code al}, {@code af}). */
    private static final int DASH_TICKS = 16;
    private static final int HUG_TICKS = 150;
    private static final int HUG_SELECTED_TICKS = 79;
    private static final int SIT_DOWN_TICKS = 109;
    private static final int JOIN_TICKS = 20;
    /** {@code sitdownMSG1}, 2.76 s into sitting down. */
    private static final int COME_TO_MOMMY_TICKS = 55;
    /** She looks for a player this far away when the potion hits, and starts a scene with one this close to her seat. */
    private static final double POTION_RANGE = 10.0;
    private static final double JOIN_RANGE = 1.5;
    /** If she can't reach the seat in this long she is put on it (as Jenny is at her bed). */
    private static final int SEAT_WALK_TIMEOUT = 200;

    /** Client: which of the four slow carry loops plays; each loop picks another (1.12.2 {@code aa}, re-rolled per client). */
    private int carryVariant = 1;
    private boolean crouched;
    /** Server: the scene the partner picked during the hug, started when someone sits down with her. */
    private GirlAction chosenScene = GirlAction.NULL;
    private @Nullable Vec3 seat;
    private float seatYaw;
    private int seatWalkTicks;
    /** Server: ticks since a player stepped up to her seat, or -1. */
    private int joinTicks = -1;

    public EllieEntity(EntityType<? extends EllieEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createGirlAttributes();
    }

    @Override
    protected String animationPrefix() {
        return "animation.ellie.";
    }

    @Override
    protected String blinkAnimationName() {
        return "eyes";
    }

    @Override
    public String dialoguePrefix() {
        return "ellie.dialogue.";
    }

    @Override
    public List<SceneOption> sceneOptions() {
        return OPTIONS;
    }

    @Override
    public List<SceneOption> availableOptions() {
        return awaitsChoice() ? HUG_OPTIONS : super.availableOptions();
    }

    /* ------------------------------------------------------------------ */
    /* Low ceilings                                                       */
    /* ------------------------------------------------------------------ */

    /** She is too tall for a block two above her feet (1.12.2 {@code isBedBlocked}: anything but air). */
    private boolean ceilingIsLow() {
        return !this.level().getBlockState(this.blockPosition().above(2)).isAir();
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            tickPotion();
            tickSeat();
        }
        boolean low = ceilingIsLow();
        if (low != this.crouched) {
            this.crouched = low;
            refreshDimensions();
        }
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return super.getDefaultDimensions(pose).withEyeHeight(this.crouched ? CROUCHED_EYE_HEIGHT : EYE_HEIGHT);
    }

    @Override
    protected String movementAnimation(boolean moving) {
        if (this.crouched) return moving ? "crouchwalk" : "crouchidle";
        return super.movementAnimation(moving);
    }

    /* ------------------------------------------------------------------ */
    /* Scenes                                                             */
    /* ------------------------------------------------------------------ */

    @Override
    protected @Nullable RawAnimation actionAnimation(GirlAction action) {
        return switch (action) {
            case NULL -> loop("null");
            case STRIP -> once("strip");
            case CARRY_INTRO -> once("carry_intro");
            case CARRY_SLOW -> loop("carry_slow" + this.carryVariant);
            case CARRY_FAST -> loop("carry_fast");
            case CARRY_CUM -> once("carry_cum");
            case DASH -> once("dash");
            case HUG -> once("hug");
            case HUGIDLE -> loop("hugidle");
            case HUGSELECTED -> once("hugselected");
            case WALK_TO_BED -> loop("null"); // the movement controller walks her
            case SITDOWN -> once("sitdown");
            case SITDOWNIDLE -> loop("sitdownidle");
            case COWGIRLSTART -> once("cowgirlstart");
            case COWGIRLSLOW -> loop("cowgirlslow2");
            case COWGIRLFAST -> loop("cowgirlfast");
            case COWGIRLCUM -> once("cowgirlcum");
            case MISSIONARY_START -> once("missionary_start");
            case MISSIONARY_SLOW -> loop("missionary_slow");
            case MISSIONARY_FAST -> loop("missionary_fast");
            case MISSIONARY_CUM -> once("missionary_cum");
            case ATTACK, RIDE, SIT, THROW_PEARL, DOWNED -> companionAnimation(action);
            default -> null;
        };
    }

    /** Client: the slow carry loop ended; the next one is a different variant. */
    public void nextCarryVariant() {
        int previous = this.carryVariant;
        do {
            this.carryVariant = this.random.nextInt(4) + 1;
        } while (this.carryVariant == previous);
    }

    @Override
    public @Nullable GirlAction sneakStep(GirlAction action) {
        return switch (action) {
            case CARRY_SLOW -> GirlAction.CARRY_FAST;
            case COWGIRLSLOW -> GirlAction.COWGIRLFAST;
            case MISSIONARY_SLOW -> GirlAction.MISSIONARY_FAST;
            default -> null;
        };
    }

    @Override
    public @Nullable GirlAction climaxStep(GirlAction action) {
        return switch (action) {
            case CARRY_SLOW, CARRY_FAST -> GirlAction.CARRY_CUM;
            case COWGIRLSLOW, COWGIRLFAST -> GirlAction.COWGIRLCUM;
            case MISSIONARY_SLOW, MISSIONARY_FAST -> GirlAction.MISSIONARY_CUM;
            default -> null;
        };
    }

    @Override
    protected boolean canStep(GirlAction from, GirlAction to) {
        if (to == sneakStep(from) || to == climaxStep(from)) return true;
        return switch (from) {
            case CARRY_FAST -> to == GirlAction.CARRY_SLOW;             // carry_fastDone
            case COWGIRLSTART, COWGIRLFAST -> to == GirlAction.COWGIRLSLOW;  // cowgirlStartDone, cowgirlfastDone
            case MISSIONARY_START, MISSIONARY_FAST -> to == GirlAction.MISSIONARY_SLOW; // missionary_startDone, missionary_fastDone
            case CARRY_CUM, COWGIRLCUM, MISSIONARY_CUM -> to == GirlAction.NULL; // *_cumDone
            default -> false;
        };
    }

    @Override
    protected int stripEndTicks() {
        return 97; // stripDone at 4.84 s
    }

    @Override
    protected void tickAction(GirlAction action, int ticks) {
        switch (action) {
            case CARRY_INTRO -> {
                if (ticks == CARRY_LIFT_TICKS && getPartner() instanceof ServerPlayer partner) liftPartner(partner);
                if (ticks >= CARRY_INTRO_TICKS) advance(GirlAction.CARRY_SLOW);
            }
            case DASH -> {
                if (ticks >= DASH_TICKS) hugPartner();
            }
            case HUG -> {
                if (ticks >= HUG_TICKS) setAction(GirlAction.HUGIDLE);
            }
            case HUGSELECTED -> {
                if (ticks >= HUG_SELECTED_TICKS) leadToBed();
            }
            case SITDOWN -> {
                if (ticks == COME_TO_MOMMY_TICKS) sayNearby("cometomommy");
                if (ticks >= SIT_DOWN_TICKS) setAction(GirlAction.SITDOWNIDLE);
            }
            default -> super.tickAction(action, ticks);
        }
    }

    /* ------------------------------------------------------------------ */
    /* Horny potion: dash, hug, choice, seat                              */
    /* ------------------------------------------------------------------ */

    /**
     * Server: the potion makes her dash at the nearest player within 10 blocks, who is held still
     * (1.12.2 {@code handleHornyPotion}). The effect is used up.
     */
    private void tickPotion() {
        if (!isHorny() || isBusy() || getAction() != GirlAction.NULL) return;
        if (!(this.level().getNearestPlayer(this, POTION_RANGE) instanceof ServerPlayer player) || player.isSpectator()) return;
        removeEffect(ModEffects.HORNY);
        leaveCompanionPose();
        setPartner(player);
        // 1.12.2 left out the +90 its other turn-to code has, so she dashed sideways; she faces the player
        float yaw = (float) Math.toDegrees(Math.atan2(this.getZ() - player.getZ(), this.getX() - player.getX())) + 90.0F;
        anchor(this.position(), yaw);
        setAction(GirlAction.DASH);
    }

    /** Server: the dash lands her behind the partner, facing the way they face, and the hug begins (1.12.2 {@code handleSitUpFinish}). */
    private void hugPartner() {
        Player partner = getPartner();
        if (partner == null) {
            endScene();
            return;
        }
        Vec3 behind = partner.position().add(VectorMath.rotateByYaw(new Vec3(0.0, 0.0, -0.5), partner.getYRot()));
        anchor(behind, partner.getYRot());
        setAction(GirlAction.HUG);
    }

    @Override
    public boolean awaitsChoice() {
        return getAction() == GirlAction.HUG || getAction() == GirlAction.HUGIDLE;
    }

    @Override
    public void chooseMidScene(ServerPlayer player, SceneOption option) {
        this.chosenScene = option.firstStep();
        setAction(GirlAction.HUGSELECTED);
    }

    /**
     * Server: after "follow me, darling~" she lets go of the partner and walks to the end of the
     * nearest bed (1.12.2 {@code handleSitUpTimer}). With no bed she gives up.
     */
    private void leadToBed() {
        BedFinder.Plan plan = BedFinder.planSeat(this.level(), this.blockPosition());
        if (!plan.ok()) {
            sayNearby("nobedinsight");
            this.level().playSound(null, this, ModSounds.GIRLS_ELLIE_SIGH.variant(0).value(), this.getSoundSource(), 6.0F, 1.0F);
            endScene();
            return;
        }
        releaseAnchor();
        setPartner(null);
        this.seat = plan.position();
        this.seatYaw = plan.yaw();
        this.seatWalkTicks = 0;
        setAction(GirlAction.WALK_TO_BED);
        this.getNavigation().moveTo(this.seat.x, this.seat.y, this.seat.z, 0.35);
    }

    /** Server: walking to the seat, then waiting on it for someone to step up (1.12.2 {@code handleSitTransition}, {@code U}). */
    private void tickSeat() {
        if (this.seat == null) return;
        GirlAction action = getAction();
        if (action == GirlAction.WALK_TO_BED) {
            this.seatWalkTicks++;
            if (this.position().distanceTo(this.seat) > 1.0 && this.seatWalkTicks <= SEAT_WALK_TIMEOUT) {
                if (this.getNavigation().isDone()) this.getNavigation().moveTo(this.seat.x, this.seat.y, this.seat.z, 0.35);
                return;
            }
            this.getNavigation().stop();
            this.setPos(this.seat);
            anchor(this.seat, this.seatYaw);
            this.noPhysics = true;
            setAction(GirlAction.SITDOWN);
        } else if (action == GirlAction.SITDOWNIDLE) {
            if (this.joinTicks < 0) {
                Player near = this.level().getNearestPlayer(this, POTION_RANGE);
                if (near instanceof ServerPlayer player && !player.isSpectator() && near.distanceTo(this) <= JOIN_RANGE) {
                    setPartner(player);
                    this.joinTicks = 0;
                }
            } else if (++this.joinTicks >= JOIN_TICKS) {
                startChosenScene();
            }
        }
    }

    /** Server: she undresses and the partner is put in place for the scene picked during the hug. */
    private void startChosenScene() {
        this.joinTicks = -1;
        if (!(getPartner() instanceof ServerPlayer partner) || this.chosenScene == GirlAction.NULL) {
            endScene();
            return;
        }
        setDressed(false);
        float yaw = this.seatYaw;
        Vec3 pos;
        float partnerYaw;
        if (this.chosenScene == GirlAction.COWGIRLSTART) {
            partnerYaw = yaw + 180.0F;
            pos = this.seat.add(VectorMath.rotateByYaw(new Vec3(0.0, 1.0 - partner.getEyeHeight(), -1.8125), partnerYaw));
        } else {
            partnerYaw = yaw;
            pos = this.seat.add(VectorMath.rotateByYaw(new Vec3(0.0, 0.0, 0.1), partnerYaw));
        }
        partner.connection.teleport(pos.x, pos.y, pos.z, Mth.wrapDegrees(partnerYaw), partner.getXRot());
        setAction(this.chosenScene);
    }

    @Override
    public void endScene() {
        super.endScene();
        this.chosenScene = GirlAction.NULL;
        this.seat = null;
        this.joinTicks = -1;
    }

    /** She picks the partner up: their eyes end up level with her face (1.12.2 {@code handleSitTimer}). */
    private void liftPartner(ServerPlayer partner) {
        Vec3 offset = VectorMath.rotateByYaw(new Vec3(0.0, 2.5625 - partner.getEyeHeight(), -0.3125), 180.0F + this.getYRot());
        Vec3 pos = this.position().add(offset);
        partner.connection.teleport(pos.x, pos.y, pos.z, partner.getYRot(), partner.getXRot());
    }

    @Override
    protected void onAnimationEvent(String event) {
        if (this.level().isClientSide()) EllieSceneEvents.handle(this, event);
    }

    /* ------------------------------------------------------------------ */
    /* Companion lines                                                    */
    /* ------------------------------------------------------------------ */

    @Override
    protected int attackTicks(int animation) {
        return animation == 0 ? 7 : 10; // attackDone at 0.32 s / 0.48 s
    }

    @Override
    public void onDismissed(ServerPlayer player) {
        sayTo(player, "stopfollowme");
        this.level().playSound(null, this, ModSounds.GIRLS_ELLIE_SIGH.variant(1).value(), this.getSoundSource(), 6.0F, 1.0F);
    }

    @Override
    public void onNewHome(ServerPlayer player) {
        sayTo(player, "newhome");
        this.level().playSound(null, this, ModSounds.GIRLS_ELLIE_HUH.variant(0).value(), this.getSoundSource(), 6.0F, 1.0F);
    }

    @Override
    public boolean shouldShowName() {
        // 1.12.2 hid her name tag while she lifts the partner
        return getAction() != GirlAction.CARRY_INTRO && super.shouldShowName();
    }
}
