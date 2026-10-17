package com.trolmastercard.sexmod.registry;

import com.trolmastercard.sexmod.Fapcraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Container menus: girl inventories, chest container (Phase 3). */
public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Fapcraft.MODID);

    private ModMenus() {
    }
}
