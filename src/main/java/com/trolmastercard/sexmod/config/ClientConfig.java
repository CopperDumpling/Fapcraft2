package com.trolmastercard.sexmod.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Client-only options (1.12.2 kept these in {@code config/sexmod.json}). */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue LOAD_OTHER_SKINS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        LOAD_OTHER_SKINS = b
            .comment("Load other players' custom girl models and skins (1.12.2: shouldLoadOtherSkins)")
            .define("loadOtherSkins", false);
        SPEC = b.build();
    }

    private ClientConfig() {
    }
}
