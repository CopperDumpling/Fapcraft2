package com.trolmastercard.sexmod.registry;

import com.trolmastercard.sexmod.network.ModPayloads;
import com.trolmastercard.sexmod.sound.ModSounds;
import net.neoforged.bus.api.IEventBus;

/** Attaches every deferred register (and the payload registration listener) to the mod bus. */
public final class ModRegistries {
    private ModRegistries() {
    }

    public static void register(IEventBus modBus) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModEffects.EFFECTS.register(modBus);
        ModEffects.POTIONS.register(modBus);
        ModAttachments.ATTACHMENTS.register(modBus);
        ModMenus.MENUS.register(modBus);
        ModComponents.COMPONENTS.register(modBus);
        modBus.addListener(ModPayloads::register);
    }
}
