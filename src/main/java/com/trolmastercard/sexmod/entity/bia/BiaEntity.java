package com.trolmastercard.sexmod.entity.bia;

import com.geckolib.animation.RawAnimation;
import com.trolmastercard.sexmod.client.scene.BiaSceneEvents;
import com.trolmastercard.sexmod.entity.BedFinder;
import com.trolmastercard.sexmod.entity.GirlAction;
import com.trolmastercard.sexmod.entity.GirlEntity;
import com.trolmastercard.sexmod.entity.SceneOption;
import com.trolmastercard.sexmod.entity.SceneOption.Outfit;
import com.trolmastercard.sexmod.sound.ModSounds;
import com.trolmastercard.sexmod.util.VectorMath;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Bia: the catgirl (1.12.2 {@code BiaEntity}).
 * <ul>
 *   <li>The companion behaviour every girl shares ({@link GirlEntity}); strip and dress up, free.</li>
 *   <li>Headpat: she walks up to the player and enjoys it.</li>
 *   <li>Talk: she says she's horny and offers anal or doggy. Picked, she lets the partner go and
 *   walks to a bed: for anal she lies on its free side, undressed, and waits; for doggy she sits
 *   on the end of it. Whoever then steps up to her gets the scene.</li>
 *   <li>Anal: start, slow ⇄ fast on sneak, climax. Prone doggy: intro, sneak to insert, soft, and
 *   hard stretches while sneak is held, climax; the camera follows her {@code boyCam} bone.</li>
 * </ul>
 */
public class BiaEntity extends GirlEntity {
    private static final List<SceneOption> OPTIONS = List.of(
        new SceneOption("strip", "action.names.strip", null, 0, GirlAction.STRIP, Outfit.DRESSED, false, true, false, ""),
        new SceneOption("dressup", "action.names.dressup", null, 0, GirlAction.STRIP, Outfit.NUDE, false, true, false, ""),
        new SceneOption("talk", "action.names.talk", null, 0, GirlAction.TALK_HORNY, Outfit.ANY, false, false, false, ""),
        new SceneOption("headpat", "action.names.headpat", null, 0, GirlAction.HEAD_PAT, Outfit.ANY, false, false, false, ""));
    /** Offered when the talk is over (1.12.2 {@code openBiaInventory}). */
    private static final List<SceneOption> TALK_OPTIONS = List.of(
        new SceneOption("anal", "action.names.anal", null, 0, GirlAction.ANAL_PREPARE, Outfit.ANY, false, false, false, ""),
        new SceneOption("doggy", "action.names.doggy", null, 0, GirlAction.PRONE_DOGGY_INTRO, Outfit.ANY, false, false, false, ""));

    /** {@code talk_responseDone}: she answers for 3.32 s, then heads for the bed. */
    private static final int TALK_RESPONSE_TICKS = 66;
    /** {@code anal_prepareDone} / {@code sitdownDone} at 5.48 s; {@code sitdownMSG1} at 2.76 s. */
    private static final int SETTLE_TICKS = 110;
    private static final int COME_HERE_TICKS = 55;
    /** Steps that hand over on a timer (1.12.2 {@code Action} lengths). */
    private static final int INSERT_TICKS = 42;
    private static final int HARD_TICKS = 34;
    /** A player this close to her on the bed starts the scene after 22 ticks (1.12.2 {@code handleAnalState}). */
    private static final double JOIN_RANGE = 1.0;
    private static final double LOOK_RANGE = 10.0;
    private static final int JOIN_TICKS = 22;
    /** Walking to the bed: re-path at 60 and 120 ticks, and settle wherever she is after 200 (as Jenny). */
    private static final int BED_WALK_TIMEOUT = 200;

    /** Server: the scene picked after the talk; her spot on the bed and how she gets there. */
    private GirlAction chosenScene = GirlAction.NULL;
    private @Nullable Vec3 spot;
    private float spotYaw;
    private int walkTicks;
    private int joinTicks = -1;
    /** Client: which of the three hard doggy stretches plays (1.12.2 {@code ah}, re-rolled per client). */
    private int hardVariant = 1;
    private GirlAction lastClientAction = GirlAction.NULL;

    public BiaEntity(EntityType<? extends BiaEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createGirlAttributes();
    }

    @Override
    protected String animationPrefix() {
        return "animation.bia.";
    }

    @Override
    protected String blinkAnimationName() {
        return "fhappy";
    }

    @Override
    public String dialoguePrefix() {
        return "bia.dialogue.";
    }

    @Override
    public List<SceneOption> sceneOptions() {
        return OPTIONS;
    }

    @Override
    public List<SceneOption> availableOptions() {
        return awaitsChoice() ? TALK_OPTIONS : super.availableOptions();
    }

    @Override
    protected @Nullable RawAnimation actionAnimation(GirlAction action) {
        return switch (action) {
            case NULL -> loop("null");
            case STRIP -> once("strip");
            case TALK_HORNY -> once("talk_horny2");
            case TALK_IDLE -> once("talk_idle2");
            case TALK_RESPONSE -> once("talk_response");
            case HEAD_PAT -> once("headpat");
            case WALK_TO_BED -> loop("null"); // the movement controller walks her
            case ANAL_PREPARE -> once("anal_prepare");
            case ANAL_WAIT -> once("anal_wait");
            case ANAL_START -> once("anal_start");
            case ANAL_SLOW -> loop("anal_slow");
            case ANAL_FAST -> loop("anal_fast");
            case ANAL_CUM -> once("anal_cum");
            case SITDOWN -> once("sitdown");
            case SITDOWNIDLE -> loop("sitdownidle");
            case PRONE_DOGGY_INTRO -> loop("prone_doggy_intro");
            case PRONE_DOGGY_INSERT -> loop("prone_doggy_insert");
            case PRONE_DOGGY_SOFT -> loop("prone_doggy_soft");
            case PRONE_DOGGY_HARD -> loop("prone_doggy_hard" + this.hardVariant);
            case PRONE_DOGGY_CUM -> once("prone_doggy_cum");
            case ATTACK, RIDE, SIT, THROW_PEARL, DOWNED -> companionAnimation(action);
            default -> null;
        };
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            // each hard stretch picks another of the three (1.12.2 resetAnimationControllerTicks)
            GirlAction action = getAction();
            if (action == GirlAction.PRONE_DOGGY_HARD && this.lastClientAction != action) {
                int previous = this.hardVariant;
                do {
                    this.hardVariant = this.random.nextInt(3) + 1;
                } while (this.hardVariant == previous);
            }
            this.lastClientAction = action;
        } else {
            tickBed();
        }
    }

    /* ------------------------------------------------------------------ */
    /* Scene steps                                                        */
    /* ------------------------------------------------------------------ */

    @Override
    public @Nullable GirlAction sneakStep(GirlAction action) {
        return switch (action) {
            case ANAL_SLOW -> GirlAction.ANAL_FAST;
            case PRONE_DOGGY_INTRO -> GirlAction.PRONE_DOGGY_INSERT;
            default -> null;
        };
    }

    @Override
    public @Nullable GirlAction climaxStep(GirlAction action) {
        return switch (action) {
            case ANAL_SLOW, ANAL_FAST -> GirlAction.ANAL_CUM;
            case PRONE_DOGGY_SOFT, PRONE_DOGGY_HARD -> GirlAction.PRONE_DOGGY_CUM;
            default -> null;
        };
    }

    @Override
    protected boolean canStep(GirlAction from, GirlAction to) {
        if (to == sneakStep(from) || to == climaxStep(from)) return true;
        return switch (from) {
            case TALK_HORNY -> to == GirlAction.TALK_IDLE;                      // talk_hornyDone
            case HEAD_PAT, ANAL_CUM, PRONE_DOGGY_CUM -> to == GirlAction.NULL;  // headpatDone, *_cumDone
            case ANAL_START, ANAL_FAST -> to == GirlAction.ANAL_SLOW;           // anal_startDone, anal_fastDone
            case PRONE_DOGGY_SOFT -> to == GirlAction.PRONE_DOGGY_HARD;         // doggySwitch with sneak held
            default -> false;
        };
    }

    @Override
    protected int stripEndTicks() {
        return 100; // stripDone at 5.0 s
    }

    @Override
    protected void tickAction(GirlAction action, int ticks) {
        switch (action) {
            case TALK_RESPONSE -> {
                if (ticks >= TALK_RESPONSE_TICKS) goToBed();
            }
            case ANAL_PREPARE -> {
                if (ticks >= SETTLE_TICKS) setAction(GirlAction.ANAL_WAIT);
            }
            case SITDOWN -> {
                if (ticks == COME_HERE_TICKS) sayNearby("comehere");
                if (ticks >= SETTLE_TICKS) setAction(GirlAction.SITDOWNIDLE);
            }
            case PRONE_DOGGY_INSERT -> {
                if (ticks >= INSERT_TICKS) setAction(GirlAction.PRONE_DOGGY_SOFT);
            }
            case PRONE_DOGGY_HARD -> {
                if (ticks >= HARD_TICKS) setAction(GirlAction.PRONE_DOGGY_SOFT);
            }
            default -> super.tickAction(action, ticks);
        }
    }

    @Override
    protected void onAnimationEvent(String event) {
        if (this.level().isClientSide()) BiaSceneEvents.handle(this, event);
    }

    /* ------------------------------------------------------------------ */
    /* Talk, bed, and the wait for a partner                              */
    /* ------------------------------------------------------------------ */

    @Override
    public boolean awaitsChoice() {
        return getAction() == GirlAction.TALK_HORNY || getAction() == GirlAction.TALK_IDLE;
    }

    @Override
    public boolean endsOnDeclinedChoice() {
        return true;
    }

    @Override
    public void chooseMidScene(ServerPlayer player, SceneOption option) {
        this.chosenScene = option.firstStep();
        setAction(GirlAction.TALK_RESPONSE);
    }

    /**
     * Server: after her answer she lets the partner go and walks to a bed (1.12.2
     * {@code SendGirlToSexPacket} → {@code goToSexBed}): for anal the nearest bed's free side, for
     * doggy the end of a bed. With no bed she gives up.
     */
    private void goToBed() {
        boolean anal = this.chosenScene == GirlAction.ANAL_PREPARE;
        BedFinder.Plan plan = anal ? BedFinder.plan(this.level(), this.blockPosition()) : BedFinder.planBedEndStand(this.level(), this.blockPosition());
        releaseAnchor();
        setPartner(null);
        if (!plan.ok()) {
            this.level().playSound(null, this, ModSounds.GIRLS_BIA_BREATH.variant(2).value(), this.getSoundSource(), 1.0F, 1.0F);
            sayNearby(plan.failure() == BedFinder.Failure.OBSCURED ? "bedobscured" : "nobedinsight");
            endScene();
            return;
        }
        this.spot = plan.position();
        this.spotYaw = plan.yaw();
        this.walkTicks = 0;
        setAction(GirlAction.WALK_TO_BED);
        this.getNavigation().moveTo(this.spot.x, this.spot.y, this.spot.z, 0.35);
    }

    private void tickBed() {
        if (this.spot == null) return;
        GirlAction action = getAction();
        if (action == GirlAction.WALK_TO_BED) {
            this.walkTicks++;
            if (this.position().distanceTo(this.spot) >= 0.6 && this.walkTicks <= BED_WALK_TIMEOUT) {
                if (this.walkTicks == 60 || this.walkTicks == 120) {
                    this.getNavigation().stop();
                    this.getNavigation().moveTo(this.spot.x, this.spot.y, this.spot.z, 0.35);
                }
                return;
            }
            this.getNavigation().stop();
            this.setPos(this.spot);
            anchor(this.spot, this.spotYaw);
            this.noPhysics = true;
            if (this.chosenScene == GirlAction.ANAL_PREPARE) {
                setDressed(false);
                setAction(GirlAction.ANAL_PREPARE);
            } else {
                setAction(GirlAction.SITDOWN);
            }
        } else if (action == GirlAction.ANAL_WAIT || action == GirlAction.SITDOWNIDLE) {
            if (this.joinTicks < 0) {
                Player near = this.level().getNearestPlayer(this, LOOK_RANGE);
                if (near instanceof ServerPlayer player && !player.isSpectator() && near.distanceTo(this) <= JOIN_RANGE) {
                    setPartner(player);
                    this.joinTicks = 0;
                }
            } else if (++this.joinTicks >= JOIN_TICKS) {
                startBedScene();
            }
        }
    }

    /** Server: the partner who stepped up is put in place and the scene starts (1.12.2 {@code handleAnalState}). */
    private void startBedScene() {
        this.joinTicks = -1;
        if (!(getPartner() instanceof ServerPlayer partner) || this.spot == null) {
            endScene();
            return;
        }
        float yaw = this.spotYaw;
        if (getAction() == GirlAction.ANAL_WAIT) {
            Vec3 pos = this.spot.add(VectorMath.rotateByYaw(new Vec3(-0.3, -1.0, -0.5), yaw));
            partner.connection.teleport(pos.x, pos.y, pos.z, partner.getYRot(), partner.getXRot());
            setAction(GirlAction.ANAL_START);
        } else {
            setDressed(false);
            Vec3 pos = this.spot.add(VectorMath.rotateByYaw(new Vec3(0.0, 1.1875 - partner.getEyeHeight(), 0.5), yaw));
            // she lies down along the bed, a block further on
            anchor(this.spot.add(VectorMath.rotateByYaw(new Vec3(0.0, 0.0, 1.0), yaw)), yaw);
            this.noPhysics = true;
            partner.connection.teleport(pos.x, pos.y, pos.z, Mth.wrapDegrees(yaw), 60.0F);
            setAction(GirlAction.PRONE_DOGGY_INTRO);
        }
    }

    @Override
    public void endScene() {
        super.endScene();
        this.chosenScene = GirlAction.NULL;
        this.spot = null;
        this.joinTicks = -1;
    }

    /* ------------------------------------------------------------------ */
    /* Companion                                                          */
    /* ------------------------------------------------------------------ */

    @Override
    protected int attackTicks(int animation) {
        return animation == 0 ? 7 : 10; // attackDone at 0.32 s / 0.48 s
    }

    @Override
    public void onNewHome(ServerPlayer player) {
        sayTo(player, "newhome");
        this.level().playSound(null, this, ModSounds.GIRLS_BIA_BREATH.random(this.random).value(), this.getSoundSource(), 1.0F, 1.0F);
    }

    /** 1.12.2: she also drops up to three brown wool. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        int wool = this.random.nextInt(4);
        if (wool > 0) this.spawnAtLocation(level, new ItemStack(Items.WOOL.brown(), wool));
    }
}
