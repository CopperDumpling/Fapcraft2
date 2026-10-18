package com.trolmastercard.sexmod.entity.luna;

import com.trolmastercard.sexmod.Fapcraft;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/** Creepers keep 6 blocks from Luna, as they do from cats (1.12.2 {@code LunaEntity.a}). */
@EventBusSubscriber(modid = Fapcraft.MODID)
public final class CreepersAvoidLuna {
    private CreepersAvoidLuna() {
    }

    @SubscribeEvent
    static void onJoin(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && event.getEntity() instanceof Creeper creeper) {
            creeper.goalSelector.addGoal(3, new AvoidEntityGoal<>(creeper, LunaEntity.class, 6.0F, 1.0, 1.2));
        }
    }
}
