package com.trolmastercard.sexmod.entity.slime;

import com.trolmastercard.sexmod.registry.ModEntities;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The wild slime a pregnant {@link SlimeEntity} gives birth to (1.12.2 {@code WildSlimeEntity},
 * registered as {@code friendly_slime}): a tiny slime with a tail that hops around like a vanilla
 * slime but never attacks. After 8400 ticks it grows into a slime girl; it sparkles from 5880 and
 * puffs clouds from 7980. It drops what a tiny vanilla slime does
 * ({@code tools/loot_extra/friendly_slime.json}).
 */
public class WildSlimeEntity extends AbstractCubeMob {
    private static final int GROWN_TICKS = 8400;
    private static final int SPARKLE_TICKS = 5880;
    private static final int CLOUD_TICKS = 7980;
    /** How far it has grown, for the client's particles: 0 none, 1 sparkles, 2 clouds. */
    private static final EntityDataAccessor<Integer> DATA_STAGE = SynchedEntityData.defineId(WildSlimeEntity.class, EntityDataSerializers.INT);

    private int age;

    public WildSlimeEntity(EntityType<? extends WildSlimeEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_STAGE, 0);
    }

    @Override
    protected void addBehaviourGoals() {
        // no attacks: it only hops about
    }

    @Override
    protected void addTargetingGoals() {
    }

    @Override
    protected boolean canDealDamage() {
        return false;
    }

    @Override
    protected boolean canBeABaby() {
        return false;
    }

    /** Always born tiny (1.12.2 {@code onInitialSpawn}: size 1). */
    @Override
    protected void setSpawnSize(ServerLevelAccessor level, DifficultyInstance difficulty) {
        setSize(1, true);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            int stage = this.entityData.get(DATA_STAGE);
            if (stage == 2) {
                growthParticle(ParticleTypes.CLOUD);
            } else if (stage == 1 && this.tickCount % 10 == 0) {
                growthParticle(ParticleTypes.HAPPY_VILLAGER);
            }
            return;
        }
        this.age++;
        int stage = this.age > CLOUD_TICKS ? 2 : this.age > SPARKLE_TICKS ? 1 : 0;
        if (stage != this.entityData.get(DATA_STAGE)) this.entityData.set(DATA_STAGE, stage);
        if (this.age > GROWN_TICKS) growUp();
    }

    /** Server: it becomes a slime girl where it stands. */
    private void growUp() {
        SlimeEntity girl = ModEntities.SLIME.get().create(this.level(), EntitySpawnReason.CONVERSION);
        if (girl != null) {
            girl.snapTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
            this.level().addFreshEntity(girl);
            girl.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP);
        }
        discard();
    }

    private void growthParticle(ParticleOptions type) {
        double x = this.getX() + this.random.nextFloat() * this.getBbWidth() * 2.0F - this.getBbWidth();
        double y = this.getY() + 0.15 + this.random.nextFloat() * this.getBbHeight();
        double z = this.getZ() + this.random.nextFloat() * this.getBbWidth() * 2.0F - this.getBbWidth();
        this.level().addParticle(type, x, y, z, this.random.nextGaussian() * 0.02, this.random.nextGaussian() * 0.02, this.random.nextGaussian() * 0.02);
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    @Override
    protected @Nullable ParticleOptions getParticleType() {
        return ParticleTypes.ITEM_SLIME;
    }

    @Override
    protected SoundEvent getJumpSound() {
        return isTiny() ? SoundEvents.SLIME_JUMP_SMALL : SoundEvents.SLIME_JUMP;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return isTiny() ? SoundEvents.SLIME_HURT_SMALL : SoundEvents.SLIME_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return isTiny() ? SoundEvents.SLIME_DEATH_SMALL : SoundEvents.SLIME_DEATH;
    }

    @Override
    protected SoundEvent getSquishSound() {
        return isTiny() ? SoundEvents.SLIME_SQUISH_SMALL : SoundEvents.SLIME_SQUISH;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("ageInTicks", this.age);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.age = input.getIntOr("ageInTicks", 0);
    }
}
