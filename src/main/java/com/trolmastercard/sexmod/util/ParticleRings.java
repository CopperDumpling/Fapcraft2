package com.trolmastercard.sexmod.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** 1.12.2 {@code WorldUtils.spawnParticles}: a flat ring of particles drifting up at random speeds. */
public final class ParticleRings {
    private ParticleRings() {
    }

    public static void ring(ServerLevel level, ParticleOptions particle, Vec3 center, int count, double radius, double speed) {
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0 * i / count;
            // count 0: the offset is the particle's velocity
            level.sendParticles(particle, center.x + Math.sin(angle) * radius, center.y, center.z + Math.cos(angle) * radius,
                0, 0.0, level.getRandom().nextFloat() * speed, 0.0, 1.0);
        }
    }
}
