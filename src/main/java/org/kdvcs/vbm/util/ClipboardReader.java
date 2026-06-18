/*
Copyright (c) AlkSur
All codes include AI-assisted development and manual verification.
Licensed under MIT License with explicit commercial usage restrictions.
Any commercial deployment or commercial derivative development requires official authorization.
*/
package org.kdvcs.vbm.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class ClipboardReader {

    public static final ResourceLocation CLIPBOARD_ID = new ResourceLocation("create", "clipboard");

    public static boolean isClipboard(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return CLIPBOARD_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static boolean hasData(ItemStack stack) {
        return isClipboard(stack) && stack.getTag() != null;
    }

    public static List<MaterialEntry> readMaterials(ItemStack clipboard) {
        List<MaterialEntry> result = new ArrayList<>();
        if (!hasData(clipboard)) return result;
        CompoundTag tag = clipboard.getTag();
        if (tag == null || !tag.contains("Pages")) return result;

        Tag pagesTag = tag.get("Pages");
        if (!(pagesTag instanceof ListTag pages)) return result;

        for (int i = 0; i < pages.size(); i++) {
            if (!(pages.get(i) instanceof CompoundTag pg) || !pg.contains("Entries")) continue;
            Tag eTag = pg.get("Entries");
            if (!(eTag instanceof ListTag entries)) continue;

            for (int j = 0; j < entries.size(); j++) {
                if (!(entries.get(j) instanceof CompoundTag entry)) continue;
                if (!entry.contains("Icon")) continue;
                Tag iTag = entry.get("Icon");
                if (!(iTag instanceof CompoundTag icon) || !icon.contains("id")) continue;

                String itemId = icon.getString("id");
                int count = entry.contains("ItemAmount") ? entry.getInt("ItemAmount") : 1;
                boolean ignored = false;
                ResourceLocation rl = ResourceLocation.tryParse(itemId);
                if (rl == null) continue;
                var item = BuiltInRegistries.ITEM.get(rl);
                if (item == null) continue;

                MaterialEntry me = new MaterialEntry();
                me.stack = new ItemStack(item, count);
                me.required = count;
                me.ignored = ignored;
                me.pageIndex = i;
                me.entryIndex = j;
                result.add(me);
            }
        }
        return result;
    }

    public static ItemStack findClipboard(Player player) {
        for (ItemStack s : player.getInventory().items)
            if (isClipboard(s)) return s;
        if (isClipboard(player.getOffhandItem()))
            return player.getOffhandItem();
        return ItemStack.EMPTY;
    }

    public static class MaterialEntry {
        public ItemStack stack;
        public int required;
        public boolean ignored;
        public int pageIndex, entryIndex;
    }
}