package com.trolmastercard.sexmod;

import com.mojang.logging.LogUtils;
import com.trolmastercard.sexmod.config.ServerConfig;
import com.trolmastercard.sexmod.registry.ModRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Mod entry point. The mod id stays {@code sexmod} (as in 1.12.2) so existing
 * reskin packs, which target {@code assets/sexmod/...}, keep resolving.
 */
@Mod(Fapcraft.MODID)
public class Fapcraft {
    public static final String MODID = "sexmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }

    public Fapcraft(IEventBus modEventBus, ModContainer modContainer) {
        ModRegistries.register(modEventBus);
        modContainer.registerConfig(ModConfig.Type.SYNCED, ServerConfig.SPEC);
    }
}
