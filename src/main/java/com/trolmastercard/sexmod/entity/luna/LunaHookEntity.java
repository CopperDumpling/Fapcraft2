package com.trolmastercard.sexmod.entity.luna;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Luna's bobber (1.12.2 {@code SexEntity}, registered as {@code luna_hook}): vanilla's fishing
 * hook, owned by her instead of a player. It flies from her rod; in water it bobs until a fish
 * bites ({@link #isBiting()}), and she reels in a few ticks into the bite. Landing on the ground or
 * on an entity spoils the cast: she gives up on that spot. Never saved.
 * <p>
 * Simulated on the server only; clients see its synced position.
 */
public class LunaHookEntity extends Entity {
    /** Entity id of the Luna who cast it. */
    private static final EntityDataAccessor<Integer> DATA_OWNER = SynchedEntityData.defineId(LunaHookEntity.class, EntityDataSerializers.INT);
    /** A bite was reeled in: no more bobbing or bites until she removes it (1.12.2 {@code lureTimer = 9999}). */
    private static final int REELED = 9999;

    private enum State { FLYING, HOOKED_IN_ENTITY, BOBBING }

    private State state = State.FLYING;
    private @Nullable Entity hookedIn;
    private boolean onLand;
    private int flightTicks;
    /** Ticks in free flight (1.12.2 {@code waterBobCounter}): from 5 on it may hook her too. */
    private int airTicks;
    /** Ticks a fish is still on the line (1.12.2 {@code lureTimer}). */
    private int biteTicks;
    private int timeUntilLured;
    private int timeUntilHooked;
    private float fishAngle;
    /** Ticks the lure enchantment takes off the wait for a fish. */
    private int lureSpeed;

    public LunaHookEntity(EntityType<? extends LunaHookEntity> type, Level level) {
        super(type, level);
    }

    /** Server: cast from {@code luna} towards her fishing spot, {@code distance} blocks away (1.12.2 {@code positionLunaAbove}). */
    public static LunaHookEntity cast(LunaEntity luna, double distance, int lureSpeed) {
        LunaHookEntity hook = new LunaHookEntity(com.trolmastercard.sexmod.registry.ModEntities.LUNA_HOOK.get(), luna.level());
        hook.entityData.set(DATA_OWNER, luna.getId());
        hook.lureSpeed = lureSpeed;
        double strength = distance * 0.01;
        // the farther the spot, the higher she aims
        float pitch = -22.5F + 45.0F * ((float) distance / 7.0F);
        float yaw = luna.getYRot();
        float cosYaw = Mth.cos(-yaw * Mth.DEG_TO_RAD - Mth.PI);
        float sinYaw = Mth.sin(-yaw * Mth.DEG_TO_RAD - Mth.PI);
        float cosPitch = -Mth.cos(-pitch * Mth.DEG_TO_RAD);
        float sinPitch = Mth.sin(-pitch * Mth.DEG_TO_RAD);
        hook.snapTo(luna.getX() - sinYaw * 0.3, luna.getEyeY(), luna.getZ() - cosYaw * 0.3, yaw, pitch);
        Vec3 motion = new Vec3(strength * -sinYaw, strength * Mth.clamp(-(sinPitch / cosPitch), -5.0F, 5.0F), strength * -cosYaw);
        double speed = motion.length();
        motion = new Vec3(motion.x * hook.spread(speed), motion.y * hook.spread(speed), motion.z * hook.spread(speed));
        hook.setDeltaMovement(motion);
        hook.turnToMotion();
        hook.yRotO = hook.getYRot();
        hook.xRotO = hook.getXRot();
        return hook;
    }

    /** 1.12.2's throw: each axis scaled by 0.6 / speed + 0.5, jittered a little. */
    private double spread(double speed) {
        return 0.6 / speed + 0.5 + this.random.nextGaussian() * 0.0045;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_OWNER, -1);
    }

    public @Nullable LunaEntity getOwner() {
        return this.level().getEntity(this.entityData.get(DATA_OWNER)) instanceof LunaEntity luna ? luna : null;
    }

    /** Server: a fish is on the line, but she hasn't reeled it in yet. */
    public boolean isBiting() {
        return this.biteTicks > 0 && this.biteTicks != REELED;
    }

    /** Server: ticks left of the bite (1.12.2 {@code lureTimer}; Luna reels at 15). */
    public int biteTicks() {
        return this.biteTicks;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) return;
        LunaEntity owner = getOwner();
        if (owner == null || !owner.isAlive()) {
            discard();
            return;
        }
        // a cast that lands on the ground or an entity is spoiled
        if ((this.hookedIn != null || this.onGround()) && this.biteTicks == 0) {
            owner.onSpoiledCast();
            return;
        }
        if (this.onLand && ++this.flightTicks >= 1200) {
            discard();
            return;
        }
        BlockPos pos = this.blockPosition();
        BlockState block = this.level().getBlockState(pos);
        float waterHeight = block.getFluidState().is(FluidTags.WATER) ? block.getFluidState().getHeight(this.level(), pos) : 0.0F;
        Vec3 motion = this.getDeltaMovement();
        switch (this.state) {
            case FLYING -> {
                if (this.hookedIn != null) {
                    this.setDeltaMovement(Vec3.ZERO);
                    this.state = State.HOOKED_IN_ENTITY;
                    return;
                }
                if (waterHeight > 0.0F) {
                    this.setDeltaMovement(motion.multiply(0.3, 0.2, 0.3));
                    this.state = State.BOBBING;
                    return;
                }
                checkHit();
                if (!this.onLand && !this.onGround() && !this.horizontalCollision) {
                    this.airTicks++;
                } else {
                    this.airTicks = 0;
                    this.setDeltaMovement(Vec3.ZERO);
                }
            }
            case HOOKED_IN_ENTITY -> {
                if (this.hookedIn != null) {
                    if (this.hookedIn.isRemoved()) {
                        this.hookedIn = null;
                        this.state = State.FLYING;
                    } else {
                        this.setPos(this.hookedIn.getX(), this.hookedIn.getY(0.8), this.hookedIn.getZ());
                    }
                }
                return;
            }
            case BOBBING -> {
                double dy = this.getY() + motion.y - pos.getY() - waterHeight;
                if (Math.abs(dy) < 0.01) dy += Math.signum(dy) * 0.1;
                this.setDeltaMovement(motion.x * 0.9, motion.y - dy * this.random.nextFloat() * 0.2, motion.z * 0.9);
                if (waterHeight > 0.0F) catchingFish(pos);
            }
        }
        if (!block.getFluidState().is(FluidTags.WATER)) this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.03, 0.0));
        this.move(MoverType.SELF, this.getDeltaMovement());
        turnToMotion();
        this.setDeltaMovement(this.getDeltaMovement().scale(0.92));
        this.needsSync = true;
    }

    /** Server, while flying: hooks the first entity ahead of it, or marks it as landed on a block. */
    private void checkHit() {
        Vec3 from = this.position();
        Vec3 to = from.add(this.getDeltaMovement());
        BlockHitResult blockHit = this.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (blockHit.getType() != HitResult.Type.MISS) to = blockHit.getLocation();
        LunaEntity owner = getOwner();
        Entity target = null;
        double closest = 0.0;
        List<Entity> nearby = this.level().getEntities(this, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0));
        for (Entity entity : nearby) {
            if (!(entity.isPickable() || entity instanceof ItemEntity)) continue;
            if (entity == owner && this.airTicks < 5) continue;
            AABB box = entity.getBoundingBox().inflate(0.3);
            var hit = box.clip(from, to);
            if (hit.isPresent()) {
                double dist = from.distanceToSqr(hit.get());
                if (target == null || dist < closest) {
                    target = entity;
                    closest = dist;
                }
            }
        }
        if (target != null) {
            this.hookedIn = target;
        } else if (blockHit.getType() != HitResult.Type.MISS) {
            this.onLand = true;
        }
    }

    /** Server, while bobbing: vanilla's wait for a fish, its approach, and the bite. */
    private void catchingFish(BlockPos pos) {
        ServerLevel level = (ServerLevel) this.level();
        int speed = 1;
        BlockPos above = pos.above();
        if (this.random.nextFloat() < 0.25F && level.isRainingAt(above)) speed++;
        if (this.random.nextFloat() < 0.5F && !level.canSeeSky(above)) speed--;
        if (this.biteTicks > 0) {
            if (this.biteTicks == REELED) return;
            this.biteTicks--;
            if (this.biteTicks <= 0) {
                this.timeUntilLured = 0;
                this.timeUntilHooked = 0;
            } else {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.2 * this.random.nextFloat() * this.random.nextFloat(), 0.0));
            }
        } else if (this.timeUntilHooked > 0) {
            this.timeUntilHooked -= speed;
            if (this.timeUntilHooked > 0) {
                this.fishAngle += (float) (this.random.nextGaussian() * 4.0);
                float angle = this.fishAngle * Mth.DEG_TO_RAD;
                float sin = Mth.sin(angle);
                float cos = Mth.cos(angle);
                double x = this.getX() + sin * this.timeUntilHooked * 0.1F;
                double y = Mth.floor(this.getY()) + 1.0;
                double z = this.getZ() + cos * this.timeUntilHooked * 0.1F;
                if (level.getBlockState(BlockPos.containing(x, y - 1.0, z)).is(Blocks.WATER)) {
                    if (this.random.nextFloat() < 0.15F) level.sendParticles(ParticleTypes.BUBBLE, x, y - 0.1, z, 1, sin, 0.1, cos, 0.0);
                    float xa = sin * 0.04F;
                    float za = cos * 0.04F;
                    level.sendParticles(ParticleTypes.FISHING, x, y, z, 0, za, 0.01, -xa, 1.0);
                    level.sendParticles(ParticleTypes.FISHING, x, y, z, 0, -za, 0.01, xa, 1.0);
                }
            } else {
                // the bite
                this.setDeltaMovement(this.getDeltaMovement().add(0.0, -0.4F * Mth.nextFloat(this.random, 0.6F, 1.0F), 0.0));
                this.playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.25F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
                double y = this.getY() + 0.5;
                int count = (int) (1.0F + this.getBbWidth() * 20.0F);
                level.sendParticles(ParticleTypes.BUBBLE, this.getX(), y, this.getZ(), count, this.getBbWidth(), 0.0, this.getBbWidth(), 0.2F);
                level.sendParticles(ParticleTypes.FISHING, this.getX(), y, this.getZ(), count, this.getBbWidth(), 0.0, this.getBbWidth(), 0.2F);
                this.biteTicks = Mth.nextInt(this.random, 20, 40);
            }
        } else if (this.timeUntilLured > 0) {
            this.timeUntilLured -= speed;
            float chance = 0.15F;
            if (this.timeUntilLured < 20) {
                chance += (20 - this.timeUntilLured) * 0.05F;
            } else if (this.timeUntilLured < 40) {
                chance += (40 - this.timeUntilLured) * 0.02F;
            } else if (this.timeUntilLured < 60) {
                chance += (60 - this.timeUntilLured) * 0.01F;
            }
            if (this.random.nextFloat() < chance) {
                float angle = Mth.nextFloat(this.random, 0.0F, 360.0F) * Mth.DEG_TO_RAD;
                float dist = Mth.nextFloat(this.random, 25.0F, 60.0F);
                double x = this.getX() + Mth.sin(angle) * dist * 0.1;
                double y = Mth.floor(this.getY()) + 1.0;
                double z = this.getZ() + Mth.cos(angle) * dist * 0.1;
                if (level.getBlockState(BlockPos.containing(x, y - 1.0, z)).is(Blocks.WATER)) {
                    level.sendParticles(ParticleTypes.SPLASH, x, y, z, 2 + this.random.nextInt(2), 0.1, 0.0, 0.1, 0.0);
                }
            }
            if (this.timeUntilLured <= 0) {
                this.fishAngle = Mth.nextFloat(this.random, 0.0F, 360.0F);
                this.timeUntilHooked = Mth.nextInt(this.random, 20, 80);
            }
        } else {
            this.timeUntilLured = Mth.nextInt(this.random, 100, 600) - this.lureSpeed;
        }
    }

    /**
     * Server: she reels in (1.12.2 {@code getCatchResult}). A hooked entity is pulled towards her;
     * a bite gives her a roll of the fishing loot table.
     */
    public void reelIn(LunaEntity luna, ItemStack rod) {
        if (this.hookedIn != null) {
            Vec3 pull = luna.position().subtract(this.position()).scale(0.1);
            this.hookedIn.setDeltaMovement(this.hookedIn.getDeltaMovement().add(pull));
            this.level().broadcastEntityEvent(this, (byte) 31);
        } else if (this.biteTicks > 0 && this.biteTicks != REELED) {
            LootParams params = new LootParams.Builder((ServerLevel) this.level())
                .withParameter(LootContextParams.ORIGIN, this.position())
                .withParameter(LootContextParams.TOOL, rod)
                .withParameter(LootContextParams.THIS_ENTITY, this)
                .create(LootContextParamSets.FISHING);
            LootTable table = ((ServerLevel) this.level()).getServer().reloadableRegistries().getLootTable(BuiltInLootTables.FISHING);
            for (ItemStack stack : table.getRandomItems(params)) {
                luna.setCatch(stack);
            }
            this.biteTicks = REELED;
        }
    }

    private void turnToMotion() {
        Vec3 motion = this.getDeltaMovement();
        double horizontal = motion.horizontalDistance();
        this.setYRot((float) (Mth.atan2(motion.x, motion.z) * Mth.RAD_TO_DEG));
        this.setXRot((float) (Mth.atan2(motion.y, horizontal) * Mth.RAD_TO_DEG));
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        return false;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096.0;
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }
}
