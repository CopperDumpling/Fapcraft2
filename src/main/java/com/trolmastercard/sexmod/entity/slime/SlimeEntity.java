package com.trolmastercard.sexmod.entity.slime;

import com.geckolib.animation.RawAnimation;
import com.trolmastercard.sexmod.client.scene.SlimeSceneEvents;
import com.trolmastercard.sexmod.entity.GirlAction;
import com.trolmastercard.sexmod.entity.GirlEntity;
import com.trolmastercard.sexmod.entity.SceneOption;
import com.trolmastercard.sexmod.registry.ModEffects;
import com.trolmastercard.sexmod.registry.ModEntities;
import com.trolmastercard.sexmod.sound.ModSounds;
import com.trolmastercard.sexmod.util.VectorMath;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The slime girl (1.12.2 {@code SlimeEntity}). Not a companion: no menu, no orders, no AI goals.
 * <ul>
 *   <li>She hops: every 100 ticks on the ground she turns and leaps 0.7 blocks/tick forward.
 *   Now and then a hop raises her "progress"; at 1 she undresses, from 2 she hunts the nearest
 *   player and shows hearts. The horny potion sets it to 2 at once.</li>
 *   <li>From 2, a player who touches her is grabbed for a blowjob; from 4 she gets down on all
 *   fours by herself and whoever touches her then gets doggy.</li>
 *   <li>A finished scene leaves her pregnant: 2400 ticks later she gives birth to a
 *   {@link WildSlimeEntity}, which grows into a new slime girl. She dresses again.</li>
 * </ul>
 */
public class SlimeEntity extends GirlEntity {
    /** Pregnancy progress (1.12.2 {@code TICKS_UNTIL_BIRTH}, saved as "hornyLevel"): 1 undressed, 2+ hunting, 4+ waits on all fours. */
    private static final EntityDataAccessor<Integer> DATA_PROGRESS = SynchedEntityData.defineId(SlimeEntity.class, EntityDataSerializers.INT);
    /** She carries a wild slime (1.12.2 {@code HORNY_LEVEL} != -1). */
    private static final EntityDataAccessor<Boolean> DATA_PREGNANT = SynchedEntityData.defineId(SlimeEntity.class, EntityDataSerializers.BOOLEAN);

    private static final int HOP_TURN_TICKS = 85;
    private static final int HOP_TICKS = 100;
    /** A counted hop raises the progress this many ticks after landing. */
    private static final int PROGRESS_TICKS = 50;
    private static final float HOP_UP = 0.9F;
    private static final double HOP_FORWARD = 0.7;
    private static final int PREGNANCY_TICKS = 2400;
    /** {@code undress} 0.92 s into undressing; {@code doggyGoOnBedDone} 1.6 s into getting down. */
    private static final int UNDRESS_TICKS = 18;
    private static final int GET_DOWN_TICKS = 32;
    private static final double GRAB_RANGE = 1.0;
    private static final double HUNT_RANGE = 30.0;
    /** The grabbed player stands this far in front of her. */
    private static final double PARTNER_DISTANCE = 0.65;

    /** Server: hop timing and the direction of the next hop. */
    private int hopTicks;
    private boolean wasOnGround = true;
    private boolean hopCounts;
    private float hopYaw;
    /** Server: ticks until the birth, or -1. */
    private int birthTicks = -1;

    /** Client: the hop animation cycle (1.12.2 {@code SlimeEntityState}). */
    public enum Hop {
        IDLE("idle", true), START("jumpstart", false), AIR("jumpair", true), END("jumpend", false);

        final String animation;
        final boolean loops;

        Hop(String animation, boolean loops) {
            this.animation = animation;
            this.loops = loops;
        }
    }

    private Hop hop = Hop.IDLE;
    private int clientHopTicks;
    private boolean clientWasOnGround = true;

    public SlimeEntity(EntityType<? extends SlimeEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createGirlAttributes();
    }

    @Override
    protected boolean isCompanion() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_PROGRESS, 0);
        builder.define(DATA_PREGNANT, false);
    }

    @Override
    protected String animationPrefix() {
        return "animation.slime.";
    }

    @Override
    protected String blinkAnimationName() {
        return "fhappy";
    }

    @Override
    public String dialoguePrefix() {
        return "slime.dialogue.";
    }

    @Override
    public List<SceneOption> sceneOptions() {
        return List.of();
    }

    public int progress() {
        return this.entityData.get(DATA_PROGRESS);
    }

    private void setProgress(int progress) {
        this.entityData.set(DATA_PROGRESS, progress);
    }

    public boolean isPregnant() {
        return this.entityData.get(DATA_PREGNANT);
    }

    @Override
    public boolean shouldShowName() {
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    /* ------------------------------------------------------------------ */
    /* Ticking: hops, progress, pregnancy, grabbing                       */
    /* ------------------------------------------------------------------ */

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            clientTick();
            return;
        }
        if (getAction() == GirlAction.NULL) tickHop();
        tickPotion();
        tickBirth();
        tickGrab();
    }

    /** Server: the hop cycle (1.12.2 {@code handleJumpState}, {@code stopMovement}). */
    private void tickHop() {
        boolean onGround = this.onGround();
        if (!this.wasOnGround && onGround) {
            // one landing in ten counts towards the progress, unless she is pregnant
            this.hopCounts = !isPregnant() && this.random.nextFloat() < 0.1F;
        }
        if (this.hopTicks == HOP_TURN_TICKS) this.hopYaw = nextHopYaw();
        if (this.hopTicks == HOP_TICKS) {
            Vec3 forward = VectorMath.rotateByYaw(new Vec3(0.0, 0.0, HOP_FORWARD), this.hopYaw);
            this.setDeltaMovement(forward.x, HOP_UP, forward.z);
            this.needsSync = true;
            this.hopTicks = 0;
        }
        if (this.hopCounts && this.hopTicks == PROGRESS_TICKS) {
            int progress = progress() + 1;
            setProgress(progress);
            if (progress == 1) setAction(GirlAction.UNDRESS);
        }
        setYRot(this.hopYaw);
        setYBodyRot(this.hopYaw);
        setYHeadRot(this.hopYaw);
        if (onGround) this.hopTicks++;
        this.wasOnGround = onGround;
    }

    /** Server: a random direction, or towards the nearest player once she hunts (1.12.2 {@code getBirthProgress}). */
    private float nextHopYaw() {
        if (!isPregnant() && progress() >= 2) {
            Player player = this.level().getNearestPlayer(this, HUNT_RANGE);
            if (player != null && !inScene(player)) {
                return (float) Math.toDegrees(Math.atan2(this.getZ() - player.getZ(), this.getX() - player.getX())) + 90.0F;
            }
        }
        return this.random.nextFloat() * 360.0F;
    }

    /** Server: the horny potion skips straight to hunting (1.12.2 {@code updateAITasks}). */
    private void tickPotion() {
        if (!isHorny() || isPregnant()) return;
        setProgress(2);
        if (isDressed()) setAction(GirlAction.UNDRESS);
        removeEffect(ModEffects.HORNY);
    }

    /** Server: the pregnancy ends in a wild slime at her feet (1.12.2 {@code handleHornyJump}). */
    private void tickBirth() {
        if (this.birthTicks < 0) return;
        if (this.birthTicks == 0) {
            this.level().playSound(null, this, ModSounds.MISC_PLOB.variant(0).value(), this.getSoundSource(), 1.0F, 1.0F);
            WildSlimeEntity child = ModEntities.WILD_SLIME.get().create(this.level(), EntitySpawnReason.BREEDING);
            if (child != null) {
                child.setSize(1, true);
                child.snapTo(this.getX(), this.getY(), this.getZ(), this.random.nextFloat() * 360.0F, 0.0F);
                this.level().addFreshEntity(child);
            }
            this.entityData.set(DATA_PREGNANT, false);
        }
        this.birthTicks--;
    }

    /**
     * Server: from progress 2 a player touching her is grabbed; from 4 she first gets down on all
     * fours by herself, and a player touching her then gets doggy (1.12.2 {@code checkInteractionTrigger}).
     */
    private void tickGrab() {
        if (progress() < 2 || getPartner() != null) return;
        if (progress() >= 4 && this.onGround() && getAction() == GirlAction.NULL) {
            anchor(this.position(), this.getYRot());
            this.noPhysics = true;
            setAction(GirlAction.STARTDOGGY);
            return;
        }
        Player near = this.level().getNearestPlayer(this, GRAB_RANGE);
        if (!(near instanceof ServerPlayer player) || player.isSpectator() || !player.onGround() || inScene(player)) return;
        float yaw = this.getYRot();
        anchor(this.position(), yaw);
        this.noPhysics = true;
        setPartner(player);
        Vec3 pos = this.position().add(VectorMath.rotateByYaw(new Vec3(0.0, 0.0, PARTNER_DISTANCE), yaw));
        player.connection.teleport(pos.x, pos.y, pos.z, Mth.wrapDegrees(yaw), player.getXRot());
        setAction(getAction() == GirlAction.WAITDOGGY ? GirlAction.DOGGYSTART : GirlAction.SUCKBLOWJOB);
    }

    /** Client: the hop animation cycle, hearts while she hunts, sparkles while pregnant. */
    private void clientTick() {
        if (getAction() == GirlAction.NULL) {
            boolean onGround = this.onGround();
            if (this.clientHopTicks == 90) this.hop = Hop.START;
            if (!this.clientWasOnGround && onGround) {
                this.hop = Hop.END;
                this.clientHopTicks = 0;
            }
            if (onGround) this.clientHopTicks++;
            this.clientWasOnGround = onGround;
        }
        if (progress() >= 2 && this.tickCount % 10 == 0) particle(ParticleTypes.HEART);
        if (isPregnant()) particle(ParticleTypes.WITCH);
    }

    /** One particle somewhere around her (1.12.2 {@code spawnParticlesAround}). */
    private void particle(ParticleOptions type) {
        double x = this.getX() + this.random.nextFloat() * this.getBbWidth() * 2.0F - this.getBbWidth();
        double y = this.getY() + 0.5 + this.random.nextFloat() * this.getBbHeight();
        double z = this.getZ() + this.random.nextFloat() * this.getBbWidth() * 2.0F - this.getBbWidth();
        this.level().addParticle(type, x, y, z, this.random.nextGaussian() * 0.02, this.random.nextGaussian() * 0.02, this.random.nextGaussian() * 0.02);
    }

    /** Client: a hop animation finished (events {@code jumpStartDone}, {@code jumpEndDone}). */
    public void setHop(Hop hop) {
        this.hop = hop;
    }

    /* ------------------------------------------------------------------ */
    /* Scenes                                                             */
    /* ------------------------------------------------------------------ */

    @Override
    protected @Nullable RawAnimation actionAnimation(GirlAction action) {
        return switch (action) {
            case NULL -> this.hop.loops ? loop(this.hop.animation) : once(this.hop.animation);
            case UNDRESS -> once("undress");
            case SUCKBLOWJOB -> loop("blowjobsuck");
            case THRUSTBLOWJOB -> loop("blowjobthrust");
            case CUMBLOWJOB -> once("blowjobcum");
            case STARTDOGGY -> once("doggygoonbed");
            case WAITDOGGY -> loop("doggywait");
            case DOGGYSTART -> once("doggystart");
            case DOGGYSLOW -> loop("doggyslow");
            case DOGGYFAST -> loop("doggyfast");
            case DOGGYCUM -> once("doggycum");
            default -> null;
        };
    }

    @Override
    protected String movementAnimation(boolean moving) {
        return "null"; // 1.12.2 had no movement controller: the hop is part of the action cycle
    }

    @Override
    protected boolean blinks(GirlAction action) {
        // 1.12.2's slime blinked only during the steps that allow it, never while idle
        return action != GirlAction.NULL && action.autoBlink;
    }

    @Override
    public @Nullable GirlAction sneakStep(GirlAction action) {
        return switch (action) {
            case SUCKBLOWJOB -> GirlAction.THRUSTBLOWJOB;
            case DOGGYSLOW -> GirlAction.DOGGYFAST;
            default -> null;
        };
    }

    @Override
    public @Nullable GirlAction climaxStep(GirlAction action) {
        return switch (action) {
            case SUCKBLOWJOB, THRUSTBLOWJOB -> GirlAction.CUMBLOWJOB;
            case DOGGYSLOW, DOGGYFAST -> GirlAction.DOGGYCUM;
            default -> null;
        };
    }

    @Override
    protected boolean canStep(GirlAction from, GirlAction to) {
        if (to == sneakStep(from) || to == climaxStep(from)) return true;
        return switch (from) {
            case THRUSTBLOWJOB -> to == GirlAction.SUCKBLOWJOB;   // bjtDone
            case DOGGYSTART, DOGGYFAST -> to == GirlAction.DOGGYSLOW; // doggystartDone, doggyfastDone
            case CUMBLOWJOB, DOGGYCUM -> to == GirlAction.NULL;  // bjcDone, doggyCumDone
            default -> false;
        };
    }

    @Override
    protected int stripEndTicks() {
        return 100;
    }

    @Override
    protected void tickAction(GirlAction action, int ticks) {
        switch (action) {
            case UNDRESS -> {
                if (ticks >= UNDRESS_TICKS) {
                    setDressed(false);
                    setAction(GirlAction.NULL);
                }
            }
            case STARTDOGGY -> {
                if (ticks >= GET_DOWN_TICKS) setAction(GirlAction.WAITDOGGY);
            }
            default -> super.tickAction(action, ticks);
        }
    }

    /**
     * Server: any end of a scene dresses her and resets the progress (1.12.2 {@code reinitTasks});
     * a finished one also leaves her pregnant.
     */
    @Override
    public void endScene() {
        GirlAction action = getAction();
        super.endScene();
        if (action == GirlAction.CUMBLOWJOB || action == GirlAction.DOGGYCUM) {
            this.birthTicks = PREGNANCY_TICKS;
            this.entityData.set(DATA_PREGNANT, true);
        }
        setProgress(0);
        setDressed(true);
    }

    @Override
    protected void onAnimationEvent(String event) {
        if (this.level().isClientSide()) SlimeSceneEvents.handle(this, event);
    }

    /* ------------------------------------------------------------------ */
    /* Save data                                                          */
    /* ------------------------------------------------------------------ */

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Progress", progress());
        output.putInt("BirthTicks", this.birthTicks);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setProgress(input.getIntOr("Progress", 0));
        this.birthTicks = input.getIntOr("BirthTicks", -1);
        this.entityData.set(DATA_PREGNANT, this.birthTicks >= 0);
        if (progress() != 0) setDressed(false);
    }
}
