package com.trolmastercard.sexmod.entity;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * One entry of a girl's interaction menu (1.12.2 {@code openInventoryGui} options + rewards).
 *
 * @param id             sent by the client to pick this option
 * @param translationKey menu label (1.12.2 keys, e.g. {@code action.names.blowjob})
 * @param priceItem      item taken from the player before the scene, or null for free. Kept as
 *                       an item, not a stack: options are built during class init, before item
 *                       components exist
 * @param priceCount     how many of {@code priceItem}
 * @param firstStep      the step that follows the payment animation
 * @param shownWhen      which outfit state lists the option
 * @param needsNude      she strips first if dressed (1.12.2: boobjob and doggy; the blowjob works dressed)
 * @param solo           free, no payment animation and no scene lock: she just does it (dress up)
 * @param needsBed       she walks to the nearest bed after payment and the partner joins her there (doggy)
 * @param requestLine    the partner's chat line at the payment (key suffix after the dialogue prefix)
 */
public record SceneOption(String id, String translationKey, @Nullable Item priceItem, int priceCount, GirlAction firstStep,
                          Outfit shownWhen, boolean needsNude, boolean solo, boolean needsBed, String requestLine) {
    public enum Outfit {
        ANY, DRESSED, NUDE;

        public boolean matches(boolean dressed) {
            return this == ANY || (this == DRESSED) == dressed;
        }
    }

    public ItemStack price() {
        return this.priceItem == null ? ItemStack.EMPTY : new ItemStack(this.priceItem, this.priceCount);
    }
}
