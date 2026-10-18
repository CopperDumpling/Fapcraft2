package com.trolmastercard.sexmod.entity.bee;

import com.geckolib.animation.RawAnimation;
import com.trolmastercard.sexmod.client.ClientHooks;
import com.trolmastercard.sexmod.client.scene.BeeSceneEvents;
import com.trolmastercard.sexmod.entity.GirlAction;
import com.trolmastercard.sexmod.entity.GirlEntity;
import com.trolmastercard.sexmod.entity.SceneOption;
import com.trolmastercard.sexmod.registry.ModEffects;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SpellParticleOption;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The bee girl (1.12.2 {@code BeeEntity}). Not a companion: no weapons, menu or regeneration.
 * <ul>
 *   <li>She flies (never more than 3 blocks above the ground) and glides down slowly while she
 *   isn't in the mood; no fall damage.</li>
 *   <li>Her mood builds up while she is alone with nobody to follow: 1600 ticks after she
 *   appears, and 4800 after each scene, she flies to the nearest player within 10 blocks and,
 *   within 1.5, starts her scene with them. The horny potion puts her in the mood at once.</li>
 *   <li>After a scene she sparkles for a while, then takes to the player (hearts) or not (angry);
 *   once she has, she carries a chest: right-click opens her dialogue (follow, go home, set home,
 *   open her 27-slot chest).</li>
 * </ul>
 */
public class BeeEntity extends GirlEntity {
    /** She carries a chest (1.12.2 {@code HORNY_FLAG} 112, saved as "hasChest"). */
    private static final EntityDataAccessor<Boolean> DATA_HAS_CHEST = SynchedEntityData.defineId(BeeEntity.class, EntityDataSerializers.BOOLEAN);

    public static final int CHEST_SIZE = 27;
    /** 1.12.2 {@code hornyTimer}: she is in the mood from this many ticks on. */
    private static final int MOOD_TICKS = 4800;
    /** A new bee is in the mood after 1600 ticks. */
    private static final int START_MOOD = 3200;
    private static final double SEEK_RANGE = 10.0;
    private static final double START_RANGE = 1.5;
    /** The highest she flies above the ground. */
    private static final double CEILING = 3.0;
    /** After a scene: sparkles until she decides, and how long the answer shows. */
    private static final int DECIDE_TICKS = 200;
    private static final int ANSWER_END_TICKS = 250;
    private static final int HEARTS_END_TICKS = 40;
    /** The throw-pearl animation's first two {@code pearl} events (1.28 s, 1.72 s). */
    private static final int PEARL_THROW_TICKS = 26;
    private static final int PEARL_ARRIVE_TICKS = 34;
    private static final ParticleOptions SPELL = SpellParticleOption.create(ParticleTypes.EFFECT, 0xFFFFFFFF, 1.0F);

    private final SimpleContainer chest = new SimpleContainer(CHEST_SIZE) {
        @Override
        public boolean stillValid(Player player) {
            return BeeEntity.this.isAlive() && hasChest() && player.distanceToSqr(BeeEntity.this) <= 8.0 * 8.0;
        }
    };
    /** Server: her mood (1.12.2 {@code hornyTimer}). Not saved, as in 1.12.2. */
    private int mood = START_MOOD;
    /** Server: in the mood with a player within reach. */
    private boolean seeking;
    /** Server: ticks into the after-scene particles, or 0 (1.12.2 {@code eggState}). */
    private int afterglow;
    /** Server: how fast she flies; her follow goal speeds her up the further away her master is (1.12.2 {@code jumpMovementFactor}). */
    private float airSpeed = DEFAULT_AIR_SPEED;
    private static final float DEFAULT_AIR_SPEED = 0.02F;

    public BeeEntity(EntityType<? extends BeeEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl<>(this, 20, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createGirlAttributes()
            .add(Attributes.MAX_HEALTH, 12.0)
            .add(Attributes.MOVEMENT_SPEED, 0.2)
            .add(Attributes.FLYING_SPEED, 0.4)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation navigation = new FlyingPathNavigation(this, level);
        navigation.setCanFloat(true);
        return navigation;
    }

    @Override
    protected boolean isCompanion() {
        return false;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FollowGoal());
        this.goalSelector.addGoal(1, idleOnly(new PanicGoal(this, 1.25)));
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, idleOnly(new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F)));
        this.goalSelector.addGoal(3, alone(new WaterAvoidingRandomFlyingGoal(this, 1.0)));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_HAS_CHEST, false);
    }

    public boolean hasChest() {
        return this.entityData.get(DATA_HAS_CHEST);
    }

    public SimpleContainer getChest() {
        return this.chest;
    }

    @Override
    protected String animationPrefix() {
        return "animation.bee.";
    }

    @Override
    protected String blinkAnimationName() {
        return "null"; // her animations have no blink
    }

    @Override
    public String dialoguePrefix() {
        return "bee.dialogue.";
    }

    @Override
    public List<SceneOption> sceneOptions() {
        return List.of();
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected float getFlyingSpeed() {
        return this.airSpeed;
    }

    @Override
    protected int pearlThrowTicks() {
        return PEARL_THROW_TICKS;
    }

    @Override
    protected int pearlArriveTicks() {
        return PEARL_ARRIVE_TICKS;
    }

    /** Once she carries a chest, right-click opens her dialogue; before that she ignores it. */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !hasChest()) return InteractionResult.PASS;
        if (this.level().isClientSide()) {
            if (isBusy() || !takesOrdersFrom(player)) {
                ClientHooks.showBusy(this);
            } else {
                ClientHooks.openBeeDialogue(this);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /* ------------------------------------------------------------------ */
    /* Ticking: mood, flight, afterglow                                   */
    /* ------------------------------------------------------------------ */

    private boolean inMood() {
        return this.mood >= MOOD_TICKS;
    }

    /** While she flies to a player her wandering pauses. */
    @Override
    protected boolean ownErrand() {
        return this.seeking;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        tickPotion();
        tickMood();
        tickAfterglow();
        tickFlight();
    }

    /** Server: the potion puts her in the mood at once, and stays in effect no longer (1.12.2 {@code updateAITasks}). */
    private void tickPotion() {
        if (isHorny() && !inMood() && getPartner() == null) {
            removeEffect(ModEffects.HORNY);
            this.mood = Integer.MAX_VALUE;
        }
    }

    /**
     * Server: alone and masterless, her mood builds; once she is in the mood she flies to the
     * nearest player within 10 blocks and, within 1.5, starts her scene (1.12.2 {@code handleBeeIdle}).
     */
    private void tickMood() {
        this.seeking = false;
        if (getPartner() != null || hasMaster() || isBusy()) return;
        if (!inMood()) {
            this.mood++;
            return;
        }
        Player nearest = this.level().getNearestPlayer(this, SEEK_RANGE);
        if (!(nearest instanceof ServerPlayer player) || player.isSpectator() || inScene(player)) return;
        this.seeking = true;
        if (player.distanceTo(this) < START_RANGE) {
            startWith(player);
        } else if (this.tickCount % 10 == 0 || this.getNavigation().isDone()) {
            this.getNavigation().moveTo(player, 1.0);
        }
    }

    /** Server: she stops one block in front of the player, facing them, and the player steps up to her. */
    private void startWith(ServerPlayer player) {
        this.mood = 0;
        this.seeking = false;
        setPartner(player);
        anchor(inFrontOf(player, 1.0), player.getYRot() - 180.0F);
        Vec3 pos = inFrontOf(player, 0.2);
        player.connection.teleport(pos.x, pos.y, pos.z, player.getYRot(), player.getXRot());
        setAction(GirlAction.CITIZEN_START);
    }

    /**
     * Server, from her climax on: sparkles, then (if she has no chest yet) a coin flip whether she
     * takes to the player: hearts and a chest, or angry clouds. With a chest already, just hearts
     * (1.12.2 {@code doParticleStuff}).
     */
    private void tickAfterglow() {
        if (getAction() == GirlAction.CITIZEN_CUM) this.afterglow = Math.max(1, this.afterglow);
        if (this.afterglow == 0) return;
        this.afterglow++;
        if (hasChest() && this.afterglow < DECIDE_TICKS) {
            // she had her chest before this scene
            if (this.afterglow < HEARTS_END_TICKS) {
                particles(ParticleTypes.HEART, 1);
            } else {
                this.afterglow = 0;
            }
        } else if (this.afterglow < DECIDE_TICKS) {
            particles(SPELL, 1);
        } else if (this.afterglow == DECIDE_TICKS) {
            this.entityData.set(DATA_HAS_CHEST, this.random.nextBoolean());
        } else if (this.afterglow < ANSWER_END_TICKS) {
            particles(hasChest() ? ParticleTypes.HEART : ParticleTypes.ANGRY_VILLAGER, 3);
        } else {
            this.afterglow = 0;
        }
        particles(SPELL, 10);
    }

    private void particles(ParticleOptions type, int count) {
        ((ServerLevel) this.level()).sendParticles(type, this.getX(), this.getY() + 0.3, this.getZ(), count, 0.2, 0.3, 0.2, 0.25);
    }

    /** Server: no climbing more than 3 blocks above the ground, and a slow glide down while not in the mood. */
    private void tickFlight() {
        Vec3 motion = this.getDeltaMovement();
        if (!inMood() && !this.onGround() && motion.y < 0.0) {
            motion = new Vec3(motion.x, motion.y * 0.4, motion.z);
        }
        if (motion.y > 0.0) {
            Vec3 pos = this.position();
            BlockHitResult ground = this.level().clip(new ClipContext(pos, new Vec3(pos.x, this.level().getMinY(), pos.z),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
            if (ground.getType() == HitResult.Type.BLOCK && pos.y - ground.getBlockPos().getY() > CEILING) {
                motion = new Vec3(motion.x, 0.0, motion.z);
            }
        }
        this.setDeltaMovement(motion);
    }

    /* ------------------------------------------------------------------ */
    /* Following                                                          */
    /* ------------------------------------------------------------------ */

    /**
     * 1.12.2 {@code GirlGotoGoal}: once her master is more than 5 blocks away she flies to them
     * (faster the further they are), keeps going for 3 seconds after getting within 5, and is
     * put next to them if they are beyond her path range.
     */
    private final class FollowGoal extends Goal {
        private static final double NEAR = 5.0;
        private static final int LINGER_TICKS = 60;
        private int lingerTicks;

        FollowGoal() {
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return master() != null;
        }

        private @Nullable Player master() {
            return isBusy() || getAction() != GirlAction.NULL ? null : getMaster();
        }

        @Override
        public void tick() {
            Player master = master();
            if (master == null) return;
            float distance = distanceTo(master);
            if (distance <= NEAR && --this.lingerTicks < 0) {
                this.lingerTicks = 0;
                getNavigation().stop();
                airSpeed = DEFAULT_AIR_SPEED;
                return;
            }
            if (distance > NEAR) this.lingerTicks = LINGER_TICKS;
            if (distance < getAttributeValue(Attributes.FOLLOW_RANGE)) {
                if (tickCount % 10 == 0 || getNavigation().isDone()) getNavigation().moveTo(master, 0.5);
            } else {
                teleportNear(master);
            }
            airSpeed = DEFAULT_AIR_SPEED + (float) Math.min(0.7, Math.floor(distance / 3.0F) * 0.05);
        }

        /** Up to 20 tries at a random spot within 10 blocks of the master, else onto the master. */
        private void teleportNear(Player master) {
            for (int i = 0; i < 20; i++) {
                double x = master.getBlockX() + random.nextInt(10);
                double z = master.getBlockZ() + random.nextInt(10);
                if (randomTeleport(x, master.getBlockY(), z, false, state -> false)) {
                    setDeltaMovement(Vec3.ZERO);
                    return;
                }
            }
            setPos(master.position());
            setDeltaMovement(Vec3.ZERO);
        }

        @Override
        public void stop() {
            getNavigation().stop();
            airSpeed = DEFAULT_AIR_SPEED;
            this.lingerTicks = 0;
        }
    }

    /* ------------------------------------------------------------------ */
    /* Scene                                                              */
    /* ------------------------------------------------------------------ */

    @Override
    protected @Nullable RawAnimation actionAnimation(GirlAction action) {
        return switch (action) {
            case CITIZEN_START -> once("sex_start");
            case CITIZEN_SLOW -> loop("sex_slow");
            case CITIZEN_FAST -> loop("sex_fast");
            case CITIZEN_CUM -> once("sex_cum");
            case THROW_PEARL -> loop("throw_pearl");
            default -> null;
        };
    }

    /** She always flies: the same idle whether moving or not, with her chest once she has one. */
    @Override
    protected String movementAnimation(boolean moving) {
        return hasChest() ? "idle_has_chest" : "idle";
    }

    @Override
    public @Nullable GirlAction sneakStep(GirlAction action) {
        return action == GirlAction.CITIZEN_SLOW ? GirlAction.CITIZEN_FAST : null;
    }

    @Override
    public @Nullable GirlAction climaxStep(GirlAction action) {
        return action == GirlAction.CITIZEN_SLOW || action == GirlAction.CITIZEN_FAST ? GirlAction.CITIZEN_CUM : null;
    }

    @Override
    protected boolean canStep(GirlAction from, GirlAction to) {
        if (to == sneakStep(from) || to == climaxStep(from)) return true;
        return switch (from) {
            case CITIZEN_START, CITIZEN_FAST -> to == GirlAction.CITIZEN_SLOW; // sex_startDone, sex_fastDone
            case CITIZEN_CUM -> to == GirlAction.NULL;                        // sex_cumDone
            default -> false;
        };
    }

    @Override
    protected int stripEndTicks() {
        return 0; // she never strips
    }

    @Override
    protected void onAnimationEvent(String event) {
        if (this.level().isClientSide()) BeeSceneEvents.handle(this, event);
    }

    /* ------------------------------------------------------------------ */
    /* Save data                                                          */
    /* ------------------------------------------------------------------ */

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("HasChest", hasChest());
        ContainerHelper.saveAllItems(output.child("Chest"), this.chest.getItems());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_HAS_CHEST, input.getBooleanOr("HasChest", false));
        input.child("Chest").ifPresent(saved -> {
            this.chest.clearContent();
            ContainerHelper.loadAllItems(saved, this.chest.getItems());
        });
    }
}
