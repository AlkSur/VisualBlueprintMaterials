/*
Copyright (c) AlkSur
All codes include AI-assisted development and manual verification.
Licensed under MIT License with explicit commercial usage restrictions.
Any commercial deployment or commercial derivative development requires official authorization.
*/
package org.kdvcs.vbm.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class InventoryCounter {
    public static int countInInventory(Player player, ItemStack target) {
        int count = 0;
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && stack.getItem() == target.getItem()) {
                count += stack.getCount();
            }
        }
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty() && offhand.getItem() == target.getItem()) {
            count += offhand.getCount();
        }
        for (ItemStack stack : player.getInventory().armor) {
            if (!stack.isEmpty() && stack.getItem() == target.getItem()) {
                count += stack.getCount();
            }
        }
        return count;
    }
}