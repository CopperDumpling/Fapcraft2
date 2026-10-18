package com.trolmastercard.sexmod.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import com.trolmastercard.sexmod.Fapcraft;
import com.trolmastercard.sexmod.client.ClientHooks;
import com.trolmastercard.sexmod.entity.ai.FollowMasterGoal;
import com.trolmastercard.sexmod.entity.ai.GatedGoal;
import com.trolmastercard.sexmod.registry.ModEffects;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Base for every NPC girl: synced scene state, the shared AI goals, save data and the three
 * GeckoLib controllers (movement, eyes, action) that 1.12.2's {@code BaseGirlEntity} used.
 * <p>
 * Scene state is changed only on the server ({@link #setAction}); clients render it.
 */
public abstract class GirlEntity extends PathfinderMob implements GeoEntity {
    private static final EntityDataAccessor<Integer> DATA_ACTION = SynchedEntityData.defineId(GirlEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_DRESSED = SynchedEntityData.defineId(GirlEntity.class, EntityDataSerializers.BOOLEAN);
    /** Entity id of the scene partner, or -1. */
    private static final EntityDataAccessor<Integer> DATA_PARTNER = SynchedEntityData.defineId(GirlEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> DATA_ANCHORED = SynchedEntityData.defineId(GirlEntity.class, EntityDataSerializers.BOOLEAN);
    /** The {@link SceneOption#id()} being played, or "". */
    private static final EntityDataAccessor<String> DATA_OPTION = SynchedEntityData.defineId(GirlEntity.class, EntityDataSerializers.STRING);
    /** True for scenes that don't lock the partner (dressing up). */
    private static final EntityDataAccessor<Boolean> DATA_SOLO = SynchedEntityData.defineId(GirlEntity.class, EntityDataSerializers.BOOLEAN);
    /** Entity id of the player she follows (1.12.2 {@code MASTER}), or -1. */
    private static final EntityDataAccessor<Integer> DATA_MASTER = SynchedEntityData.defineId(GirlEntity.class, EntityDataSerializers.INT);
    /** {@link Gait} ordinal: which walk animation plays (1.12.2 {@code WALK_SPEED}). */
    private static final EntityDataAccessor<Integer> DATA_GAIT = SynchedEntityData.defineId(GirlEntity.class, EntityDataSerializers.INT);
    /** What she holds: 0 nothing, 1 her weapon, 2 her bow (1.12.2 {@code ATTACK_MODE}). */
    private static final EntityDataAccessor<Integer> DATA_ATTACK_MODE = SynchedEntityData.defineId(GirlEntity.class, EntityDataSerializers.INT);
    /** Which of the three attack animations the current {@link GirlAction#ATTACK} plays (1.12.2 {@code nextAttack}). */
    private static final EntityDataAccessor<Integer> DATA_ATTACK_ANIM = SynchedEntityData.defineId(GirlEntity.class, EntityDataSerializers.INT);
    /** Copies of the equipment slots for the renderer and menu (1.12.2 {@code WEAPON}, {@code BOW}, {@code HELMET_SLOT}, ...). */
    private static final List<EntityDataAccessor<ItemStack>> DATA_EQUIPMENT = Stream.of(GirlSlot.WEAPON, GirlSlot.BOW, GirlSlot.HELMET, GirlSlot.CHEST_PLATE, GirlSlot.PANTS, GirlSlot.SHOES)
        .map(slot -> SynchedEntityData.defineId(GirlEntity.class, EntityDataSerializers.ITEM_STACK))
        .toList();

    /** Inventory slots (1.12.2 {@code AbstractGirlNpcEntity.inventory}): weapon, bow, helmet, chest, legs, boots, spare. */
    public static final int INVENTORY_SIZE = 7;
    /** Slots dropped when she dies (1.12.2 dropped the first six). */
    private static final int DROPPED_SLOTS = 6;
    /** The throw-pearl animation's two {@code pearl} events (0.958 s and 2.875 s): throw, then arrive. */
    private static final int PEARL_THROW_TICK = 19;
    private static final int PEARL_ARRIVE_TICK = 58;

    /** 1.12.2 {@code TEMPTATION_ITEMS}. */
    private static final Set<net.minecraft.world.item.Item> TEMPT_ITEMS = Set.of(Items.EMERALD, Items.DIAMOND, Items.GOLD_INGOT, Items.ENDER_PEARL);

    private final AnimatableInstanceCache animationCache = GeckoLibUtil.createInstanceCache(this);
    private final Map<String, RawAnimation> namedAnimations = new ConcurrentHashMap<>();
    /** Client: {@link #restartActionAnimation} is pending. */
    private boolean restartAction;

    /** Server: tick the current action started on (per girl; see {@link GirlAction}). */
    private int actionStartTick;
    /** Server: where the girl is held while anchored. */
    private @Nullable Vec3 anchorPos;
    private float anchorYaw;

    /** Server: the step that follows the payment animation (1.12.2 {@code animationFollowUp}). */
    private GirlAction pendingStep = GirlAction.NULL;
    /** Server: the step after a strip that was only a means to the chosen scene, or NULL to end. */
    private GirlAction followUp = GirlAction.NULL;
    /** Server: ticks into the walk-into-place before a scene (1.12.2 {@code setDismounted}), or -1. */
    private int approachTicks = -1;
    /** Server: partner position the scene's player poses are relative to (1.12.2 {@code cameraOriginPos}). */
    private @Nullable Vec3 sceneOrigin;
    /** Server: where she is walking to / stands at the bed, and which player ordered it. */
    private @Nullable Vec3 bedPos;
    private float bedYaw;
    private @Nullable UUID bedOrderer;
    private int bedTicks;
    /** Server: yaw of the scene frame: the direction the partner faces her (1.12.2 {@code cameraYaw}). */
    private float sceneYaw;

    private final SimpleContainer inventory = new SimpleContainer(INVENTORY_SIZE);
    /** Server: where "go home" sends her; her spawn point until a player sets one (1.12.2 {@code homePos}). */
    private @Nullable Vec3 home;
    /** Server: at 0 HP with a master she is downed instead of dying, until fully healed (1.12.2 {@code downed}). */
    private boolean downed;
    /** Server: the pearl she threw towards her home, while it flies. */
    private @Nullable ThrownEnderpearl pearl;

    protected GirlEntity(EntityType<? extends GirlEntity> type, Level level) {
        super(type, level);
        this.getNavigation().setCanOpenDoors(true);
        // 1.12.2: every NPC girl starts with an iron sword and a bow
        if (isCompanion()) {
            this.inventory.setItem(GirlSlot.WEAPON.index(), new ItemStack(Items.IRON_SWORD));
            this.inventory.setItem(GirlSlot.BOW.index(), new ItemStack(Items.BOW));
        }
    }

    public static AttributeSupplier.Builder createGirlAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.5)
            .add(Attributes.FOLLOW_RANGE, 30.0)
            .add(Attributes.TEMPT_RANGE, 10.0); // 26.3 TemptGoal needs it; 1.12.2 tempted within 10 blocks
    }

    /**
     * Whether she is a companion (1.12.2 {@code AbstractGirlNpcEntity}): armed, with the shared AI
     * goals, a menu, regeneration and the follow / home / equipment orders. False for girls that
     * only act on their own (the slime). Constant: it is read during construction.
     */
    protected boolean isCompanion() {
        return true;
    }

    /** Whether {@code player} is the partner of any girl near them. */
    public static boolean inScene(Player player) {
        return !player.level().getEntitiesOfClass(GirlEntity.class, player.getBoundingBox().inflate(16.0), girl -> girl.isPartner(player)).isEmpty();
    }

    /** Prefix of this girl's animation names, e.g. {@code "animation.jenny."}. */
    protected abstract String animationPrefix();

    /** Animation for a scene step (looping or played once and held), or null to show none. */
    protected abstract @Nullable RawAnimation actionAnimation(GirlAction action);

    /** Eyes animation while idle (blinking); 1.12.2 used per-girl names. */
    protected abstract String blinkAnimationName();

    /** The interaction menu. */
    public abstract List<SceneOption> sceneOptions();

    /** Step the partner's sneak key asks for during {@code action} (1.12.2 {@code getNextAction}), or null. */
    public abstract @Nullable GirlAction sneakStep(GirlAction action);

    /** Step a full meter + jump asks for during {@code action} (1.12.2 {@code getCumAction}), or null. */
    public abstract @Nullable GirlAction climaxStep(GirlAction action);

    /**
     * Whether the partner's client may move the scene from {@code from} to {@code to}
     * (animation keyframe events and the controls above). {@link GirlAction#NULL} ends the scene.
     * Payment → the chosen first step is always allowed.
     */
    protected abstract boolean canStep(GirlAction from, GirlAction to);

    /** Server: side effects of an accepted step, before it starts (1.12.2 {@code setCurrentAction} overrides). */
    protected void onStep(ServerPlayer partner, GirlAction from, GirlAction to) {
    }

    /**
     * Server, every tick of a scene step: steps that run on a timer instead of a keyframe
     * (e.g. strip, which has no partner whose client could report its events).
     */
    protected void tickAction(GirlAction action, int ticks) {
        if (action == GirlAction.STRIP) {
            if (ticks == stripToggleTicks()) setDressed(!isDressed());
            if (ticks >= stripEndTicks()) finishStrip();
        }
    }

    /**
     * Strip / dress up is timed on the server (it may have no partner client to report keyframes):
     * the tick its {@code becomeNude} event swaps the outfit, and the tick its {@code stripDone} ends it.
     */
    protected int stripToggleTicks() {
        return 60;
    }

    protected abstract int stripEndTicks();

    /** A named partner position the client may ask for during {@code action}, or null. */
    protected @Nullable PlayerPose scenePose(String name, GirlAction action) {
        return null;
    }

    /** Prefix of the dialogue keys her animation events may broadcast, e.g. {@code "jenny.dialogue."}. */
    public abstract String dialoguePrefix();

    /* ------------------------------------------------------------------ */
    /* Synced state                                                       */
    /* ------------------------------------------------------------------ */

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ACTION, GirlAction.NULL.ordinal());
        builder.define(DATA_DRESSED, true);
        builder.define(DATA_PARTNER, -1);
        builder.define(DATA_ANCHORED, false);
        builder.define(DATA_OPTION, "");
        builder.define(DATA_SOLO, false);
        builder.define(DATA_MASTER, -1);
        builder.define(DATA_GAIT, Gait.WALK.ordinal());
        builder.define(DATA_ATTACK_MODE, 0);
        builder.define(DATA_ATTACK_ANIM, 1);
        for (EntityDataAccessor<ItemStack> accessor : DATA_EQUIPMENT) {
            builder.define(accessor, ItemStack.EMPTY);
        }
    }

    public GirlAction getAction() {
        return GirlAction.byId(this.entityData.get(DATA_ACTION));
    }

    /** Server only. */
    public void setAction(GirlAction action) {
        if (this.level().isClientSide()) {
            Fapcraft.LOGGER.warn("setAction({}) called on the client for {}", action, this);
            return;
        }
        if (action != getAction()) {
            this.entityData.set(DATA_ACTION, action.ordinal());
            this.actionStartTick = this.tickCount;
        }
    }

    /** Server: the current action's timer starts over (1.12.2 {@code ResetControllerPacket}). */
    protected final void restartActionTimer() {
        this.actionStartTick = this.tickCount;
    }

    /** Ticks since the current action started (server). */
    public int actionTicks() {
        return this.tickCount - this.actionStartTick;
    }

    public boolean isDressed() {
        return this.entityData.get(DATA_DRESSED);
    }

    public void setDressed(boolean dressed) {
        this.entityData.set(DATA_DRESSED, dressed);
    }

    /**
     * True while she waits for her partner to pick a scene mid-scene (Ellie's hug): the partner's
     * menu then lists only {@link #availableOptions()}, and picking one goes to {@link #chooseMidScene}.
     */
    public boolean awaitsChoice() {
        return false;
    }

    /** Whether closing the mid-scene menu without picking ends the scene (1.12.2 Bia's {@code ac}; Ellie kept hugging). */
    public boolean endsOnDeclinedChoice() {
        return false;
    }

    /** Client: her partner closed the mid-scene menu without picking (Allie's wish menu then comes back). */
    public void onChoiceDeclined() {
    }

    /** Server: her partner picked {@code option} while she {@link #awaitsChoice()}. */
    public void chooseMidScene(ServerPlayer player, SceneOption option) {
    }

    /** The menu entries that apply to her current outfit. */
    public List<SceneOption> availableOptions() {
        return sceneOptions().stream().filter(o -> o.shownWhen().matches(isDressed())).toList();
    }

    public @Nullable SceneOption currentOption() {
        String id = this.entityData.get(DATA_OPTION);
        return sceneOptions().stream().filter(o -> o.id().equals(id)).findFirst().orElse(null);
    }

    /** True if the scene doesn't lock the partner. */
    public boolean isSolo() {
        return this.entityData.get(DATA_SOLO);
    }

    /** Under the horny potion she waives payment (1.12.2 {@code yFlag}). */
    public boolean isHorny() {
        return this.hasEffect(ModEffects.HORNY);
    }

    public @Nullable Player getPartner() {
        int id = this.entityData.get(DATA_PARTNER);
        return id >= 0 && this.level().getEntity(id) instanceof Player player ? player : null;
    }

    public boolean isPartner(Player player) {
        return this.entityData.get(DATA_PARTNER) == player.getId();
    }

    protected void setPartner(@Nullable Player player) {
        this.entityData.set(DATA_PARTNER, player == null ? -1 : player.getId());
    }

    /** True while a scene (or its approach) involves a player, or she is downed or going home: she takes no orders and her own AI pauses. */
    public boolean isBusy() {
        return this.entityData.get(DATA_PARTNER) >= 0 || getAction().busy;
    }

    public boolean isAnchored() {
        return this.entityData.get(DATA_ANCHORED);
    }

    /** Server: holds the girl still at {@code pos} facing {@code yaw}, with no gravity. */
    protected void anchor(Vec3 pos, float yaw) {
        // player yaw is unbounded (thousands of degrees after a lot of turning); sending that to
        // clients makes the model sweep through every full turn, so wrap it and snap the old angles
        yaw = Mth.wrapDegrees(yaw);
        this.anchorPos = pos;
        this.anchorYaw = yaw;
        this.yRotO = yaw;
        this.yBodyRotO = yaw;
        this.yHeadRotO = yaw;
        this.entityData.set(DATA_ANCHORED, true);
        this.setNoGravity(true);
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
    }

    protected void releaseAnchor() {
        this.anchorPos = null;
        this.entityData.set(DATA_ANCHORED, false);
        this.setNoGravity(false);
        this.noPhysics = false;
    }


    /* ------------------------------------------------------------------ */
    /* Companion: master, home, equipment, health                         */
    /* ------------------------------------------------------------------ */

    /** Which walk animation plays while she moves. */
    public enum Gait {
        WALK("walk"), FAST_WALK("fastwalk"), RUN("run");

        private static final Gait[] VALUES = values();
        final String animation;

        Gait(String animation) {
            this.animation = animation;
        }
    }

    /**
     * The player she follows and fights for. Not saved: as in 1.12.2 she stops following when
     * the master leaves the level (logs out, dies, changes dimension).
     */
    public @Nullable Player getMaster() {
        int id = this.entityData.get(DATA_MASTER);
        return id >= 0 && this.level().getEntity(id) instanceof Player player ? player : null;
    }

    public boolean hasMaster() {
        return this.entityData.get(DATA_MASTER) >= 0;
    }

    public boolean isMaster(Player player) {
        return this.entityData.get(DATA_MASTER) == player.getId();
    }

    /** Only her master may use her menu while she follows someone (1.12.2 {@code openInteractionMenu}). */
    public boolean takesOrdersFrom(Player player) {
        return !hasMaster() || isMaster(player);
    }

    /** Server: "follow me". */
    public void setMaster(Player player) {
        this.entityData.set(DATA_MASTER, player.getId());
    }

    /** Server: she stops following and walks again (1.12.2 {@code goHome}, which despite the name only did this). */
    public void clearMaster() {
        this.entityData.set(DATA_MASTER, -1);
        setGait(Gait.WALK);
    }

    public Gait getGait() {
        int id = this.entityData.get(DATA_GAIT);
        return id >= 0 && id < Gait.VALUES.length ? Gait.VALUES[id] : Gait.WALK;
    }

    public void setGait(Gait gait) {
        this.entityData.set(DATA_GAIT, gait.ordinal());
    }

    /** 0 empty hands, 1 weapon drawn, 2 bow drawn. */
    public int getAttackMode() {
        return this.entityData.get(DATA_ATTACK_MODE);
    }

    public void setAttackMode(int mode) {
        this.entityData.set(DATA_ATTACK_MODE, mode);
    }

    /** Which attack animation (0, 1, 2) the current {@link GirlAction#ATTACK} plays; 0 is a stab. */
    public int getAttackAnimation() {
        return this.entityData.get(DATA_ATTACK_ANIM);
    }

    /** How long attack animation {@code animation} runs before she can swing again (its {@code attackDone} event). */
    protected int attackTicks(int animation) {
        return 10;
    }

    /** Server: before a scene: off any mount, weapons away, out of a swing. */
    protected final void leaveCompanionPose() {
        if (this.isPassenger()) this.stopRiding();
        this.noPhysics = false;
        this.setNoGravity(false);
        setAttackMode(0);
        if (!getAction().busy) setAction(GirlAction.NULL);
    }

    /** Server: a swing starts the next attack animation, but only from idle (as in 1.12.2). */
    public void playAttack() {
        if (getAction() == GirlAction.NULL) setAction(GirlAction.ATTACK);
    }

    public SimpleContainer getInventory() {
        return this.inventory;
    }

    public ItemStack getWeapon() {
        return this.inventory.getItem(GirlSlot.WEAPON.index());
    }

    public ItemStack getBow() {
        return this.inventory.getItem(GirlSlot.BOW.index());
    }

    /** The synced copy of an equipment slot (weapon to shoes), for rendering on any side. */
    public ItemStack getShownEquipment(GirlSlot slot) {
        return slot.index() < DATA_EQUIPMENT.size() ? this.entityData.get(DATA_EQUIPMENT.get(slot.index())) : ItemStack.EMPTY;
    }

    /** The synced copy of what she holds, for rendering. */
    public ItemStack getHeldItem() {
        return switch (getAttackMode()) {
            case 1 -> getShownEquipment(GirlSlot.WEAPON);
            case 2 -> getShownEquipment(GirlSlot.BOW);
            default -> ItemStack.EMPTY;
        };
    }

    /** Server: "set new home": where she stands now. */
    public void setHome(Vec3 pos) {
        this.home = pos;
    }

    /** Server: her reply when told to stop following or to go home (1.12.2 {@code goHome} overrides). */
    public void onDismissed(ServerPlayer player) {
    }

    /** Server: her reply to "set new home", to the player who set it (1.12.2 {@code onArriveHome}). */
    public void onNewHome(ServerPlayer player) {
    }

    /** Server: tells one player a line of hers ("{@code <Name> text}"). */
    protected final void sayTo(ServerPlayer player, String keySuffix) {
        player.sendSystemMessage(Component.literal("<").append(getName()).append("> ").append(Component.translatable(dialoguePrefix() + keySuffix)));
    }

    private Vec3 home() {
        if (this.home == null) this.home = this.position();
        return this.home;
    }

    /**
     * Server: "go home". She stops following, turns to her home and throws an ender pearl
     * at it, then arrives there (1.12.2 {@code SendCompanionHomePacket}'s three steps, timed by
     * the animation's {@code pearl} events).
     */
    public void goHome() {
        clearMaster();
        Vec3 target = home();
        float yaw = (float) Math.toDegrees(Math.atan2(this.getZ() - target.z, this.getX() - target.x)) + 90.0F;
        anchor(this.position(), yaw);
        this.pearl = null;
        setAction(GirlAction.THROW_PEARL);
    }

    /** Ticks into her throw-pearl animation that she throws, and that she arrives home at the latest. */
    protected int pearlThrowTicks() {
        return PEARL_THROW_TICK;
    }

    protected int pearlArriveTicks() {
        return PEARL_ARRIVE_TICK;
    }

    private void tickPearl(int ticks) {
        if (ticks == pearlThrowTicks() && this.pearl == null) {
            Vec3 target = home();
            ThrownEnderpearl thrown = new ThrownEnderpearl(this.level(), this, new ItemStack(Items.ENDER_PEARL));
            float distance = (float) this.position().distanceTo(target);
            thrown.shoot(target.x - this.getX(), target.y - this.getY(), target.z - this.getZ(), Math.min(4.0F, distance * 0.1F), 0.0F);
            this.level().addFreshEntity(thrown);
            this.pearl = thrown;
        } else if (ticks >= pearlArriveTicks()) {
            arriveHome(home()); // the pearl missed (or is still flying): she goes there anyway
        }
    }

    /** Server: her pearl hit something. It carries her only if it landed near her home (1.12.2 {@code KoboldEggProjectileEntity}). */
    public void onPearlLanded(ThrownEnderpearl landed) {
        portalParticles(landed.position());
        if (landed == this.pearl) this.pearl = null;
        if (getAction() == GirlAction.THROW_PEARL && home().distanceTo(landed.position()) < 5.0) arriveHome(landed.position());
    }

    private void arriveHome(Vec3 pos) {
        if (this.pearl != null) {
            this.pearl.discard();
            this.pearl = null;
        }
        portalParticles(this.position());
        releaseAnchor();
        this.teleportTo(pos.x, pos.y, pos.z);
        this.resetFallDistance();
        setAction(GirlAction.NULL);
        clearMaster();
    }

    private void portalParticles(Vec3 pos) {
        ((ServerLevel) this.level()).sendParticles(ParticleTypes.PORTAL, pos.x, pos.y + 1.0, pos.z, 32, 0.2, 0.5, 0.2, 1.0);
    }

    public boolean isDowned() {
        return this.downed;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (!source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            // 1.12.2 GirlCombatProtection: nothing hurts her during a scene
            if (this.entityData.get(DATA_PARTNER) >= 0 || this.downed) return false;
            if (isCompanion() && hasMaster() && getHealth() - damage <= 0.0F) {
                // with a master a companion is downed at 1 HP instead of dying (1.12.2 GirlFollowGoal.a)
                this.downed = true;
                setAction(GirlAction.DOWNED);
                this.getNavigation().stop();
                damage = getHealth() - 1.0F;
            }
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public void heal(float amount) {
        if (this.downed && getHealth() + amount >= getMaxHealth()) getUp();
        super.heal(amount);
    }

    private void getUp() {
        this.downed = false;
        if (getAction() == GirlAction.DOWNED) setAction(GirlAction.NULL);
    }

    @Override
    protected void dropEquipment(ServerLevel level) {
        super.dropEquipment(level);
        for (int i = 0; i < DROPPED_SLOTS; i++) {
            ItemStack stack = this.inventory.removeItemNoUpdate(i);
            if (!stack.isEmpty()) this.spawnAtLocation(level, stack);
        }
    }

    /** Server, every tick: regeneration, the downed state, timed companion actions and the equipment copies. */
    private void tickCompanion() {
        if (this.home == null) this.home = this.position(); // where she spawned
        if (isCompanion() && this.tickCount % 80 == 0 && getHealth() < getMaxHealth()) {
            if (!hasMaster()) {
                heal(1.0F);
            } else {
                // out of a fight she heals faster, with hearts
                boolean monsters = !this.level().getEntitiesOfClass(Monster.class, this.getBoundingBox().inflate(7.0, 1.0, 7.0)).isEmpty();
                int amount = monsters ? 1 : 4;
                heal(amount);
                ((ServerLevel) this.level()).sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 1.0 + this.random.nextDouble(), this.getZ(),
                    amount, 1.0, 1.0, 1.0, this.random.nextGaussian());
            }
        }
        if (this.downed && !hasMaster()) getUp();
        GirlAction action = getAction();
        if (action == GirlAction.ATTACK && actionTicks() >= attackTicks(getAttackAnimation())) {
            // attackDone: the next swing plays the next of the three animations
            setAction(GirlAction.NULL);
            this.entityData.set(DATA_ATTACK_ANIM, (getAttackAnimation() + 1) % 3);
        } else if (action == GirlAction.THROW_PEARL) {
            tickPearl(actionTicks());
        }
        for (int i = 0; i < DATA_EQUIPMENT.size(); i++) {
            EntityDataAccessor<ItemStack> accessor = DATA_EQUIPMENT.get(i);
            ItemStack stack = this.inventory.getItem(i);
            if (!ItemStack.matches(this.entityData.get(accessor), stack)) this.entityData.set(accessor, stack.copy());
        }
    }

    /* ------------------------------------------------------------------ */
    /* AI + ticking                                                       */
    /* ------------------------------------------------------------------ */

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        if (isCompanion()) this.goalSelector.addGoal(1, new FollowMasterGoal(this));
        this.goalSelector.addGoal(2, alone(new TemptGoal(this, 0.4, stack -> isTemptItem(stack), false)));
        this.goalSelector.addGoal(3, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(5, idleOnly(new LookAtPlayerGoal(this, Player.class, 3.0F, 1.0F)));
        this.goalSelector.addGoal(5, alone(new WaterAvoidingRandomStrollGoal(this, 0.35)));
    }

    /** Runs {@code goal} only while she is doing nothing in particular. */
    protected final GatedGoal idleOnly(net.minecraft.world.entity.ai.goal.Goal goal) {
        return new GatedGoal(goal, () -> !isBusy() && getAction() == GirlAction.NULL && !ownErrand());
    }

    /** As {@link #idleOnly}, and only while she follows nobody (the follow goal wanders near the master instead). */
    protected final GatedGoal alone(net.minecraft.world.entity.ai.goal.Goal goal) {
        return new GatedGoal(goal, () -> !isBusy() && getAction() == GirlAction.NULL && !hasMaster() && !ownErrand());
    }

    /** Server: she is about something of her own (Luna heading for water), so her idle goals pause. */
    protected boolean ownErrand() {
        return false;
    }

    private static boolean isTemptItem(ItemStack stack) {
        return TEMPT_ITEMS.contains(stack.getItem());
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!isCompanion()) return InteractionResult.PASS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (this.level().isClientSide()) {
            if (isBusy() || !takesOrdersFrom(player)) {
                ClientHooks.showBusy(this);
            } else {
                ClientHooks.openActionMenu(this);
            }
        }
        return InteractionResult.SUCCESS;
    }

    /* ------------------------------------------------------------------ */
    /* Scenes (server)                                                    */
    /* ------------------------------------------------------------------ */

    /**
     * Starts a scene chosen from the menu (payment already taken). She stops, the partner is
     * nudged forward and she walks into place in front of them over 40 ticks; then she is
     * anchored and the payment animation plays (1.12.2 {@code KoboldStatePacket} +
     * {@code setDismounted}).
     */
    public boolean startScene(ServerPlayer player, SceneOption option) {
        if (isBusy()) return false;
        leaveCompanionPose();
        setPartner(player);
        this.entityData.set(DATA_OPTION, option.id());
        if (option.needsNude() && isDressed()) {
            this.pendingStep = GirlAction.STRIP;
            this.followUp = option.firstStep();
        } else {
            this.pendingStep = option.firstStep();
            this.followUp = GirlAction.NULL;
        }
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        Vec3 snap = inFrontOf(player, 0.35);
        player.connection.teleport(snap.x, snap.y, snap.z, player.getYRot(), player.getXRot());
        this.approachTicks = 0;
        this.sceneYaw = Mth.wrapDegrees(player.getYHeadRot() + 180.0F); // she faces the partner while walking in
        return true;
    }

    /** A scene that needs no partner lock and no payment animation (dress up). */
    public boolean startSolo(ServerPlayer player, SceneOption option) {
        if (isBusy()) return false;
        leaveCompanionPose();
        setPartner(player);
        this.entityData.set(DATA_OPTION, option.id());
        this.entityData.set(DATA_SOLO, true);
        this.followUp = GirlAction.NULL;
        this.getNavigation().stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.lookAt(player, 30.0F, 30.0F);
        setAction(option.firstStep());
        return true;
    }

    /**
     * Applies a step requested by the partner's client, if it is legal. After the payment
     * animation the client's {@code to} is ignored: the step is the one she was asked for.
     */
    public void requestStep(ServerPlayer player, GirlAction from, GirlAction to) {
        if (!isPartner(player) || getAction() != from || from == GirlAction.NULL) return;
        if (from == GirlAction.PAYMENT) {
            advance(this.pendingStep);
            return;
        }
        if (!canStep(from, to)) {
            Fapcraft.LOGGER.debug("rejected scene step {} -> {} for {}", from, to, this);
            return;
        }
        advance(to);
    }

    /** Server: moves the scene to {@code to} (NULL ends it). */
    protected final void advance(GirlAction to) {
        if (to == GirlAction.NULL) {
            endScene();
            return;
        }
        if (to == GirlAction.STARTDOGGY && getAction() != GirlAction.WALK_TO_BED) {
            beginBedWalk();
            return;
        }
        if (getPartner() instanceof ServerPlayer partner) onStep(partner, getAction(), to);
        setAction(to);
    }

    /** What she would do for a bed-bound option right now; the menu refuses (before taking payment) if it fails. */
    public BedFinder.Plan planBed() {
        return BedFinder.plan(this.level(), this.blockPosition());
    }

    /** Server: she turns the request down because there is no usable bed (no payment is taken). */
    public void refuseBed(ServerPlayer player, BedFinder.Failure failure) {
        refuseSound();
        sayTo(player, failure == BedFinder.Failure.NO_BED ? "nobedinsight" : "bedobscured");
    }

    /** Server: her "hmph" when she refuses. */
    protected void refuseSound() {
    }

    /** Server: speaks a line of hers to everyone nearby ("{@code <Name> text}", translated per player). */
    protected void sayNearby(String keySuffix) {
        Component line = Component.literal("<").append(getName()).append("> ").append(Component.translatable(dialoguePrefix() + keySuffix));
        for (ServerPlayer nearby : ((ServerLevel) this.level()).players()) {
            if (nearby.distanceToSqr(this) <= 40 * 40) nearby.sendSystemMessage(line);
        }
    }

    /**
     * Server: after payment (and strip), she lets go of the partner and walks to the bed; the
     * partner joins her there on their own (1.12.2 {@code goToSexBed}, {@code zFlag}).
     */
    private void beginBedWalk() {
        BedFinder.Plan plan = planBed();
        Player orderer = getPartner();
        if (!plan.ok() || orderer == null) {
            endScene();
            return;
        }
        this.bedOrderer = orderer.getUUID();
        this.bedPos = plan.position();
        this.bedYaw = plan.yaw();
        this.bedTicks = 0;
        releaseAnchor();
        setPartner(null);
        this.sceneOrigin = null;
        setAction(GirlAction.WALK_TO_BED);
        this.getNavigation().moveTo(this.bedPos.x, this.bedPos.y, this.bedPos.z, 0.35);
    }

    private void tickBed() {
        if (this.bedPos == null) return;
        GirlAction action = getAction();
        if (action == GirlAction.WALK_TO_BED) {
            this.bedTicks++;
            boolean arrived = this.position().distanceTo(this.bedPos) < 0.6;
            if (!arrived && this.bedTicks <= 200) {
                if (this.bedTicks == 60 || this.bedTicks == 120) {
                    this.getNavigation().stop();
                    this.getNavigation().moveTo(this.bedPos.x, this.bedPos.y, this.bedPos.z, 0.35);
                }
                return;
            }
            this.getNavigation().stop();
            this.setPos(this.bedPos); // gave up walking: stand there anyway
            anchor(this.bedPos, this.bedYaw);
            this.noPhysics = true;
            setAction(GirlAction.STARTDOGGY);
        } else if (action == GirlAction.WAITDOGGY) {
            ServerPlayer orderer = this.bedOrderer == null ? null : ((ServerLevel) this.level()).getServer().getPlayerList().getPlayer(this.bedOrderer);
            if (orderer == null || actionTicks() > BED_WAIT_TICKS) {
                endScene();
            } else if (orderer.position().distanceTo(this.position()) < 0.5) {
                joinAtBed(orderer);
            }
        }
    }

    /** The ordering player walked into her: they take their place and the scene proper begins. */
    private void joinAtBed(ServerPlayer player) {
        setPartner(player);
        this.sceneYaw = this.bedYaw;
        this.sceneOrigin = this.position();
        player.connection.teleport(this.getX(), this.getY(), this.getZ(), player.getYRot(), player.getXRot());
        positionPartner(player, DOGGY_POSE);
        setAction(GirlAction.DOGGYSTART);
    }

    /** Server: the strip animation is over: continue to the scene it was for, or end. */
    protected final void finishStrip() {
        GirlAction next = this.followUp;
        this.followUp = GirlAction.NULL;
        advance(next);
    }

    /** A named request from the partner's client: a pose to move to, or a girl-specific flag. */
    public void requestNamed(ServerPlayer player, String name) {
        if (!isPartner(player)) return;
        PlayerPose pose = scenePose(name, getAction());
        if (pose != null) positionPartner(player, pose);
    }

    /** Ends the scene and frees both sides (natural end, leave key, or the partner leaving). */
    public void endScene() {
        releaseAnchor();
        setPartner(null);
        setAction(GirlAction.NULL);
        this.entityData.set(DATA_OPTION, "");
        this.entityData.set(DATA_SOLO, false);
        this.pendingStep = GirlAction.NULL;
        this.followUp = GirlAction.NULL;
        this.bedPos = null;
        this.bedOrderer = null;
        this.approachTicks = -1;
        this.sceneOrigin = null;
    }

    /** Server: from now on the partner's poses are measured from {@code origin}, facing {@code yaw}. */
    protected final void setSceneFrame(Vec3 origin, float yaw) {
        this.sceneOrigin = origin;
        this.sceneYaw = Mth.wrapDegrees(yaw);
    }

    protected final void positionPartner(ServerPlayer player, PlayerPose pose) {
        if (this.sceneOrigin == null) this.sceneOrigin = player.position();
        double yawRad = Math.toRadians(this.sceneYaw);
        double rightRad = Math.toRadians(this.sceneYaw + 90.0F);
        Vec3 pos = this.sceneOrigin
            .add(-Math.sin(rightRad) * pose.right(), pose.up(), Math.cos(rightRad) * pose.right())
            .add(-Math.sin(yawRad) * pose.forward(), 0.0, Math.cos(yawRad) * pose.forward());
        player.connection.teleport(pos.x, pos.y, pos.z, Mth.wrapDegrees(this.sceneYaw + pose.yaw()), pose.pitch());
    }

    /** {@code distance} blocks in front of the player, by body yaw (1.12.2 {@code getVectorTowardPlayer}). */
    protected static Vec3 inFrontOf(Player player, double distance) {
        double yaw = Math.toRadians(player.getYRot());
        return player.position().add(-Math.sin(yaw) * distance, 0.0, Math.cos(yaw) * distance);
    }

    private static final int BED_WAIT_TICKS = 6000;
    /** Partner's place when they join her at the bed (1.12.2 {@code positionPlayerRelative(0,0,0.4,0,60)}). */
    private static final PlayerPose DOGGY_POSE = new PlayerPose(0.0, 0.0, 0.4, 0.0F, 60.0F);

    private void tickScene() {
        tickBed();
        Player partner = getPartner();
        if (this.entityData.get(DATA_PARTNER) >= 0 && (partner == null || !partner.isAlive() || partner.distanceToSqr(this) > 16 * 16)) {
            endScene(); // partner logged out, died or teleported away
            return;
        }
        if (!this.level().isClientSide() && getAction() != GirlAction.NULL) tickAction(getAction(), actionTicks());
        if (this.approachTicks < 0 || partner == null) return;
        this.approachTicks++;
        if (this.approachTicks <= 40) {
            // one step of the remaining distance per tick; arrives exactly on tick 40
            Vec3 target = inFrontOf(partner, 1.0);
            int stepsLeft = 40 - this.approachTicks;
            Vec3 pos = stepsLeft == 0 ? target : this.position().add(target.subtract(this.position()).scale(1.0 / stepsLeft));
            this.setPos(pos);
            this.setDeltaMovement(Vec3.ZERO);
            this.setYRot(this.sceneYaw);
            this.setYBodyRot(this.sceneYaw);
            this.setYHeadRot(this.sceneYaw);
            return;
        }
        this.approachTicks = -1;
        float yaw = Mth.wrapDegrees(partner.getYRot() + 180.0F);
        anchor(this.position(), yaw);
        this.sceneYaw = Mth.wrapDegrees(yaw + 180.0F);
        this.sceneOrigin = null;
        SceneOption option = currentOption();
        if (isHorny() || (option != null && option.price().isEmpty())) {
            advance(this.pendingStep); // no payment to wait for
        } else {
            setAction(GirlAction.PAYMENT);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) {
            tickScene();
            tickCompanion();
        }
        if (!this.level().isClientSide() && isAnchored() && this.anchorPos != null) {
            this.setPos(this.anchorPos);
            this.setDeltaMovement(Vec3.ZERO);
            this.setYRot(this.anchorYaw);
            this.setYHeadRot(this.anchorYaw);
            this.setYBodyRot(this.anchorYaw);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean isPushable() {
        return !isAnchored() && super.isPushable();
    }

    /* ------------------------------------------------------------------ */
    /* Save data                                                          */
    /* ------------------------------------------------------------------ */

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Dressed", isDressed());
        if (this.home != null) output.store("Home", Vec3.CODEC, this.home);
        ContainerHelper.saveAllItems(output.child("Inventory"), this.inventory.getItems());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        setDressed(input.getBooleanOr("Dressed", true));
        this.home = input.read("Home", Vec3.CODEC).orElse(null);
        input.child("Inventory").ifPresent(saved -> {
            this.inventory.clearContent();
            ContainerHelper.loadAllItems(saved, this.inventory.getItems());
        });
        // scenes don't survive a reload (as in 1.12.2): start idle and unanchored
    }

    /* ------------------------------------------------------------------ */
    /* Animation                                                          */
    /* ------------------------------------------------------------------ */

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.animationCache;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("movement", 5, this::movementState)
            .setSoundKeyframeHandler(event -> onMovementAnimationEvent(event.keyframeData().getSound())));
        controllers.add(new AnimationController<>("eyes", 10, this::eyesState));
        controllers.add(new AnimationController<GirlEntity>("action", 0, this::actionState)
            .setSoundKeyframeHandler(event -> onAnimationEvent(event.keyframeData().getSound())));
    }

    /**
     * Client: starts the current scene step's animation over (1.12.2 {@code resetAnimationControllerOffset}).
     * Applied when the controller next asks for its animation: keyframe events call this from inside
     * the controller's update, where resetting it would break the rest of that update.
     */
    public void restartActionAnimation() {
        this.restartAction = true;
    }

    /** 1.12.2's animations put scene events in sound keyframes ({@code bjiDone}, ...). Client only. */
    protected void onAnimationEvent(String event) {
    }

    /** Events of her walk / idle animations (Luna's {@code idleDone}). Client only. */
    protected void onMovementAnimationEvent(String event) {
    }

    /** Client: how big the price items in her hand are drawn (Luna eats hers bite by bite). */
    public float paymentItemScale() {
        return 1.0F;
    }

    /** The companion actions' animations, named alike for every girl. */
    protected final @Nullable RawAnimation companionAnimation(GirlAction action) {
        return switch (action) {
            case ATTACK -> once("attack" + getAttackAnimation());
            case RIDE -> loop("ride");
            case SIT -> loop("sit");
            case THROW_PEARL -> loop("throwpearl");
            case DOWNED -> loop("downed");
            default -> null;
        };
    }

    protected final RawAnimation loop(String name) {
        return this.namedAnimations.computeIfAbsent("loop:" + name, n -> RawAnimation.begin().thenLoop(animationPrefix() + name));
    }

    protected final RawAnimation once(String name) {
        // 1.12.2 played non-looping scene steps once and held the last frame until the next step
        return this.namedAnimations.computeIfAbsent("once:" + name, n -> RawAnimation.begin().thenPlayAndHold(animationPrefix() + name));
    }

    private PlayState movementState(AnimationTest<GirlEntity> test) {
        String always = constantMovementAnimation();
        if (always != null) return test.setAndContinue(loop(always));
        if (getAction() != GirlAction.NULL && getAction() != GirlAction.WALK_TO_BED) return rest(test);
        if (this.isPassenger()) return test.setAndContinue(loop("sit"));
        return test.setAndContinue(loop(movementAnimation(test.isMoving())));
    }

    /** A movement animation that loops whatever she does (Allie's tail), or null for walk / idle. */
    protected @Nullable String constantMovementAnimation() {
        return null;
    }

    /** Walk / idle animation name while she does nothing else. */
    protected String movementAnimation(boolean moving) {
        return moving ? getGait().animation : "idle";
    }

    private PlayState eyesState(AnimationTest<GirlEntity> test) {
        return blinks(getAction()) ? test.setAndContinue(loop(blinkAnimationName())) : rest(test);
    }

    /** A controller with nothing to play: her empty {@code null} animation, or stopped if she has none. */
    private PlayState rest(AnimationTest<GirlEntity> test) {
        return hasEmptyNullAnimation() ? test.setAndContinue(loop("null")) : PlayState.STOP;
    }

    /** Whether her {@code null} animation is empty; Manglelie's isn't, so her idle controllers stop instead. */
    protected boolean hasEmptyNullAnimation() {
        return true;
    }

    /** Whether the eyes controller blinks during {@code action}. */
    protected boolean blinks(GirlAction action) {
        return action == GirlAction.NULL && action.autoBlink;
    }

    private PlayState actionState(AnimationTest<GirlEntity> test) {
        GirlAction action = getAction();
        RawAnimation raw = actionAnimation(action);
        test.controller().setTransitionTicks(action.transitionTicks);
        if (this.restartAction) {
            this.restartAction = false;
            test.controller().reset();
        }
        return raw == null ? rest(test) : test.setAndContinue(raw);
    }
}
