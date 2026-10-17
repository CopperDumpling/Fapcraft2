package com.trolmastercard.sexmod.client;

import com.trolmastercard.sexmod.Fapcraft;
import com.trolmastercard.sexmod.client.render.MolangVariables;
import com.trolmastercard.sexmod.config.ClientConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/** Client-only entry point: renderers, screens, keybinds. Never loaded on a dedicated server. */
@Mod(value = Fapcraft.MODID, dist = Dist.CLIENT)
public class FapcraftClient {
    public FapcraftClient(ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        MolangVariables.register();
        // Mods screen > Fapcraft > Config
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }
}
