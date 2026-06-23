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

    public static final ResourceLocation CLIPBOARD_ID = ResourceLocation.fromNamespaceAndPath("create", "clipboard");

    public static boolean isClipboard(ItemStack stack) {
        if (stack.isEmpty()) return false;
        return CLIPBOARD_ID.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static List<MaterialEntry> readMaterials(ItemStack clipboard) {
        List<MaterialEntry> result = new ArrayList<>();
        if (!isClipboard(clipboard)) return result;

        CompoundTag tag = readBlueprintTag(clipboard);
        if (tag == null) return result;

        /*
         * ============================================================
         * Create NBT structure changed significantly in 1.21.1
         * ============================================================
         *
         * 1.20.1 (Forge): raw NBT in ItemStack.tag
         *   Pages: [{ Entries: [{ Icon: {id, Count}, ItemAmount }, ...] }]
         *   - PascalCase keys
         *   - Page is CompoundTag with "Entries" sub-list
         *
         * 1.21.1 (NeoForge): "create:clipboard_content" component
         *   pages: [[{ icon: {id, count}, item_amount }, ...]]
         *   - snake_case keys
         *   - Page is directly a ListTag (NO "Entries" wrapper)
         *   - Component read via ItemStack.save() fallback
         * ============================================================
         */
        Tag pagesTag = tag.get("pages");
        if (pagesTag == null) pagesTag = tag.get("Pages"); // fallback for 1.20.1
        if (!(pagesTag instanceof ListTag pages)) return result;

        for (int i = 0; i < pages.size(); i++) {
            Tag pageTag = pages.get(i);
            ListTag entries = null;
            if (pageTag instanceof ListTag directList)
                entries = directList; // 1.21.1: page IS the list
            else if (pageTag instanceof CompoundTag pg) {
                Tag eTag = pg.get("entries");
                if (eTag == null) eTag = pg.get("Entries");
                if (eTag instanceof ListTag nested) entries = nested;
            }
            if (entries == null) continue;

            for (int j = 0; j < entries.size(); j++) {
                if (!(entries.get(j) instanceof CompoundTag entry)) continue;
                // Try lowercase icon first, then capital
                Tag iTag = entry.get("icon");
                if (iTag == null) iTag = entry.get("Icon");
                if (!(iTag instanceof CompoundTag icon)) continue;
                if (!icon.contains("id")) continue;

                String itemId = icon.getString("id");
                int count = 0;
                if (entry.contains("item_amount")) count = entry.getInt("item_amount");
                else if (entry.contains("ItemAmount")) count = entry.getInt("ItemAmount");
                // Skip placeholders, but keep Create-checked items (count zeroed by cannon)
                if (count <= 0) {
                    // Try to recover original required count from text hint
                    // Create preserves "+N" in text.extra even after zeroing item_amount
                    // Works for both checked items (cannon satisfied) and unchecked items (cannon un-check pending)
                    if (entry.contains("text")) {
                        Tag textTag = entry.get("text");
                        if (textTag instanceof CompoundTag textCt && textCt.contains("extra")) {
                            Tag extraTag = textCt.get("extra");
                            if (extraTag instanceof ListTag extra) {
                                for (int k = extra.size() - 1; k >= 0; k--) {
                                    if (extra.get(k) instanceof CompoundTag part && part.contains("text")) {
                                        String txt = part.getString("text");
                                        int plusIdx = txt.lastIndexOf('+');
                                        if (plusIdx >= 0) {
                                            try { count = Integer.parseInt(txt.substring(plusIdx + 1).trim()); } catch (Exception ignored) {}
                                            break;
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Still 0 after text parsing -> truly a placeholder (empty icon, >>>)
                    if (count <= 0) continue;
                }

                ResourceLocation rl = ResourceLocation.tryParse(itemId);
                if (rl == null) continue;
                var item = BuiltInRegistries.ITEM.get(rl);
                if (item == null) continue;

                MaterialEntry me = new MaterialEntry();
                me.stack = new ItemStack(item, count);
                me.required = count;
                me.checked = entry.contains("checked") && entry.getByte("checked") == 1;
                me.ignored = false;
                me.pageIndex = i;
                me.entryIndex = j;
                result.add(me);
            }
        }
        return result;
    }

    private static CompoundTag readBlueprintTag(ItemStack clipboard) {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null) return null;

        var raw = clipboard.save(mc.level.registryAccess());
        if (!(raw instanceof CompoundTag full) || !full.contains("components")) return null;

        CompoundTag comps = full.getCompound("components");
        String[] keys = {"create:clipboard_content", "create:clipboard_data", "create:blueprint"};
        for (String key : keys) {
            var val = comps.get(key);
            if (val instanceof CompoundTag ct && (ct.contains("pages") || ct.contains("Pages"))) {
                return ct;
            }
        }
        return null;
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
        public boolean checked;
        public int pageIndex, entryIndex;
    }
}
