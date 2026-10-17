package com.trolmastercard.sexmod.entity.ai;

import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * Runs {@code goal} only while {@code enabled} holds. Replaces 1.12.2's habit of removing and
 * re-adding wander/look tasks around scenes ({@code reinitTasks}).
 */
public final class GatedGoal extends Goal {
    private final Goal goal;
    private final BooleanSupplier enabled;

    public GatedGoal(Goal goal, BooleanSupplier enabled) {
        this.goal = goal;
        this.enabled = enabled;
        this.setFlags(goal.getFlags());
    }

    @Override
    public boolean canUse() {
        return enabled.getAsBoolean() && goal.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return enabled.getAsBoolean() && goal.canContinueToUse();
    }

    @Override
    public boolean isInterruptable() {
        return goal.isInterruptable();
    }

    @Override
    public void start() {
        goal.start();
    }

    @Override
    public void stop() {
        goal.stop();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return goal.requiresUpdateEveryTick();
    }

    @Override
    public void tick() {
        goal.tick();
    }
}
