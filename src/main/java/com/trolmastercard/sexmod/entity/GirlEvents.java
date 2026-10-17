package com.trolmastercard.sexmod.entity;

import com.trolmastercard.sexmod.Fapcraft;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Game events about girls that their own entity code can't see. */
@EventBusSubscriber(modid = Fapcraft.MODID)
public final class GirlEvents {
    private GirlEvents() {
    }

    /** A girl's pearl doesn't teleport her wherever it lands (vanilla moves any owner): she decides. */
    @SubscribeEvent
    static void onProjectileImpact(ProjectileImpactEvent event) {
        if (event.getProjectile() instanceof ThrownEnderpearl pearl && pearl.getOwner() instanceof GirlEntity girl && !pearl.level().isClientSide()) {
            event.setCanceled(true);
            pearl.discard();
            girl.onPearlLanded(pearl);
        }
    }

    /** A scene partner standing at their girl can't be hurt, except by the void and /kill (1.12.2 {@code GirlCombatProtection}). */
    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player) || event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        for (GirlEntity girl : player.level().getEntitiesOfClass(GirlEntity.class, player.getBoundingBox().inflate(2.0))) {
            if (girl.isPartner(player) && girl.distanceTo(player) < 1.0F) {
                event.setCanceled(true);
                return;
            }
        }
    }
}
