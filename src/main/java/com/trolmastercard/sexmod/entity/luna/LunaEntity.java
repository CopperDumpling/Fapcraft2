package com.trolmastercard.sexmod.entity.luna;

import com.geckolib.animation.RawAnimation;
import com.trolmastercard.sexmod.client.scene.LunaSceneEvents;
import com.trolmastercard.sexmod.entity.BedFinder;
import com.trolmastercard.sexmod.entity.GirlAction;
import com.trolmastercard.sexmod.entity.GirlEntity;
import com.trolmastercard.sexmod.entity.GirlSlot;
import com.trolmastercard.sexmod.entity.PlayerPose;
import com.trolmastercard.sexmod.entity.SceneOption;
import com.trolmastercard.sexmod.entity.SceneOption.Outfit;
import com.trolmastercard.sexmod.registry.ModItems;
import com.trolmastercard.sexmod.sound.ModSounds;
import com.trolmastercard.sexmod.util.VectorMath;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Luna: the catgirl who fishes (1.12.2 {@code LunaEntity}).
 * <ul>
 *   <li>The companion behaviour every girl shares ({@link GirlEntity}), with a fishing rod slot.
 *   Alone she moves twice as fast as with a master.</li>
 *   <li>Fishing: alone and idle, she finds deep water by a river, ocean, beach or swamp, walks to
 *   its shore, casts, and reels in a bite. Fish she eats; anything else she throws away. After
 *   three catches she rests for 1200 ticks. A cast that lands on the ground or an entity rules
 *   that spot out. The rod's lure enchantment speeds her up.</li>
 *   <li>Touch boobs (two salmon): intro, slow ⇄ fast on sneak, climax.</li>
 *   <li>Sex (three cod): she walks to the nearest bed, sits by it and calls; whoever stands within
 *   1.25 blocks of her for 25 ticks gets the sitting cowgirl: intro, slow ⇄ fast on sneak, climax.</li>
 *   <li>Headpat, free.</li>
 * </ul>
 */
public class LunaEntity extends GirlEntity {
    private static final List<SceneOption> OPTIONS = List.of(
        new SceneOption("sex", "action.names.sex", Items.COD, 3, GirlAction.WALK_TO_BED, Outfit.ANY, false, false, false, ""),
        new SceneOption("touchboobs", "action.names.touchboobs", Items.SALMON, 2, GirlAction.TOUCH_BOOBS_INTRO, Outfit.ANY, false, false, false, ""),
        new SceneOption("headpat", "action.names.headpat", null, 0, GirlAction.HEAD_PAT, Outfit.ANY, false, false, false, ""));

    /** The rod she casts while her hook is out (1.12.2 {@code af}). */
    private static final EntityDataAccessor<Boolean> DATA_CAST = SynchedEntityData.defineId(LunaEntity.class, EntityDataSerializers.BOOLEAN);
    /** What she reeled in (1.12.2 {@code ag}). */
    private static final EntityDataAccessor<ItemStack> DATA_CATCH = SynchedEntityData.defineId(LunaEntity.class, EntityDataSerializers.ITEM_STACK);
    /** The rod in her rod slot, for its enchantment glint (1.12.2 {@code az}). */
    private static final EntityDataAccessor<ItemStack> DATA_ROD = SynchedEntityData.defineId(LunaEntity.class, EntityDataSerializers.ITEM_STACK);
    /** How far the end of her path is; she runs past 3 blocks (1.12.2 {@code yFlag}). */
    private static final EntityDataAccessor<Float> DATA_PATH_LEFT = SynchedEntityData.defineId(LunaEntity.class, EntityDataSerializers.FLOAT);

    /** Ticks she rests after three catches (1.12.2 {@code ax}); she starts rested. */
    private static final int REST_TICKS = 1200;
    private static final int CATCHES_BEFORE_REST = 3;
    /** start_fishing: {@code rod_shoot} at 1.21 s, {@code start_fishingDone} at 2.83 s. */
    private static final int CAST_TICK = 24;
    private static final int START_DONE_TICKS = 57;
    /** eat_fishing: {@code eatingDone} at 6.46 s; throw_away: {@code throw_away} at 4.42 s, {@code eatingDone} at 5.0 s. */
    private static final int EAT_DONE_TICKS = 129;
    private static final int THROW_TICK = 88;
    private static final int THROW_DONE_TICKS = 100;
    /** She reels in when the bite has this many ticks left (1.12.2 {@code lureTimer == 15}). */
    private static final int REEL_AT = 15;
    /** Water search: up to 60 blocks out, 10 up or down, the first 49 finds (1.12.2 {@code findFishingSpot}). */
    private static final int SEARCH_RADIUS = 60;
    private static final int SEARCH_HEIGHT = 10;
    private static final int SEARCH_FINDS = 49;
    private static final int GOOD_DEPTH = 6;
    /** How often she looks for water, and how long a better spot must stay better before she switches. */
    private static final int SEARCH_INTERVAL = 20;
    private static final int SWITCH_TICKS = 20;

    /** wait: {@code call_playerMSG1} at 0.42 s. */
    private static final int CALL_TICK = 8;
    private static final double JOIN_RANGE = 1.25;
    private static final double LOOK_RANGE = 10.0;
    private static final int JOIN_TICKS = 25;
    private static final int BED_WALK_TIMEOUT = 200;
    /** The partner sits facing away from her lap; the fast stretch leans them back (1.12.2 {@code sitting_fastTp}). */
    private static final PlayerPose SITTING_POSE = new PlayerPose(0.0, -0.075, -0.7109375, 0.0F, 0.0F);
    private static final PlayerPose SITTING_FAST_POSE = new PlayerPose(0.0, -0.160625, -0.9925, 0.0F, 0.0F);

    // server: fishing
    private int restTicks = 8000;
    private int catches;
    private @Nullable BlockPos spot;
    private int spotDepth;
    private int switchTicks;
    private final Set<BlockPos> spoiledSpots = new HashSet<>();
    private @Nullable Path lastPath;
    private @Nullable LunaHookEntity hook;
    private long hookRemoveTime = -1;
    // server: the bed
    private boolean bedBound;
    private @Nullable Vec3 bedSpot;
    private float bedYaw;
    private int walkTicks;
    private int joinTicks;

    // client: how much of her catch and of the payment is left, and where the catch is
    private float catchScale = 1.0F;
    private float paymentScale = 1.0F;
    /** 0 at the hook, 1 in her hand (1.12.2 {@code zFlag}). */
    private float catchFlight;
    private boolean slowVariant;
    private boolean idleVariant;

    public LunaEntity(EntityType<? extends LunaEntity> type, Level level) {
        super(type, level);
        getInventory().setItem(GirlSlot.ROD.index(), new ItemStack(Items.FISHING_ROD));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createGirlAttributes().add(Attributes.MOVEMENT_SPEED, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_CAST, false);
        builder.define(DATA_CATCH, ItemStack.EMPTY);
        builder.define(DATA_ROD, ItemStack.EMPTY);
        builder.define(DATA_PATH_LEFT, 0.0F);
    }

    @Override
    protected String animationPrefix() {
        return "animation.cat.";
    }

    @Override
    protected String blinkAnimationName() {
        return "blink";
    }

    @Override
    public String dialoguePrefix() {
        return "luna.dialogue.";
    }

    @Override
    public List<SceneOption> sceneOptions() {
        return OPTIONS;
    }

    /* ------------------------------------------------------------------ */
    /* Animation                                                          */
    /* ------------------------------------------------------------------ */

    @Override
    protected @Nullable RawAnimation actionAnimation(GirlAction action) {
        return switch (action) {
            case NULL, WALK_TO_BED -> loop("null");
            case PAYMENT -> once("payment");
            case FISHING_START -> once("start_fishing");
            case FISHING_IDLE -> loop("idle_fishing");
            case FISHING_EAT -> once("eat_fishing");
            case FISHING_THROW_AWAY -> once("throw_away");
            case TOUCH_BOOBS_INTRO -> once("touch_boobs_intro");
            case TOUCH_BOOBS_SLOW -> loop(this.slowVariant ? "touch_boobs_slow1" : "touch_boobs_slow");
            case TOUCH_BOOBS_FAST -> loop("touch_boobs_fast");
            case TOUCH_BOOBS_CUM -> once("touch_boobs_cum");
            case WAIT_CAT -> once("wait");
            case COWGIRL_SITTING_INTRO -> once("sitting_intro");
            case COWGIRL_SITTING_SLOW -> loop("sitting_slow");
            case COWGIRL_SITTING_FAST -> loop("sitting_fast");
            case COWGIRL_SITTING_CUM -> once("sitting_cum");
            case HEAD_PAT -> loop("head_pat");
            case RIDE -> loop("sit"); // she has no ride animation; 1.12.2 sat her down
            case ATTACK, SIT, THROW_PEARL, DOWNED -> companionAnimation(action);
            default -> null;
        };
    }

    /** Walking near the end of her path, running further out, and in the air she flies (1.12.2 {@code animationPredicate}). */
    @Override
    protected String movementAnimation(boolean moving) {
        if (!moving) return this.idleVariant ? "idle2" : "idle";
        if (!this.onGround() || Math.abs(this.getY() - this.yo) >= 0.1) return "fly";
        return this.entityData.get(DATA_PATH_LEFT) < 3.0F ? "walk" : "run";
    }

    @Override
    protected void onMovementAnimationEvent(String event) {
        switch (event) {
            case "idleDone" -> this.idleVariant = this.random.nextInt(10) == 0;
            case "idle2Done" -> this.idleVariant = false;
            default -> {
            }
        }
    }

    @Override
    protected void onAnimationEvent(String event) {
        if (this.level().isClientSide()) LunaSceneEvents.handle(this, event);
    }

    /** What her hand shows: her rod while she casts and waits, else her drawn weapon. */
    @Override
    public ItemStack getHeldItem() {
        GirlAction action = getAction();
        if (action == GirlAction.FISHING_START || action == GirlAction.FISHING_IDLE) {
            // 1.12.2 held its own rod, wearing the enchantments of the one in her rod slot
            ItemStack rod = new ItemStack(ModItems.LUNA_ROD.get());
            rod.set(DataComponents.ENCHANTMENTS, this.entityData.get(DATA_ROD).get(DataComponents.ENCHANTMENTS));
            return rod;
        }
        return super.getHeldItem();
    }

    /** Whether her rod is cast with nothing on it yet (its model shows the cast rod). */
    public boolean isRodCast() {
        return this.entityData.get(DATA_CAST);
    }

    /** What she reeled in, if she holds it. */
    public ItemStack getCatch() {
        return this.entityData.get(DATA_CATCH);
    }

    /** Server: what she reeled in. */
    void setCatch(ItemStack stack) {
        this.entityData.set(DATA_CATCH, stack);
    }

    /* client render state, driven by her animation events and the hook renderer */

    public float catchScale() {
        return this.catchScale;
    }

    public void biteCatch() {
        this.catchScale -= 1.0F / 3.0F;
    }

    public float catchFlight() {
        return this.catchFlight;
    }

    public void setCatchFlight(float flight) {
        this.catchFlight = flight;
    }

    /** Client: her catch is gone (eaten or thrown). */
    public void resetCatchView() {
        this.catchScale = 1.0F;
        this.catchFlight = 0.0F;
    }

    @Override
    public float paymentItemScale() {
        return this.paymentScale;
    }

    public void bitePayment() {
        this.paymentScale -= 1.0F / 3.0F;
    }

    public void resetPaymentScale() {
        this.paymentScale = 1.0F;
    }

    /** Client: the slow touch picks one of its two loops each time round (1.12.2 {@code touch_boobs_slowDone}). */
    public void rollSlowVariant() {
        this.slowVariant = !this.slowVariant && this.random.nextBoolean();
    }

    /* ------------------------------------------------------------------ */
    /* Ticking                                                            */
    /* ------------------------------------------------------------------ */

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            // her catch is gone (the server may end eating before the eatingDone keyframe plays)
            if (getCatch().isEmpty()) resetCatchView();
            return;
        }
        this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(hasMaster() ? 0.5 : 1.0);
        Path path = this.getNavigation().getPath();
        if (path != null && path.getEndNode() != null) {
            this.entityData.set(DATA_PATH_LEFT, path.getEndNode().distanceTo(this.blockPosition()));
        }
        tickFishing();
        this.entityData.set(DATA_CAST, this.hook != null && getCatch().isEmpty());
        if (this.hook != null && this.level().getGameTime() == this.hookRemoveTime) {
            this.hook.discard();
            this.hook = null;
        }
        ItemStack rod = getInventory().getItem(GirlSlot.ROD.index());
        if (!ItemStack.matches(this.entityData.get(DATA_ROD), rod)) this.entityData.set(DATA_ROD, rod.copy());
    }

    @Override
    protected boolean ownErrand() {
        return this.spot != null;
    }

    private static boolean isFishing(GirlAction action) {
        return action == GirlAction.FISHING_START || action == GirlAction.FISHING_IDLE
            || action == GirlAction.FISHING_EAT || action == GirlAction.FISHING_THROW_AWAY;
    }

    /** Server: 1.12.2 {@code handleFishingIdle}. */
    private void tickFishing() {
        GirlAction action = getAction();
        boolean alone = !hasMaster() && getPartner() == null && !this.bedBound && (action == GirlAction.NULL || isFishing(action));
        if (!alone) {
            if (isRodCast()) stopFishing();
            return;
        }
        if (++this.restTicks < REST_TICKS) return;
        if (this.hook != null && this.hook.biteTicks() == REEL_AT) reelIn();
        if (!isFishing(getAction())) {
            if (this.tickCount % SEARCH_INTERVAL == 0) findSpot();
            walkToSpot();
        }
        Path path = this.getNavigation().getPath();
        if (this.spot != null && this.lastPath == null && path == null && !this.isInWater() && this.onGround()) {
            this.setSilent(true);
            if (getAction() == GirlAction.NULL) {
                float yaw = (float) (Mth.atan2(this.getZ() - this.spot.getZ(), this.getX() - this.spot.getX()) * Mth.RAD_TO_DEG) + 90.0F;
                anchor(this.position(), yaw);
                setAction(GirlAction.FISHING_START);
            }
        } else {
            this.lastPath = path;
        }
    }

    @Override
    protected void tickAction(GirlAction action, int ticks) {
        switch (action) {
            case FISHING_START -> {
                if (ticks == CAST_TICK) castRod();
                if (ticks >= START_DONE_TICKS) setAction(GirlAction.FISHING_IDLE);
            }
            case FISHING_EAT -> {
                if (ticks >= EAT_DONE_TICKS) finishCatch();
            }
            case FISHING_THROW_AWAY -> {
                if (ticks == THROW_TICK) throwCatch();
                if (ticks >= THROW_DONE_TICKS) finishCatch();
            }
            case WALK_TO_BED -> tickBedWalk();
            case WAIT_CAT -> tickWait(ticks);
            default -> super.tickAction(action, ticks);
        }
    }

    /* ------------------------------------------------------------------ */
    /* Fishing                                                            */
    /* ------------------------------------------------------------------ */

    /** The rod she fishes with: the one in her rod slot, else a plain one. */
    private ItemStack fishingRod() {
        ItemStack rod = getInventory().getItem(GirlSlot.ROD.index());
        return rod.isEmpty() ? new ItemStack(ModItems.LUNA_ROD.get()) : rod;
    }

    /** Server: the {@code rod_shoot} of her cast (1.12.2 {@code LunaRodItem.castFishingRod}). */
    private void castRod() {
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FISHING_BOBBER_THROW, this.getSoundSource(),
            0.5F, 0.4F / (this.random.nextFloat() * 0.4F + 0.8F));
        if (this.hook == null && this.spot != null) {
            double distance = this.position().distanceTo(Vec3.atLowerCornerOf(this.spot));
            int lureSpeed = (int) (EnchantmentHelper.getFishingTimeReduction((ServerLevel) this.level(), fishingRod(), this) * 20.0F);
            this.hook = LunaHookEntity.cast(this, distance, lureSpeed);
            this.level().addFreshEntity(this.hook);
        }
    }

    /** Server: she reels in a bite; the hook goes a second later. Fish she eats, the rest she throws away. */
    private void reelIn() {
        if (this.hook == null) return;
        this.hook.reelIn(this, fishingRod());
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.FISHING_BOBBER_RETRIEVE, this.getSoundSource(),
            1.0F, 0.4F / (this.random.nextFloat() * 0.4F + 0.8F));
        this.hookRemoveTime = this.level().getGameTime() + 20;
        ItemStack caught = getCatch();
        if (!caught.isEmpty()) setAction(caught.has(DataComponents.FOOD) ? GirlAction.FISHING_EAT : GirlAction.FISHING_THROW_AWAY);
    }

    /** Server: she tosses her catch over her shoulder (1.12.2 {@code dropHeldItem}). */
    private void throwCatch() {
        ItemEntity item = new ItemEntity(this.level(), this.getX(), this.getY(), this.getZ(), getCatch().copy());
        item.setDeltaMovement(VectorMath.rotateByYaw(new Vec3(0.0, 0.2 + this.random.nextFloat() * 0.1, -0.2 - this.random.nextFloat() * 0.1), this.getYRot()));
        this.level().addFreshEntity(item);
        setCatch(ItemStack.EMPTY);
    }

    /** Server: done with a catch; after three she rests (1.12.2 {@code onFishingTick}). */
    private void finishCatch() {
        stopFishing();
        if (++this.catches >= CATCHES_BEFORE_REST) {
            this.catches = 0;
            this.restTicks = 0;
        }
    }

    /** Server: the cast hit the ground or an entity: that spot is no good (1.12.2 {@code addCaughtItem}). */
    void onSpoiledCast() {
        if (this.spot != null) this.spoiledSpots.add(this.spot);
        stopFishing();
    }

    /** Server: back to idle (1.12.2 {@code clearFishingState}). */
    private void stopFishing() {
        this.spot = null;
        this.spotDepth = 0;
        this.switchTicks = 0;
        setCatch(ItemStack.EMPTY);
        this.setSilent(false);
        if (isFishing(getAction())) {
            releaseAnchor();
            setAction(GirlAction.NULL);
        }
        if (this.hook != null) {
            this.hook.discard();
            this.hook = null;
        }
    }

    /**
     * Server: the deepest water among the nearest she knows of, not ruled out (1.12.2
     * {@code findFishingSpot}). She keeps her spot unless a deeper one turns up, or the best one
     * stays another for a while.
     */
    private void findSpot() {
        BlockPos best = null;
        int bestDepth = 0;
        for (BlockPos found : nearbyWater()) {
            BlockPos surface = found;
            while (this.level().getBlockState(surface.above()).is(Blocks.WATER)) surface = surface.above();
            int depth = 1;
            for (BlockPos probe = surface; this.level().getBlockState(probe.below()).is(Blocks.WATER); probe = probe.below()) depth++;
            if (this.spoiledSpots.contains(surface)) continue;
            if (best == null || depth > bestDepth) {
                best = surface;
                bestDepth = depth;
                if (bestDepth >= GOOD_DEPTH) break;
            }
        }
        if (best == null) return;
        if (this.spot == null || this.spotDepth < bestDepth) {
            this.spot = best;
            this.spotDepth = bestDepth;
        }
        if (this.spot.equals(best)) {
            this.switchTicks = 0;
        } else if ((this.switchTicks += SEARCH_INTERVAL) > SWITCH_TICKS) {
            this.spot = best;
            this.spotDepth = bestDepth;
        }
    }

    /** Water blocks outward from her in square rings, in fishing biomes (1.12.2 {@code findNearestStructureBlock}). */
    private List<BlockPos> nearbyWater() {
        List<BlockPos> found = new ArrayList<>();
        BlockPos origin = this.blockPosition();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int ring = 1; ring < SEARCH_RADIUS; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.abs(dx) != ring && Math.abs(dz) != ring) continue;
                    for (int dy = -SEARCH_HEIGHT; dy <= SEARCH_HEIGHT; dy++) {
                        pos.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                        if (!this.level().getBlockState(pos).is(Blocks.WATER) || !isFishingBiome(this.level().getBiome(pos))) continue;
                        found.add(pos.immutable());
                        if (found.size() >= SEARCH_FINDS) return found;
                    }
                }
            }
        }
        return found;
    }

    /** 1.12.2: river, ocean, deep ocean, beach, stone beach, swamp. */
    private static boolean isFishingBiome(Holder<Biome> biome) {
        return biome.is(Biomes.RIVER) || biome.is(BiomeTags.IS_OCEAN) || biome.is(Biomes.BEACH) || biome.is(Biomes.STONY_SHORE) || biome.is(Biomes.SWAMP);
    }

    /**
     * Server: walks towards her spot, stopping where the next step would be into water (1.12.2
     * {@code handleFishingMove}).
     */
    private void walkToSpot() {
        if (this.spot == null) return;
        this.getNavigation().moveTo(this.spot.getX(), this.spot.getY(), this.spot.getZ(), 0.35);
        Path path = this.getNavigation().getPath();
        if (path == null || path.getNodeCount() <= path.getNextNodeIndex() + 1) return;
        Node next = path.getNode(path.getNextNodeIndex() + 1);
        Node end = path.getNode(path.getNodeCount() - 1);
        Vec3 endPos = new Vec3(end.x, end.y, end.z);
        if (this.position().distanceTo(endPos) < 0.75) {
            this.getNavigation().stop();
            this.setPos(endPos);
        }
        BlockPos nextPos = next.asBlockPos();
        if (this.level().getBlockState(nextPos.above()).is(Blocks.WATER) || this.level().getBlockState(nextPos).is(Blocks.WATER)
            || this.level().getBlockState(nextPos.below()).is(Blocks.WATER)) {
            this.getNavigation().stop();
        }
    }

    /* ------------------------------------------------------------------ */
    /* Scenes                                                             */
    /* ------------------------------------------------------------------ */

    @Override
    public boolean startScene(ServerPlayer player, SceneOption option) {
        if (isBusy()) return false;
        stopFishing();
        if (!super.startScene(player, option)) return false;
        if (option.firstStep() == GirlAction.TOUCH_BOOBS_INTRO) setDressed(false);
        return true;
    }

    @Override
    public boolean startSolo(ServerPlayer player, SceneOption option) {
        if (isBusy()) return false;
        stopFishing();
        return super.startSolo(player, option);
    }

    @Override
    public @Nullable GirlAction sneakStep(GirlAction action) {
        return switch (action) {
            case TOUCH_BOOBS_SLOW -> GirlAction.TOUCH_BOOBS_FAST;
            case COWGIRL_SITTING_SLOW -> GirlAction.COWGIRL_SITTING_FAST;
            default -> null;
        };
    }

    @Override
    public @Nullable GirlAction climaxStep(GirlAction action) {
        return switch (action) {
            case TOUCH_BOOBS_SLOW, TOUCH_BOOBS_FAST -> GirlAction.TOUCH_BOOBS_CUM;
            case COWGIRL_SITTING_SLOW, COWGIRL_SITTING_FAST -> GirlAction.COWGIRL_SITTING_CUM;
            default -> null;
        };
    }

    @Override
    protected boolean canStep(GirlAction from, GirlAction to) {
        if (to == sneakStep(from) || to == climaxStep(from)) return true;
        return switch (from) {
            case TOUCH_BOOBS_INTRO, TOUCH_BOOBS_FAST -> to == GirlAction.TOUCH_BOOBS_SLOW;               // touch_boobs_introDone, fastDone
            case COWGIRL_SITTING_INTRO, COWGIRL_SITTING_FAST -> to == GirlAction.COWGIRL_SITTING_SLOW;   // sitting_introDone, sitting_fastDone
            case TOUCH_BOOBS_CUM, COWGIRL_SITTING_CUM, HEAD_PAT -> to == GirlAction.NULL;               // touch_boobs_cumDone, resetGirl
            default -> false;
        };
    }

    @Override
    protected void onStep(ServerPlayer partner, GirlAction from, GirlAction to) {
        if (to == GirlAction.COWGIRL_SITTING_FAST || to == GirlAction.COWGIRL_SITTING_CUM) positionPartner(partner, SITTING_FAST_POSE);
        if (from == GirlAction.COWGIRL_SITTING_FAST && to == GirlAction.COWGIRL_SITTING_SLOW) positionPartner(partner, SITTING_POSE);
    }

    @Override
    protected int stripEndTicks() {
        return 100; // she has no strip
    }

    /**
     * Server, first tick after her payment for sex: she lets the partner go and walks to the
     * nearest bed (1.12.2 {@code goToSexBed}). With none she giggles that she ate the fish anyway.
     */
    private void tickBedWalk() {
        if (this.bedSpot == null) {
            BedFinder.Plan plan = BedFinder.plan(this.level(), this.blockPosition());
            releaseAnchor();
            setPartner(null);
            if (!plan.ok()) {
                this.level().playSound(null, this, ModSounds.GIRLS_LUNA_GIGGLE.random(this.random).value(), this.getSoundSource(), 1.0F, 1.0F);
                sayNearby(plan.failure() == BedFinder.Failure.OBSCURED ? "bedobscured" : "nobedinsight");
                endScene();
                return;
            }
            this.bedBound = true;
            this.bedSpot = plan.position();
            this.bedYaw = plan.yaw();
            this.walkTicks = 0;
            this.getNavigation().moveTo(this.bedSpot.x, this.bedSpot.y, this.bedSpot.z, 0.2);
            return;
        }
        this.walkTicks++;
        if (this.position().distanceTo(this.bedSpot) >= 0.5 && this.walkTicks <= BED_WALK_TIMEOUT) {
            if (this.walkTicks == 60 || this.walkTicks == 120) {
                this.getNavigation().stop();
                this.getNavigation().moveTo(this.bedSpot.x, this.bedSpot.y, this.bedSpot.z, 0.2);
            }
            return;
        }
        this.getNavigation().stop();
        anchor(this.bedSpot, this.bedYaw);
        this.noPhysics = true;
        this.joinTicks = 0;
        setAction(GirlAction.WAIT_CAT);
    }

    /** Server: she calls, and whoever stands close to her for long enough is taken (1.12.2 {@code handleNearbyPlayer}). */
    private void tickWait(int ticks) {
        if (ticks == CALL_TICK) {
            this.level().playSound(null, this, ModSounds.GIRLS_LUNA_GIGGLE.random(this.random).value(), this.getSoundSource(), 1.0F, 1.0F);
            sayNearby("comehere");
        }
        Player near = this.level().getNearestPlayer(this, LOOK_RANGE);
        if (!(near instanceof ServerPlayer player) || player.isSpectator() || near.distanceTo(this) > JOIN_RANGE) return;
        if (++this.joinTicks < JOIN_TICKS) return;
        setPartner(player);
        setSceneFrame(this.position(), this.getYRot() + 180.0F);
        positionPartner(player, SITTING_POSE);
        setDressed(false);
        setAction(GirlAction.COWGIRL_SITTING_INTRO);
    }

    @Override
    public void endScene() {
        super.endScene();
        this.bedBound = false;
        this.bedSpot = null;
        this.joinTicks = 0;
        setDressed(true);
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
        this.level().playSound(null, this, ModSounds.GIRLS_LUNA_OWO.random(this.random).value(), this.getSoundSource(), 1.0F, 1.0F);
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.GIRLS_LUNA_OUU.random(this.random).value();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return this.random.nextFloat() * 100.0F > 95.0F ? ModSounds.GIRLS_ALLIE_SCAWY.variant(2).value() : ModSounds.GIRLS_LUNA_OUU.variant(12).value();
    }

    /** 1.12.2: she jumps higher than the others, and much higher out of water. */
    @Override
    protected float getJumpPower(float multiplier) {
        return (this.isInWater() ? 1.0F : 0.5F) * multiplier * this.getBlockJumpFactor() + this.getJumpBoostPower();
    }

    @Override
    public void remove(RemovalReason reason) {
        if (this.hook != null) this.hook.discard();
        super.remove(reason);
    }
}
