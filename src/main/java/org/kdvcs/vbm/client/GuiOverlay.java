/*
Copyright (c) AlkSur
All codes include AI-assisted development and manual verification.
Licensed under MIT License with explicit commercial usage restrictions.
Any commercial deployment or commercial derivative development requires official authorization.
*/
package org.kdvcs.vbm.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.kdvcs.vbm.config.VBMConfig;
import org.kdvcs.vbm.util.ClipboardReader;
import org.kdvcs.vbm.util.InventoryCounter;
import org.lwjgl.glfw.GLFW;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiOverlay {

    public static final KeyMapping MOVE_KEY = new KeyMapping(
        "key.create_qbp.move", GLFW.GLFW_KEY_END, "key.categories.create_qbp");
    public static final KeyMapping MOVE_UP = new KeyMapping(
        "key.create_qbp.move_up", GLFW.GLFW_KEY_UNKNOWN, "key.categories.create_qbp");
    public static final KeyMapping MOVE_DOWN = new KeyMapping(
        "key.create_qbp.move_down", GLFW.GLFW_KEY_UNKNOWN, "key.categories.create_qbp");
    public static final KeyMapping MOVE_LEFT = new KeyMapping(
        "key.create_qbp.move_left", GLFW.GLFW_KEY_UNKNOWN, "key.categories.create_qbp");
    public static final KeyMapping MOVE_RIGHT = new KeyMapping(
        "key.create_qbp.move_right", GLFW.GLFW_KEY_UNKNOWN, "key.categories.create_qbp");

    private static final int BS = 16, PW = 172, MAX = 14, EH = 20;
    private static final ResourceLocation
        EMPTY_SCHEMATIC = ResourceLocation.fromNamespaceAndPath("create", "empty_schematic"),
        BLUEPRINT = ResourceLocation.fromNamespaceAndPath("create", "blueprint"),
        BRASS_INGOT = ResourceLocation.fromNamespaceAndPath("create", "brass_ingot"),
        ANDESITE_NUGGET = ResourceLocation.fromNamespaceAndPath("create", "andesite_nugget");
    private ItemStack cacheEmptySchematic, cacheBlueprint, cacheBrassIngot, cacheAndesiteNugget;

    private int bx, by, scrollOffset, lastMx, lastMy;
    private boolean open, locked, editMode;
    private List<ClipboardReader.MaterialEntry> mats = List.of();

    public GuiOverlay() { bx = VBMConfig.buttonX; by = VBMConfig.buttonY; }

    private ItemStack getOrCache(ResourceLocation id) {
        if (EMPTY_SCHEMATIC.equals(id) && cacheEmptySchematic != null) return cacheEmptySchematic;
        if (BLUEPRINT.equals(id) && cacheBlueprint != null) return cacheBlueprint;
        if (BRASS_INGOT.equals(id) && cacheBrassIngot != null) return cacheBrassIngot;
        if (ANDESITE_NUGGET.equals(id) && cacheAndesiteNugget != null) return cacheAndesiteNugget;
        var item = BuiltInRegistries.ITEM.get(id);
        ItemStack s = item != null ? new ItemStack(item) : ItemStack.EMPTY;
        if (EMPTY_SCHEMATIC.equals(id)) cacheEmptySchematic = s;
        else if (BLUEPRINT.equals(id)) cacheBlueprint = s;
        else if (BRASS_INGOT.equals(id)) cacheBrassIngot = s;
        else if (ANDESITE_NUGGET.equals(id)) cacheAndesiteNugget = s;
        return s;
    }

    private boolean hasClipboard() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        for (ItemStack s : mc.player.getInventory().items)
            if (ClipboardReader.isClipboard(s)) return true;
        return ClipboardReader.isClipboard(mc.player.getOffhandItem());
    }

    private void refresh() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) { mats = List.of(); return; }
        ItemStack cb = ClipboardReader.findClipboard(mc.player);
        mats = cb.isEmpty() ? List.of() : ClipboardReader.readMaterials(cb);
        String fp = blueprintFingerprint(cb);
        restoreRequiredCounts(fp);
        applyIgnoredFromFile(fp);
    }

    private String blueprintFingerprint(net.minecraft.world.item.ItemStack clipboard) {
        java.util.List<ClipboardReader.MaterialEntry> all = ClipboardReader.readMaterials(clipboard);
        java.util.TreeSet<String> ids = new java.util.TreeSet<>();
        for (ClipboardReader.MaterialEntry e : all)
            ids.add(BuiltInRegistries.ITEM.getKey(e.stack.getItem()).toString());
        return Integer.toHexString(String.join(",", ids).hashCode());
    }

    @SuppressWarnings("unchecked")
    private void applyIgnoredFromFile(String fp) {
        Map<String, Boolean> map = loadIgnoreMap();
        for (ClipboardReader.MaterialEntry e : mats) {
            String key = BuiltInRegistries.ITEM.getKey(e.stack.getItem()).toString();
            e.ignored = map.getOrDefault(fp + ":" + key, false);
        }
    }

    @SuppressWarnings("unchecked")
    private void saveIgnored(net.minecraft.world.entity.player.Player player, net.minecraft.world.item.ItemStack item, boolean ignored) {
        String fp = blueprintFingerprint(ClipboardReader.findClipboard(player));
        String key = BuiltInRegistries.ITEM.getKey(item.getItem()).toString();
        Map<String, Boolean> map = loadIgnoreMap();
        if (ignored) map.put(fp + ":" + key, true);
        else map.remove(fp + ":" + key);
        saveIgnoreMap(map);
    }

    private Path ignoreFile() {
        return net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath()
            .resolve("VisualBlueprintMaterials").resolve("ignored.json");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Boolean> loadIgnoreMap() {
        try {
            Path p = ignoreFile();
            if (Files.exists(p)) {
                String json = Files.readString(p);
                Map<String, Boolean> map = new Gson().fromJson(json, new TypeToken<Map<String, Boolean>>(){}.getType());
                return map != null ? map : new HashMap<>();
            }
        } catch (Exception ignored) {}
        return new HashMap<>();
    }

    private void saveIgnoreMap(Map<String, Boolean> map) {
        try {
            Path p = ignoreFile();
            Files.createDirectories(p.getParent());
            Files.writeString(p, new Gson().toJson(map));
        } catch (Exception ignored) {}
    }

    // ---- Required count cache (Create zeroes item_amount for checked items) ----
    private Path requiredFile() {
        return net.minecraft.client.Minecraft.getInstance().gameDirectory.toPath()
            .resolve("VisualBlueprintMaterials").resolve("required_cache.json");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Map<String, Integer>> loadRequiredCache() {
        try {
            Path p = requiredFile();
            if (Files.exists(p)) {
                String json = Files.readString(p);
                Map<String, Map<String, Integer>> map = new Gson().fromJson(json,
                    new TypeToken<Map<String, Map<String, Integer>>>(){}.getType());
                return map != null ? map : new HashMap<>();
            }
        } catch (Exception ignored) {}
        return new HashMap<>();
    }

    private void saveRequiredCache(Map<String, Map<String, Integer>> cache) {
        try {
            Path p = requiredFile();
            Files.createDirectories(p.getParent());
            Files.writeString(p, new Gson().toJson(cache));
        } catch (Exception ignored) {}
    }

    private void restoreRequiredCounts(String fp) {
        Map<String, Map<String, Integer>> cache = loadRequiredCache();
        Map<String, Integer> forFp = cache.computeIfAbsent(fp, k -> new HashMap<>());
        boolean updated = false;
        for (ClipboardReader.MaterialEntry e : mats) {
            String id = BuiltInRegistries.ITEM.getKey(e.stack.getItem()).toString();
            if (!e.checked && e.required > 0) {
                // Cache the original required count before Create zeroes it
                forFp.put(id, e.required);
                updated = true;
            } else if (e.checked && e.required == 0 && forFp.containsKey(id)) {
                // Restore from cache
                int orig = forFp.get(id);
                e.required = orig;
                e.stack = new net.minecraft.world.item.ItemStack(e.stack.getItem(), orig);
            }
        }
        if (updated) {
            cache.put(fp, forFp);
            saveRequiredCache(cache);
        }
    }

    private int[] btnPos(Screen screen) {
        if (screen instanceof AbstractContainerScreen<?> acs)
            return new int[]{acs.getGuiLeft() + bx, acs.getGuiTop() + by};
        return new int[]{screen.width + bx - BS, by};
    }

    private int[] panelPos(Screen screen, int cx, int cy, int ph) {
        int px, py;
        if (screen instanceof AbstractContainerScreen<?> acs) {
            px = acs.getGuiLeft() - PW - 4;
            py = Math.max(2, Math.min(acs.getGuiTop() + by, screen.height - ph - 2));
            if (VBMConfig.movePanelX) px = cx - PW - BS - 4;
            if (VBMConfig.movePanelY) py = Math.max(2, Math.min(cy, screen.height - ph - 2));
        } else {
            px = cx - PW - BS - 4;
            py = Math.max(2, Math.min(cy, screen.height - ph - 2));
        }
        if (px < 2) px = 2;
        if (px + PW > screen.width) px = screen.width - PW - 2;
        return new int[]{px, py};
    }

    // ========== Render ==========
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRender(ScreenEvent.Render.Post event) {
        var s = event.getScreen();
        if (!(s instanceof AbstractContainerScreen)) return;

        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = event.getGuiGraphics();
        int mx = (int) event.getMouseX(), my = (int) event.getMouseY();
        lastMx = mx; lastMy = my;

        // Edit mode overlay
        if (editMode) {
            g.fill(0, 0, s.width, s.height, 0x55000000);
            g.drawString(mc.font, "[Edit Mode] X:" + bx + " Y:" + by, s.width / 2 - 60, s.height / 2, 0xFFFFFF, false);
        }

        boolean clip = hasClipboard();
        boolean data = clip && !mats.isEmpty();
        int[] pos = btnPos(s);
        int cx = pos[0], cy = pos[1];
        boolean hover = mx >= cx && mx <= cx + BS && my >= cy && my <= cy + BS;

        // Button
        g.fill(cx + 1, cy + 1, cx + BS + 1, cy + BS + 1, 0xFF222222);
        g.fill(cx, cy, cx + BS, cy + BS, hover ? 0xFF999999 : 0xAA888888);
        g.fill(cx, cy, cx + BS, cy + 1, 0xFF555555);
        g.fill(cx, cy + BS - 1, cx + BS, cy + BS, 0xFF555555);
        g.fill(cx, cy, cx + 1, cy + BS, 0xFF555555);
        g.fill(cx + BS - 1, cy, cx + BS, cy + BS, 0xFF555555);
        ItemStack icon = getOrCache(EMPTY_SCHEMATIC);
        if (icon.isEmpty()) icon = getOrCache(BLUEPRINT);
        if (icon.isEmpty()) icon = new ItemStack(Items.BOOK);
        g.renderFakeItem(icon, cx, cy);

        // Status label
        if (!open && clip)
            g.drawString(mc.font, data ? "C+D" : "C", cx, cy - 2, 0xFFFFAA00, false);
        if (hover && !editMode)
            g.renderTooltip(mc.font, Component.literal(data ? mats.size() + " items" : (clip ? "Empty" : "No clipboard")), mx, my);

        if (!open || !data || mats.isEmpty()) return;

        // Panel
        int maxV = Math.min(MAX, mats.size());
        int ph = maxV * EH + 30;
        int[] pp = panelPos(s, cx, cy, ph);
        int px = pp[0], py = pp[1];

        g.fill(px, py, px + PW, py + ph, 0xFF2B2B2B);
        g.fill(px, py, px + PW, py + 2, 0xFF555555);
        g.fill(px, py + ph - 2, px + PW, py + ph, 0xFF555555);
        g.fill(px, py, px + 2, py + ph, 0xFF555555);
        g.fill(px + PW - 2, py, px + PW, py + ph, 0xFF555555);
        g.fill(px + 2, py + 2, px + PW - 2, py + 3, 0xFFB8860B);
        g.fill(px + 2, py + ph - 3, px + PW - 2, py + ph - 2, 0xFFB8860B);
        g.fill(px + 2, py + 2, px + 3, py + ph - 2, 0xFFB8860B);
        g.fill(px + PW - 3, py + 2, px + PW - 2, py + ph - 2, 0xFFB8860B);

        g.drawString(mc.font, "Materials", px + 6, py + 5, 0xFFFFAA00, false);

        int btnS = 14;
        boolean lockHv = mx >= px + PW - 18 && mx <= px + PW - 4 && my >= py + 3 && my <= py + 17;
        g.fill(px + PW - 18, py + 3, px + PW - 4, py + btnS + 3,
            locked ? (lockHv ? 0xFF66CC66 : 0xFF448844) : (lockHv ? 0xFFAA6666 : 0xFF664444));
        g.renderFakeItem(getOrCache(locked ? BRASS_INGOT : ANDESITE_NUGGET), px + PW - 19, py + 2);
        if (lockHv) g.renderTooltip(mc.font, Component.literal("Lock panel"), mx, my);

        int listY = py + 18, cs = Math.max(0, Math.min(scrollOffset, mats.size() - maxV));
        for (int i = 0; i < maxV; i++) {
            int idx = i + cs;
            if (idx >= mats.size()) break;
            ClipboardReader.MaterialEntry e = mats.get(idx);
            int ey = listY + i * EH;
            int have = InventoryCounter.countInInventory(mc.player, e.stack);
            boolean chHv = mx >= px + 4 && mx <= px + 16 && my >= ey + 1 && my <= ey + 15;

            g.fill(px + 4, ey + 1, px + 16, ey + 13, chHv ? 0xFF666666 : 0xFF444444);
            g.fill(px + 4, ey + 1, px + 16, ey + 2, 0xFF888888);
            g.fill(px + 4, ey + 12, px + 16, ey + 13, 0xFF888888);
            g.fill(px + 4, ey + 1, px + 5, ey + 13, 0xFF888888);
            g.fill(px + 15, ey + 1, px + 16, ey + 13, 0xFF888888);
            if (e.checked) g.drawString(mc.font, "\u2713", px + 6, ey + 2, 0xFF55FF55, false);
            else if (e.ignored) g.drawString(mc.font, "\u2713", px + 6, ey + 2, 0xFFFFFFFF, false);

            g.fill(px + 20, ey + 1, px + 38, ey + 19, 0xFF888888);
            g.fill(px + 19, ey, px + 39, ey + 1, 0xFF555555);
            g.fill(px + 19, ey + 19, px + 39, ey + 20, 0xFF555555);
            g.fill(px + 19, ey, px + 20, ey + 20, 0xFF555555);
            g.fill(px + 38, ey, px + 39, ey + 20, 0xFF555555);
            g.renderFakeItem(e.stack, px + 21, ey + 2);

            boolean met = have >= e.required;
            int color = e.checked ? 0xFF55FF55 : (e.ignored ? 0xFF555555 : (met ? 0xFF55FF55 : 0xFFFF5555));
            String name = e.stack.getHoverName().getString();
            if (name.length() > 11) name = name.substring(0, 10) + "..";
            g.drawString(mc.font, name, px + 42, ey + 6, e.checked ? 0xFF55FF55 : (e.ignored ? 0xFF555555 : 0xFFFFFFFF), false);
            g.drawString(mc.font, "x" + e.required, px + PW - 56, ey + 6, color, false);
            g.drawString(mc.font, String.valueOf(have), px + PW - 18, ey + 6, color, false);

            if (e.checked) g.fill(px + 18, ey + 10, px + PW - 2, ey + 11, 0xFF55FF55);
            else if (e.ignored) g.fill(px + 18, ey + 10, px + PW - 2, ey + 11, 0xFF555555);
            if (mx >= px + 4 && mx <= px + PW - 2 && my >= ey && my <= ey + EH && !chHv)
                g.renderTooltip(mc.font, Component.literal(e.stack.getHoverName().getString() + " x" + e.required), mx, my);
        }
        if (mats.size() > maxV) {
            int sh = Math.max(8, maxV * EH * maxV / mats.size());
            g.fill(px + PW - 4, listY + (maxV * EH - sh) * cs / (mats.size() - maxV), px + PW - 1,
                listY + (maxV * EH - sh) * cs / (mats.size() - maxV) + sh, 0x66FFFFFF);
        }
    }

    // ========== Click: button + panel ==========
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        var s = event.getScreen();
        if (!(s instanceof AbstractContainerScreen) || event.getButton() != 0) return;
        int mx = (int) event.getMouseX(), my = (int) event.getMouseY();
        int[] pos = btnPos(s);
        int cx = pos[0], cy = pos[1];

        // Button click
        if (mx >= cx && mx <= cx + BS && my >= cy && my <= cy + BS) {
            if (hasClipboard()) { refresh();
            open = !open; scrollOffset = 0; }
            event.setCanceled(true); return;
        }
        if (!open || mats.isEmpty()) return;

        int maxV = Math.min(MAX, mats.size());
        int ph = maxV * EH + 30;
        int[] pp = panelPos(s, cx, cy, ph);
        int px = pp[0], py = pp[1];
        int listY = py + 18, cs = Math.max(0, Math.min(scrollOffset, mats.size() - maxV));

        // Lock
        if (mx >= px + PW - 18 && mx <= px + PW - 4 && my >= py + 3 && my <= py + 17) {
            locked = !locked; event.setCanceled(true); return;
        }
        // Checkboxes + items
        for (int i = 0; i < maxV; i++) {
            int idx = i + cs;
            if (idx >= mats.size()) break;
            ClipboardReader.MaterialEntry e = mats.get(idx);
            int ey = listY + i * EH;
            if (mx >= px + 4 && mx <= px + 16 && my >= ey + 1 && my <= ey + 13) {
                e.ignored = !e.ignored;
                saveIgnored(Minecraft.getInstance().player, e.stack, e.ignored);
                event.setCanceled(true); return;
            }
            if (mx >= px + 18 && mx <= px + PW - 2 && my >= ey && my <= ey + EH)
                { event.setCanceled(true); return; }
        }
        if (!locked && (mx < px || mx > px + PW || my < py || my > py + ph)) open = false;
        else if (mx >= px && mx <= px + PW && my >= py && my <= py + ph) event.setCanceled(true);
    }

    // ========== Scroll ==========
    @SubscribeEvent
    public void onScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!open || mats.size() <= MAX) return;
        if (!(event.getScreen() instanceof AbstractContainerScreen)) return;
        int mx = lastMx, my = lastMy;
        int[] pos = btnPos(event.getScreen());
        int maxV = Math.min(MAX, mats.size());
        int[] pp = panelPos(event.getScreen(), pos[0], pos[1], maxV * EH + 30);
        if (mx < pp[0] || mx > pp[0] + PW || my < pp[1] || my > pp[1] + maxV * EH + 30) return;
        scrollOffset = Math.max(0, Math.min(scrollOffset - (int)Math.signum(event.getScrollDeltaY()), mats.size() - maxV));
        event.setCanceled(true);
    }

    // ========== Closing / Init ==========
    @SubscribeEvent
    public void onScreenClosing(ScreenEvent.Closing event) {
        editMode = false;
        if (!locked) { open = false; scrollOffset = 0; }
    }

    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {
        if (locked && event.getScreen() instanceof AbstractContainerScreen && hasClipboard()) {
            refresh();
            open = true;
        }
    }

    // ========== Keys: edit mode + R/U JEI ==========
    @SubscribeEvent
    public void onKey(ScreenEvent.KeyPressed.Pre event) {
        int kc = event.getKeyCode();
        // Toggle edit mode
        if (kc == MOVE_KEY.getKey().getValue() && kc != -1) {
            editMode = !editMode;
            event.setCanceled(true);
            return;
        }
        // Edit mode movement
        if (editMode) {
            boolean moved = true;
            int uv = MOVE_UP.getKey().getValue(); if (kc == uv && kc != -1) by -= 1;
            else {
                int dv = MOVE_DOWN.getKey().getValue(); if (kc == dv && kc != -1) by += 1;
                else {
                    int lv = MOVE_LEFT.getKey().getValue(); if (kc == lv && kc != -1) bx -= 1;
                    else {
                        int rv = MOVE_RIGHT.getKey().getValue(); if (kc == rv && kc != -1) bx += 1;
                        else moved = false;
                    }
                }
            }
            if (moved) { event.setCanceled(true); return; }
        }
        // R/U JEI lookup
        if (!open || mats.isEmpty()) return;
        if (kc != GLFW.GLFW_KEY_R && kc != GLFW.GLFW_KEY_U) return;
        var s = event.getScreen();
        if (!(s instanceof AbstractContainerScreen)) return;
        int[] pos = btnPos(s);
        int maxV = Math.min(MAX, mats.size());
        int[] pp = panelPos(s, pos[0], pos[1], maxV * EH + 30);
        int listY = pp[1] + 18, cs = Math.max(0, Math.min(scrollOffset, mats.size() - maxV));
        for (int i = 0; i < maxV; i++) {
            int idx = i + cs;
            if (idx >= mats.size()) break;
            ClipboardReader.MaterialEntry e = mats.get(idx);
            int ey = listY + i * EH;
            if (lastMx >= pp[0] + 18 && lastMx <= pp[0] + PW - 2 && lastMy >= ey && lastMy <= ey + EH) {
                callJeiShowRecipe(e.stack, kc);
                event.setCanceled(true);
                return;
            }
        }
    }

    // ========== JEI ==========
    private static void callJeiShowRecipe(ItemStack stack, int key) {
        if (!ModList.get().isLoaded("jei")) return;
        try {
            Class.forName("org.kdvcs.vbm.client.JeiBridge")
                .getMethod("showRecipe", ItemStack.class, int.class)
                .invoke(null, stack, key);
        } catch (Exception ignored) {}
    }
}