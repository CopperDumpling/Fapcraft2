package com.trolmastercard.sexmod.registry;

import com.trolmastercard.sexmod.Fapcraft;
import com.trolmastercard.sexmod.menu.GirlEquipmentMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Container menus: girl equipment (Phase 3); Luna's and the 27-slot girl chests come with their girls. */
public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Fapcraft.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<GirlEquipmentMenu>> GIRL_EQUIPMENT =
        MENUS.register("girl_equipment", () -> IMenuTypeExtension.create(GirlEquipmentMenu::fromNetwork));

    private ModMenus() {
    }
}
