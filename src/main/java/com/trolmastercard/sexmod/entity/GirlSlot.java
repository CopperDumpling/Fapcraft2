package com.trolmastercard.sexmod.entity;

import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;

/**
 * A girl's inventory slots and what each accepts (1.12.2 {@code GirlInventorySlot}): a sword or
 * tool, a bow, one armour piece per body part, and Luna's fishing rod.
 */
public enum GirlSlot {
    WEAPON,
    BOW,
    HELMET,
    CHEST_PLATE,
    PANTS,
    SHOES,
    ROD;

    /** The inventory index of this slot. */
    public int index() {
        return ordinal();
    }

    public boolean accepts(ItemStack stack) {
        return switch (this) {
            // 1.12.2 ItemSword or ItemTool (pickaxe, axe, shovel; not the hoe)
            case WEAPON -> stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(ItemTags.PICKAXES) || stack.is(ItemTags.SHOVELS);
            case BOW -> stack.getItem() instanceof BowItem;
            case HELMET -> stack.is(ItemTags.HEAD_ARMOR);
            case CHEST_PLATE -> stack.is(ItemTags.CHEST_ARMOR);
            case PANTS -> stack.is(ItemTags.LEG_ARMOR);
            case SHOES -> stack.is(ItemTags.FOOT_ARMOR);
            case ROD -> stack.getItem() instanceof FishingRodItem;
        };
    }
}
