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

    /** 统计玩家背包（含快捷栏+副手）中与 target 相同的物品总数 */
    public static int countInInventory(Player player, ItemStack target) {
        int count = 0;
        // 主背包 36 格（含快捷栏）
        for (ItemStack stack : player.getInventory().items) {
            if (!stack.isEmpty() && ItemStack.isSameItem(stack, target)) {
                count += stack.getCount();
            }
        }
        // 副手
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.isEmpty() && ItemStack.isSameItem(offhand, target)) {
            count += offhand.getCount();
        }
        // 盔甲栏（有些模组在盔甲格放特殊物品）
        for (ItemStack stack : player.getInventory().armor) {
            if (!stack.isEmpty() && ItemStack.isSameItem(stack, target)) {
                count += stack.getCount();
            }
        }
        return count;
    }
}