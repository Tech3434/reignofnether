package com.solegendary.reignofnether.items;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * The six slots a unit carries world items in.
 *
 * <p>This used to be the unit item layer: a {@code UnitItem} was a modded item that carried its own
 * abilities, each stack in a slot was a {@code UnitItem}, and the slots had methods for using them on
 * the ground, on buildings and on other entities. That layer is gone, so what is left is the plain
 * container - carry, drop, swap, hand over - which is what a new faction's units need to pick things
 * up off the ground and carry them home.
 *
 * <p>Slots are identified by UUID because a stack can move between units; the index is only valid for
 * the inventory it was read from.
 */
public interface UnitInventory {
    int MAX_INVENTORY_SIZE = 6;

    NonNullList<ItemStack> getAllItems();
    boolean isEmpty();
    ItemStack get(int index);
    ItemStack get(UUID uuid);
    void set(int index, ItemStack stack, UUID uuid);
    void set(int index, ItemStack stack);
    void swapSlots(int index1, int index2);
    boolean dropUUID(UUID uuid, BlockPos bp);
    boolean deleteItem(UUID uuid);
    boolean tryAdding(ItemStack itemStack);
    void giveTo(UUID uuid, UnitInventory inv);
}