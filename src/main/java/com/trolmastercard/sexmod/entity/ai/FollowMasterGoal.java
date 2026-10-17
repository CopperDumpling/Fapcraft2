package com.trolmastercard.sexmod.entity.ai;

import com.trolmastercard.sexmod.entity.GirlAction;
import com.trolmastercard.sexmod.entity.GirlEntity;
import com.trolmastercard.sexmod.util.VectorMath;
import java.util.Comparator;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.equine.AbstractHorse;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A girl's companion behaviour while she has a master (1.12.2 {@code GirlFollowGoal} +
 * {@code GirlFollowAiBase}): she follows them, rides along, fights whatever attacks either of
 * them (sword up close, bow from range), wanders near them when idle, and stays put while
 * downed.
 * <p>
 * Each tick picks a {@link State} from the world, then acts on it. The goal claims no flags, as
 * in 1.12.2 (mutex 0): her other goals are gated on having no master instead.
 */
public class FollowMasterGoal extends Goal {
    private enum State { ATTACK, FOLLOW, IDLE, RIDE, DOWNED, BUSY }

    /** Speed multipliers (1.12.2 {@code followDistance}, {@code attackDistance}). */
    private static final double WALK_SPEED = 0.5;
    private static final double RUN_SPEED = 0.7;

    private final GirlEntity girl;
    private final PathNavigation navigation;
    private @Nullable Player master;
    private @Nullable LivingEntity target;
    private @Nullable Entity mount;
    private State state = State.IDLE;
    private int wanderTimer;
    private int attackCooldown;
    private int attackTimer;
    private int bowTimer;
    private int followTimer;

    public FollowMasterGoal(GirlEntity girl) {
        this.girl = girl;
        this.navigation = girl.getNavigation();
    }

    @Override
    public boolean canUse() {
        return this.girl.getMaster() != null;
    }

    @Override
    public boolean canContinueToUse() {
        return this.girl.getMaster() != null;
    }

    @Override
    public void start() {
        this.master = this.girl.getMaster();
    }

    @Override
    public void stop() {
        // the master left (logged out, died, changed dimension) or sent her off
        this.navigation.stop();
        if (this.state == State.RIDE || this.girl.isPassenger()) dismount();
        this.state = State.IDLE;
        this.girl.clearMaster();
        this.girl.setAttackMode(0);
        this.master = null;
        this.target = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.master = this.girl.getMaster();
        if (this.master == null) return;
        this.state = pickState(this.master);
        act(this.master);
    }

    private State pickState(Player master) {
        this.attackCooldown--;
        if (this.girl.isDowned()) return State.DOWNED;
        if (this.girl.isBusy()) return State.BUSY;
        if (master.isPassenger()) {
            Entity vehicle = master.getVehicle();
            if (vehicle != null && (this.girl.isPassenger() || this.girl.startRiding(vehicle) || vehicle instanceof AbstractHorse horse && horse.isSaddled())) {
                this.mount = vehicle;
                return State.RIDE;
            }
        } else if (this.girl.isPassenger() || this.state == State.RIDE) {
            dismount();
        }

        if (isValidTarget(this.target, master)) return State.ATTACK;
        if (attacker(this.girl.getLastDamageSource()) instanceof LivingEntity attacker && isValidTarget(attacker, master)) {
            return attack(attacker);
        }
        LivingEntity attacked = master.getLastHurtMob();
        if (master.tickCount - master.getLastHurtMobTimestamp() < 140 && isValidTarget(attacked, master)) return attack(attacked);
        if (this.state != State.FOLLOW) {
            if (attacker(master.getLastDamageSource()) instanceof LivingEntity attacker && isValidTarget(attacker, master)) {
                return attack(attacker);
            }
            Vec3 pos = this.girl.position();
            List<Monster> monsters = this.girl.level().getEntitiesOfClass(Monster.class,
                new AABB(pos.x - 5.0, pos.y - 2.0, pos.z - 5.0, pos.x + 5.0, pos.y + 2.0, pos.z + 5.0));
            monsters.sort(Comparator.comparingDouble(this.girl::distanceToSqr));
            for (Monster monster : monsters) {
                if (!(monster instanceof Creeper) && isValidTarget(monster, master)) return attack(monster);
            }
        }

        // once she has caught up she keeps following for up to 3 more seconds
        boolean tooFar = this.girl.distanceTo(master) > 5.0F;
        if (!tooFar && this.state == State.FOLLOW) {
            if (++this.followTimer > 60) {
                this.followTimer = 0;
            } else {
                tooFar = true;
            }
        }
        // left the fight to catch up: no new fights for 3 seconds
        if (tooFar && this.state == State.ATTACK) this.attackCooldown = 60;
        return tooFar ? State.FOLLOW : State.IDLE;
    }

    private State attack(LivingEntity newTarget) {
        this.target = newTarget;
        return State.ATTACK;
    }

    private static @Nullable Entity attacker(@Nullable DamageSource source) {
        return source == null ? null : source.getEntity();
    }

    private boolean isValidTarget(@Nullable LivingEntity candidate, Player master) {
        return candidate != null
            && !(candidate instanceof GirlEntity)
            && this.attackCooldown <= 0
            && candidate.isAlive()
            && candidate.level() == this.girl.level()
            && candidate != master
            && this.girl.distanceTo(master) < 15.0F
            && this.girl.distanceTo(candidate) < 20.0F;
    }

    private void act(Player master) {
        switch (this.state) {
            case ATTACK -> fight(master);
            case FOLLOW -> {
                this.girl.setAttackMode(0);
                if (this.girl.getAttributeValue(Attributes.FOLLOW_RANGE) > this.girl.distanceTo(master)) {
                    this.navigation.stop();
                    this.navigation.moveTo(master, WALK_SPEED);
                    hopObstacle();
                } else {
                    teleportNear(master);
                }
                this.wanderTimer = 300;
                matchPace(master);
            }
            case IDLE -> {
                this.girl.setAttackMode(0);
                if (++this.wanderTimer > 200 + this.girl.getRandom().nextInt(100)) {
                    this.wanderTimer = 0;
                    Vec3 pos = master.position();
                    this.navigation.stop();
                    this.navigation.moveTo(pos.x + 1.0 + this.girl.getRandom().nextFloat() * 3.0F, pos.y,
                        pos.z + 1.0 + this.girl.getRandom().nextFloat() * 3.0F, WALK_SPEED);
                }
                matchPace(master);
            }
            case RIDE -> ride(master);
            case DOWNED -> this.navigation.stop();
            case BUSY -> {
                // a scene, or walking to its bed: the scene moves her
            }
        }
    }

    private void fight(Player master) {
        LivingEntity enemy = this.target;
        if (enemy == null) return;
        this.girl.getLookControl().setLookAt(enemy, 30.0F, 30.0F);
        double distance = this.girl.distanceTo(enemy);
        this.navigation.stop();
        if (distance < 1.9 && --this.attackTimer <= 0) {
            swing(master, enemy);
            return;
        }
        // 1.12.2 counted the bow timer up twice per tick while drawing (once in the check)
        if (this.girl.getBow().getItem() instanceof BowItem && this.girl.getSensing().hasLineOfSight(enemy) && ++this.bowTimer > 0 && distance > 6.0) {
            this.girl.setAttackMode(2);
            if (++this.bowTimer >= 32) {
                this.bowTimer = -20;
                shoot(enemy);
            }
            return;
        }
        this.girl.setAttackMode(1);
        if (distance < 2.0) {
            this.navigation.moveTo(enemy, WALK_SPEED);
            this.girl.setGait(GirlEntity.Gait.WALK);
        } else {
            this.navigation.moveTo(enemy, RUN_SPEED);
            this.girl.setGait(GirlEntity.Gait.RUN);
        }
    }

    /** One sword hit: the weapon's damage and enchantments, sweeping included (1.12.2 {@code startAttack}). */
    private void swing(Player master, LivingEntity enemy) {
        if (!(this.girl.level() instanceof ServerLevel level)) return;
        this.girl.playAttack();
        this.girl.setAttackMode(1);
        ItemStack weapon = this.girl.getWeapon();
        ItemAttributeModifiers modifiers = weapon.getAttributeModifiers();
        float damage = (float) modifiers.compute(Attributes.ATTACK_DAMAGE, 0.0, EquipmentSlot.MAINHAND);
        // 1.12.2 clamped the (negative) attack speed up to 0.5, so every weapon swings every 3 ticks
        float speed = Math.max((float) modifiers.compute(Attributes.ATTACK_SPEED, 0.0, EquipmentSlot.MAINHAND), 0.5F);
        DamageSource source = this.girl.damageSources().mobAttack(this.girl);
        float total = EnchantmentHelper.modifyDamage(level, weapon, enemy, source, damage);
        float knockback = EnchantmentHelper.modifyKnockback(level, weapon, enemy, source, 0.0F);
        double yaw = this.girl.getYRot() * Mth.DEG_TO_RAD;
        double pushX = Mth.sin(yaw);
        double pushZ = -Mth.cos(yaw);
        if (knockback > 0.0F) enemy.knockback(knockback * 0.5, pushX, pushZ, source, total);

        double[] sweepRatio = {0.0};
        EnchantmentHelper.forEachModifier(weapon, EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.is(Attributes.SWEEPING_DAMAGE_RATIO)) sweepRatio[0] += modifier.amount();
        });
        if (sweepRatio[0] > 0.0) {
            for (LivingEntity nearby : level.getEntitiesOfClass(LivingEntity.class, enemy.getBoundingBox().inflate(1.0, 0.25, 1.0))) {
                if (nearby != this.girl && nearby != master && nearby != enemy && !this.girl.isAlliedTo(nearby) && this.girl.distanceToSqr(nearby) < 9.0) {
                    nearby.knockback(0.4, pushX, pushZ, source, total);
                    nearby.hurtServer(level, source, (float) (total * sweepRatio[0]));
                }
            }
        }
        enemy.hurtServer(level, source, total);
        EnchantmentHelper.doPostAttackEffectsWithItemSource(level, enemy, source, weapon); // fire aspect
        this.attackTimer = Math.round(Math.abs(speed) / 3.373494F * 20.0F);
    }

    /** One arrow from her bow; the bow's enchantments apply (1.12.2 {@code shootArrow}). */
    private void shoot(LivingEntity enemy) {
        ItemStack bow = this.girl.getBow();
        AbstractArrow arrow = ProjectileUtil.getMobArrow(this.girl, new ItemStack(Items.ARROW), 1.0F, bow);
        arrow.setBaseDamage(4.5);
        double dx = enemy.getX() - this.girl.getX();
        double dy = enemy.getY(1.0 / 3.0) - arrow.getY();
        double dz = enemy.getZ() - this.girl.getZ();
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        arrow.shoot(dx, dy + horizontal * 0.2, dz, 1.6F, 2.0F);
        this.girl.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.girl.getRandom().nextFloat() * 0.4F + 0.8F));
        this.girl.level().addFreshEntity(arrow);
    }

    /** Riding along: as a passenger she sits; behind the master on a one-seat horse she is held in place. */
    private void ride(Player master) {
        if (this.girl.isPassenger()) {
            this.girl.setAction(GirlAction.SIT);
            return;
        }
        Entity vehicle = this.mount;
        if (vehicle == null) return;
        this.girl.setNoGravity(true);
        this.girl.noPhysics = true;
        Vec3 look = vehicle.getLookAngle();
        Vec3 pos = master.position().subtract(look.x * 0.5, 0.0, look.z * 0.5);
        this.girl.setPos(pos);
        this.girl.setYRot(vehicle.getYRot());
        this.girl.setYBodyRot(vehicle.getYRot());
        this.girl.setDeltaMovement(Vec3.ZERO);
        this.girl.setAction(GirlAction.RIDE);
    }

    private void dismount() {
        if (this.girl.getAction() == GirlAction.RIDE || this.girl.getAction() == GirlAction.SIT) this.girl.setAction(GirlAction.NULL);
        this.girl.stopRiding();
        this.girl.noPhysics = false;
        this.girl.setNoGravity(false);
        this.mount = null;
    }

    /** Out of path range: up to 20 tries at a spot near the master, else onto the master (1.12.2 {@code updateNavigation}). */
    private void teleportNear(Player master) {
        boolean placed = false;
        for (int attempt = 0; attempt < 20 && !placed; attempt++) {
            BlockPos pos = master.blockPosition().offset(this.girl.getRandom().nextInt(10), 0, this.girl.getRandom().nextInt(10));
            placed = this.girl.randomTeleport(pos.getX(), pos.getY(), pos.getZ(), false, state -> false);
        }
        if (!placed) this.girl.setPos(master.position());
        this.girl.setDeltaMovement(Vec3.ZERO);
    }

    /** Walks when the master walks, runs when they sprint, and hurries the further behind she is. */
    private void matchPace(Player master) {
        double speed;
        GirlEntity.Gait gait;
        if (master.isSprinting()) {
            speed = RUN_SPEED;
            gait = GirlEntity.Gait.RUN;
        } else {
            speed = WALK_SPEED;
            gait = GirlEntity.Gait.WALK;
        }
        speed += Math.floor(this.girl.distanceTo(master) / 5.0F) * 0.2;
        if (this.girl.isInWater()) {
            speed *= 60.0;
            gait = GirlEntity.Gait.WALK;
        }
        this.navigation.setSpeedModifier(speed);
        this.girl.setGait(gait);
    }

    /** A small push forward when she jumps without moving, so she clears the step (1.12.2 {@code handleKnockback}). */
    private void hopObstacle() {
        Vec3 motion = this.girl.getDeltaMovement();
        if (!this.girl.onGround() && !this.girl.isInWater() && motion.x + motion.z == 0.0 && motion.y > 0.0) {
            Vec3 push = VectorMath.rotateByYaw(new Vec3(0.0, 0.0, 0.1), this.girl.getYRot());
            this.girl.setDeltaMovement(push.x, motion.y, push.z);
        }
    }
}
