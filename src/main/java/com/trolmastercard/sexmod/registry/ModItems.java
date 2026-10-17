package com.trolmastercard.sexmod.registry;

import com.trolmastercard.sexmod.Fapcraft;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.SpawnEggItem;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Items: spawn eggs (Phase 2+), lamp, staff, coin, eggs, rod, wand, Winchester (phases 4–8). */
public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Fapcraft.MODID);

    public static final DeferredItem<SpawnEggItem> JENNY_SPAWN_EGG = ITEMS.registerItem(
        "jenny_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.JENNY.get()));
    public static final DeferredItem<SpawnEggItem> ELLIE_SPAWN_EGG = ITEMS.registerItem(
        "ellie_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.ELLIE.get()));
    public static final DeferredItem<SpawnEggItem> BIA_SPAWN_EGG = ITEMS.registerItem(
        "bia_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.BIA.get()));
    public static final DeferredItem<SpawnEggItem> SLIME_SPAWN_EGG = ITEMS.registerItem(
        "slime_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.SLIME.get()));
    public static final DeferredItem<SpawnEggItem> MANGLELIE_SPAWN_EGG = ITEMS.registerItem(
        "manglelie_spawn_egg", SpawnEggItem::new, p -> p.spawnEgg(ModEntities.MANGLELIE.get()));

    private ModItems() {
    }

    @EventBusSubscriber(modid = Fapcraft.MODID)
    static final class Tabs {
        @SubscribeEvent
        static void onBuildTabs(BuildCreativeModeTabContentsEvent event) {
            if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
                event.accept(JENNY_SPAWN_EGG);
                event.accept(ELLIE_SPAWN_EGG);
                event.accept(BIA_SPAWN_EGG);
                event.accept(SLIME_SPAWN_EGG);
                event.accept(MANGLELIE_SPAWN_EGG);
            }
        }
    }
}
