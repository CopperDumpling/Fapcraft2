package com.trolmastercard.sexmod.menu;

import com.trolmastercard.sexmod.entity.GirlEntity;
import com.trolmastercard.sexmod.entity.GirlSlot;
import com.trolmastercard.sexmod.registry.ModMenus;
import java.util.List;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A girl's equipment: weapon, bow and four armour slots above the player's inventory (1.12.2
 * {@code ChestContainer}). The server's menu edits her inventory directly; 1.12.2 instead let the
 * client upload both inventories when the screen closed ({@code UploadInventoryToServerPacket}).
 */
public class GirlEquipmentMenu extends AbstractContainerMenu {
    /** The slots shown, in order, and their x positions (1.12.2 layout; all at y 60). */
    public static final List<GirlSlot> SLOTS = List.of(GirlSlot.WEAPON, GirlSlot.BOW, GirlSlot.HELMET, GirlSlot.CHEST_PLATE, GirlSlot.PANTS, GirlSlot.SHOES);
    private static final int[] SLOT_X = {31, 50, 72, 91, 110, 129};
    private static final int SLOT_Y = 60;
    /** Range in which the menu stays open (vanilla's entity menus use the same). */
    private static final double MAX_DISTANCE_SQR = 8.0 * 8.0;

    private final @Nullable GirlEntity girl;

    /** Client: opened by the server, which sends the girl's entity id. */
    public static GirlEquipmentMenu fromNetwork(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf data) {
        GirlEntity girl = playerInventory.player.level().getEntity(data.readVarInt()) instanceof GirlEntity found ? found : null;
        return new GirlEquipmentMenu(containerId, playerInventory, girl);
    }

    public GirlEquipmentMenu(int containerId, Inventory playerInventory, @Nullable GirlEntity girl) {
        super(ModMenus.GIRL_EQUIPMENT.get(), containerId);
        this.girl = girl;
        Container inventory = girl != null ? girl.getInventory() : new SimpleContainer(GirlEntity.INVENTORY_SIZE);
        for (int i = 0; i < SLOTS.size(); i++) {
            GirlSlot type = SLOTS.get(i);
            addSlot(new Slot(inventory, type.index(), SLOT_X[i], SLOT_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return type.accepts(stack);
                }
            });
        }
        addStandardInventorySlots(playerInventory, 8, 84);
    }

    @Override
    public boolean stillValid(Player player) {
        return this.girl != null && this.girl.isAlive() && !this.girl.isBusy() && player.distanceToSqr(this.girl) <= MAX_DISTANCE_SQR;
    }

    /** Shift-click: her slots to the player (last slot first), the player's items into the first slot that takes them. */
    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        int girlSlots = SLOTS.size();
        if (slotIndex < girlSlots) {
            if (!moveItemStackTo(stack, girlSlots, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, girlSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }
}
