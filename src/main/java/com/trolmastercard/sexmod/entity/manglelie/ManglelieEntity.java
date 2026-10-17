package com.trolmastercard.sexmod.entity.manglelie;

import com.geckolib.animation.RawAnimation;
import com.trolmastercard.sexmod.entity.GirlAction;
import com.trolmastercard.sexmod.entity.GirlEntity;
import com.trolmastercard.sexmod.entity.SceneOption;
import com.trolmastercard.sexmod.world.NetherSpawns;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

/**
 * Manglelie, "Mang": the little imp girl of the Nether (1.12.2 {@code ManglelieEntity}).
 * <p>
 * Wild, she wanders and runs from players within 20 blocks (a scared run). Everything else she
 * does belongs to a Galath, who adopts her, carries her on her head and corrupts mobs with her;
 * that comes with Galath (Phase 5). She shrugs off potion effects.
 */
public class ManglelieEntity extends GirlEntity {
    /** She is running from a player (1.12.2 data key 115, set by {@code AvoidPlayerGoal}). */
    private static final EntityDataAccessor<Boolean> DATA_SCARED = SynchedEntityData.defineId(ManglelieEntity.class, EntityDataSerializers.BOOLEAN);
    private static final float FLEE_DISTANCE = 20.0F;

    public ManglelieEntity(EntityType<? extends ManglelieEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createGirlAttributes();
    }

    /**
     * Natural spawns (1.12.2 {@code getCanSpawnHere}): the usual mob checks, and at least 700 blocks
     * from any earlier Galath or Manglelie spawn. A spawn that passes is remembered.
     */
    public static boolean checkSpawnRules(EntityType<? extends Mob> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (!Mob.checkMobSpawnRules(type, level, reason, pos, random)) return false;
        if (reason != EntitySpawnReason.NATURAL && reason != EntitySpawnReason.CHUNK_GENERATION) return true;
        NetherSpawns spawns = NetherSpawns.get(level.getLevel().getServer());
        if (!spawns.isFree(pos)) return false;
        spawns.addManglelie(pos);
        return true;
    }

    @Override
    protected boolean isCompanion() {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SCARED, false);
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new FleePlayerGoal(this));
    }

    /** Runs from players (1.12.2 {@code AvoidPlayerGoal}); the Galath exceptions come with Galath. */
    private static final class FleePlayerGoal extends AvoidEntityGoal<Player> {
        private final ManglelieEntity mang;

        FleePlayerGoal(ManglelieEntity mang) {
            super(mang, Player.class, FLEE_DISTANCE, 1.0, 1.2);
            this.mang = mang;
        }

        @Override
        public void start() {
            this.mang.entityData.set(DATA_SCARED, true);
            super.start();
        }

        @Override
        public void stop() {
            this.mang.entityData.set(DATA_SCARED, false);
            super.stop();
        }
    }

    /** 1.12.2 ignored every potion effect on her. */
    @Override
    public boolean canBeAffected(MobEffectInstance effect) {
        return false;
    }

    @Override
    protected String animationPrefix() {
        return "animation.manglelie.";
    }

    @Override
    protected String blinkAnimationName() {
        return "angry_face";
    }

    @Override
    protected boolean blinks(GirlAction action) {
        return false; // her face only animates while she corrupts a mob
    }

    @Override
    protected String movementAnimation(boolean moving) {
        if (!moving) return "idle";
        return this.entityData.get(DATA_SCARED) ? "scared_run" : "walk";
    }

    @Override
    protected @Nullable RawAnimation actionAnimation(GirlAction action) {
        return null; // her scenes come with Galath
    }

    /** Her {@code null} animation swings her legs, so 1.12.2 stopped idle controllers instead. */
    @Override
    protected boolean hasEmptyNullAnimation() {
        return false;
    }

    @Override
    public String dialoguePrefix() {
        return "manglelie.dialogue.";
    }

    @Override
    public List<SceneOption> sceneOptions() {
        return List.of();
    }

    @Override
    public @Nullable GirlAction sneakStep(GirlAction action) {
        return null;
    }

    @Override
    public @Nullable GirlAction climaxStep(GirlAction action) {
        return null;
    }

    @Override
    protected boolean canStep(GirlAction from, GirlAction to) {
        return false;
    }

    @Override
    protected int stripEndTicks() {
        return 100;
    }
}
