package com.trolmastercard.sexmod.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server options (1.12.2 kept these in {@code config/sexmod.json}). A copy in a world's
 * {@code syncedconfig/} folder overrides the global one; synced to clients on join.
 */
public final class ServerConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue GENERATE_BUILDINGS;
    public static final ModConfigSpec.BooleanValue ALLOW_FLYING;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        GENERATE_BUILDINGS = b
            .comment("Generate the girls' houses in new chunks (1.12.2: shouldGenBuildings)")
            .define("generateBuildings", true);
        ALLOW_FLYING = b
            .comment("Let players transformed into a winged girl fly (1.12.2: allowFlying)")
            .define("allowFlying", true);
        SPEC = b.build();
    }

    private ServerConfig() {
    }
}
