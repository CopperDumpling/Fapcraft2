package com.trolmastercard.sexmod.client.render;

import com.geckolib.loading.math.MathParser;
import com.geckolib.loading.math.value.Variable;

/**
 * Custom Molang variables the 1.12.2 animations use. GeckoLib 3 created unknown variables on
 * the fly (as 0); GeckoLib 5 refuses to bake an animation that mentions one it doesn't know.
 * Must run before the first resource load.
 * <ul>
 *   <li>{@code size}: Kobold body size (1.12.2 {@code KoboldEntity} set it each frame).</li>
 *   <li>{@code pitch}, {@code yaw}, {@code armpitch}, {@code armyaw}: Galath's aim
 *       (1.12.2 {@code GalathNpcModel}).</li>
 * </ul>
 * They read 0 until those girls are ported and drive them per entity.
 */
public final class MolangVariables {
    private MolangVariables() {
    }

    public static void register() {
        for (String name : new String[] {"size", "pitch", "yaw", "armpitch", "armyaw"}) {
            MathParser.registerVariable(new Variable(name, 0.0));
        }
    }
}
