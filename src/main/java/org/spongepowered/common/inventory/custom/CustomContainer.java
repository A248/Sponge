/*
 * This file is part of Sponge, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.spongepowered.common.inventory.custom;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import org.spongepowered.api.item.inventory.ContainerType;

public class CustomContainer extends Container {

    public CustomInventory inv;

    public CustomContainer(int id, final PlayerEntity player, final CustomInventory inventory, ContainerType type) {
        super((net.minecraft.inventory.container.ContainerType<?>) type, id);
        this.inv = inventory;

        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            this.addSlot(new Slot(inventory, slot, 0, 0));
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(player.inventory, (row + 1) * 9 + col, 0, 0));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(player.inventory, col, 0, 0));
        }
    }

    @Override
    public boolean canInteractWith(final PlayerEntity playerIn) {
        return true;
    }

    @Override
    public void onContainerClosed(final PlayerEntity playerIn) {
        super.onContainerClosed(playerIn);
        this.inv.closeInventory(playerIn);
    }

    @Override
    public ItemStack transferStackInSlot(final PlayerEntity playerIn, final int index) {
        // Almost 1:1 copy of ChestContainer#transferStackInSlot
        ItemStack itemstack = ItemStack.EMPTY;
        final Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            final ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            if (index < this.inv.getSizeInventory()) {
                if (!this.mergeItemStack(itemstack1, this.inv.getSizeInventory(), this.inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!this.mergeItemStack(itemstack1, 0, this.inv.getSizeInventory(), false)) {
                return ItemStack.EMPTY;
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }

        return itemstack;
    }
}
