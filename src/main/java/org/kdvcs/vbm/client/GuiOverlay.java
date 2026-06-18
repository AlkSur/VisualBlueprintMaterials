/*
Copyright (c) AlkSur
All codes include AI-assisted development and manual verification.
Licensed under MIT License with explicit commercial usage restrictions.
Any commercial deployment or commercial derivative development requires official authorization.
*/
package org.kdvcs.vbm.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.kdvcs.vbm.VisualBlueprintMaterials;
import org.kdvcs.vbm.config.VBMConfig;
import org.kdvcs.vbm.util.ClipboardReader;
import org.kdvcs.vbm.util.InventoryCounter;
import java.util.List;

public class GuiOverlay {

    private static final int BS = 16, PW = 172, MAX = 14, EH = 20;
    private static final ResourceLocation
        EMPTY_SCHEMATIC = new ResourceLocation("create", "empty_schematic"),
        BLUEPRINT = new ResourceLocation("create", "blueprint"),
        BRASS_INGOT = new ResourceLocation("create", "brass_ingot"),
        ANDESITE_NUGGET = new ResourceLocation("create", "andesite_nugget");
    private ItemStack cacheEmptySchematic, cacheBlueprint, cacheBrassIngot, cacheAndesiteNugget;

    private int bx, by, scrollOffset, lastMx, lastMy;
    private boolean open, locked;
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
        applyIgnored(mc.player);
    }

    private String blueprintFingerprint(net.minecraft.world.item.ItemStack clipboard) {
        java.util.List<ClipboardReader.MaterialEntry> all = ClipboardReader.readMaterials(clipboard);
        StringBuilder sb = new StringBuilder();
        for (ClipboardReader.MaterialEntry e : all) {
            sb.append(e.pageIndex).append(',').append(e.entryIndex).append('|');
        }
        return Integer.toHexString(sb.toString().hashCode());
    }

    private void applyIgnored(net.minecraft.world.entity.player.Player player) {
        net.minecraft.nbt.CompoundTag vbm = player.getPersistentData().getCompound(VisualBlueprintMaterials.MOD_ID);
        String fp = blueprintFingerprint(ClipboardReader.findClipboard(player));
        for (ClipboardReader.MaterialEntry e : mats) {
            String key = "ign_" + fp + '_' + e.pageIndex + '_' + e.entryIndex;
            e.ignored = vbm.contains(key) && vbm.getBoolean(key);
        }
    }

    private void saveIgnored(net.minecraft.world.entity.player.Player player, int page, int entry, boolean ignored) {
        net.minecraft.nbt.CompoundTag vbm = player.getPersistentData().getCompound(VisualBlueprintMaterials.MOD_ID);
        String fp = blueprintFingerprint(ClipboardReader.findClipboard(player));
        vbm.putBoolean("ign_" + fp + '_' + page + '_' + entry, ignored);
        player.getPersistentData().put(VisualBlueprintMaterials.MOD_ID, vbm);
    }

    private int[] btnPos(Screen screen) {
        if (screen instanceof AbstractContainerScreen<?> acs) {
            int x = acs.getGuiLeft() + acs.getXSize() + 2;
            if (x + BS > screen.width) x = acs.getGuiLeft() + acs.getXSize() - BS - 2;
            int y = acs.getGuiTop() + 2;
            if (y + BS > screen.height) y = screen.height - BS - 2;
            return new int[]{x, y};
        }
        return new int[]{screen.width + bx - BS, by};
    }

    /** Panel to the LEFT of the container screen — zero overlap with container slots, JEI, or right-side UI.
     *  Minimum 30px from left edge to keep FTB/IPN side buttons accessible. */
    private int[] panelPos(Screen screen, int cx, int cy, int ph) {
        int px;
        if (screen instanceof AbstractContainerScreen<?> acs) {
            px = acs.getGuiLeft() - PW - 4;
            if (px < 30) px = 30;
        } else {
            px = cx + BS + 2;
            if (px + PW > screen.width) px = Math.max(30, screen.width - PW - 2);
        }
        int py = Math.max(0, Math.min(cy, screen.height - ph));
        return new int[]{px, py};
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRender(ScreenEvent.Render.Post event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof AbstractContainerScreen)) return;
        Minecraft mc = Minecraft.getInstance();
        GuiGraphics g = event.getGuiGraphics();
        int mx = (int) event.getMouseX(), my = (int) event.getMouseY();
        lastMx = mx; lastMy = my;

        boolean clip = hasClipboard();
        boolean data = clip && !mats.isEmpty();

        int[] pos = btnPos(screen);
        int cx = pos[0], cy = pos[1];
        boolean hover = mx >= cx && mx <= cx + BS && my >= cy && my <= cy + BS;

        // Main button with shadow
        g.fill(cx + 1, cy + 1, cx + BS + 1, cy + BS + 1, 0xFF222222);
        g.fill(cx, cy, cx + BS, cy + BS, hover ? 0xFF999999 : 0xAA888888);
        g.fill(cx, cy, cx + BS, cy + 1, 0xFF555555);
        g.fill(cx, cy + BS - 1, cx + BS, cy + BS, 0xFF555555);
        g.fill(cx, cy, cx + 1, cy + BS, 0xFF555555);
        g.fill(cx + BS - 1, cy, cx + BS, cy + BS, 0xFF555555);
        ItemStack icon = getOrCache(EMPTY_SCHEMATIC);
        if (icon.isEmpty()) icon = getOrCache(BLUEPRINT);
        if (icon.isEmpty()) icon = new ItemStack(Items.BOOK);
        g.renderItem(icon, cx, cy);

        if (!open && clip)
            g.drawString(mc.font, data ? "C+D" : "C", cx + BS + 2, cy + 2, 0xFFFFAA00, false);
        if (hover)
            g.renderTooltip(mc.font, Component.literal(data ? mats.size() + " items" : (clip ? "Empty" : "No clipboard")), mx, my);
        if (!open || !data || mats.isEmpty()) return;

        // Panel
        int maxV = Math.min(MAX, mats.size());
        int ph = maxV * EH + 30;
        int[] pp = panelPos(screen, cx, cy, ph);
        int px = pp[0], py = pp[1];

        // Bg + borders: outer 2px #555, inner 1px brass
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

        // Lock button only
        int btnS = 14, lockX = px + PW - 18, lockY = py + 3;
        boolean lockHv = mx >= lockX && mx <= lockX + btnS && my >= lockY && my <= lockY + btnS;
        g.fill(lockX, lockY, lockX + btnS, lockY + btnS,
            locked ? (lockHv ? 0xFF66CC66 : 0xFF448844) : (lockHv ? 0xFFAA6666 : 0xFF664444));
        g.renderItem(getOrCache(locked ? BRASS_INGOT : ANDESITE_NUGGET), lockX - 1, lockY - 1);
        if (lockHv) g.renderTooltip(mc.font, Component.literal("Lock panel"), mx, my);

        // Items
        int listY = py + 18;
        int cs = Math.max(0, Math.min(scrollOffset, mats.size() - maxV));
        for (int i = 0; i < maxV; i++) {
            int idx = i + cs;
            if (idx >= mats.size()) break;
            ClipboardReader.MaterialEntry e = mats.get(idx);
            int ey = listY + i * EH;
            int have = InventoryCounter.countInInventory(mc.player, e.stack);
            boolean chHv = mx >= px + 4 && mx <= px + 16 && my >= ey + 1 && my <= ey + 15;

            // Checkbox
            g.fill(px + 4, ey + 1, px + 16, ey + 13, chHv ? 0xFF666666 : 0xFF444444);
            g.fill(px + 4, ey + 1, px + 16, ey + 2, 0xFF888888);
            g.fill(px + 4, ey + 12, px + 16, ey + 13, 0xFF888888);
            g.fill(px + 4, ey + 1, px + 5, ey + 13, 0xFF888888);
            g.fill(px + 15, ey + 1, px + 16, ey + 13, 0xFF888888);
            if (e.ignored) g.drawString(mc.font, "\u2713", px + 6, ey + 2, 0xFFFFFFFF, false);

            // Slot bg
            g.fill(px + 20, ey + 1, px + 38, ey + 19, 0xFF888888);
            g.fill(px + 19, ey, px + 39, ey + 1, 0xFF555555);
            g.fill(px + 19, ey + 19, px + 39, ey + 20, 0xFF555555);
            g.fill(px + 19, ey, px + 20, ey + 20, 0xFF555555);
            g.fill(px + 38, ey, px + 39, ey + 20, 0xFF555555);
            g.renderItem(e.stack, px + 21, ey + 2);

            boolean met = have >= e.required;
            int color = e.ignored ? 0xFF555555 : (met ? 0xFF55FF55 : 0xFFFF5555);

            // Name + counts, centered vertically at ey+6 (with 8px font, center at ey+10 = icon center)
            String name = e.stack.getHoverName().getString();
            if (name.length() > 11) name = name.substring(0, 10) + "..";
            g.drawString(mc.font, name, px + 42, ey + 6, e.ignored ? 0xFF555555 : 0xFFFFFFFF, false);
            g.drawString(mc.font, "x" + e.required, px + PW - 56, ey + 6, color, false);
            g.drawString(mc.font, String.valueOf(have), px + PW - 18, ey + 6, color, false);

            // Strikethrough at text center (ey+10)
            if (e.ignored)
                g.fill(px + 18, ey + 10, px + PW - 2, ey + 11, 0xFF555555);

            if (mx >= px + 4 && mx <= px + PW - 2 && my >= ey && my <= ey + EH && !chHv)
                g.renderTooltip(mc.font, Component.literal(e.stack.getHoverName().getString() + " x" + e.required), mx, my);
        }

        if (mats.size() > maxV) {
            int sh = Math.max(8, maxV * EH * maxV / mats.size());
            g.fill(px + PW - 4, listY + (maxV * EH - sh) * cs / (mats.size() - maxV), px + PW - 1,
                listY + (maxV * EH - sh) * cs / (mats.size() - maxV) + sh, 0x66FFFFFF);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onClick(ScreenEvent.MouseButtonPressed event) {
        Screen screen = event.getScreen();
        if (!(screen instanceof AbstractContainerScreen) || event.getButton() != 0) return;
        int mx = (int) event.getMouseX(), my = (int) event.getMouseY();
        int[] pos = btnPos(screen);
        int cx = pos[0], cy = pos[1];

        if (mx >= cx && mx <= cx + BS && my >= cy && my <= cy + BS) {
            if (hasClipboard()) { refresh(); open = !open; scrollOffset = 0; }
            event.setCanceled(true); return;
        }
        if (!open || mats.isEmpty()) return;

        int maxV = Math.min(MAX, mats.size());
        int ph = maxV * EH + 30;
        int[] pp = panelPos(screen, cx, cy, ph);
        int px = pp[0], py = pp[1];
        int listY = py + 18, cs = Math.max(0, Math.min(scrollOffset, mats.size() - maxV));

        // Lock
        int btnS = 14;
        if (mx >= px + PW - 18 && mx <= px + PW - 4 && my >= py + 3 && my <= py + 17) {
            locked = !locked; event.setCanceled(true); return;
        }

        for (int i = 0; i < maxV; i++) {
            int idx = i + cs;
            if (idx >= mats.size()) break;
            ClipboardReader.MaterialEntry e = mats.get(idx);
            int ey = listY + i * EH;
            if (mx >= px + 4 && mx <= px + 16 && my >= ey + 1 && my <= ey + 13) {
                e.ignored = !e.ignored;
                saveIgnored(Minecraft.getInstance().player, e.pageIndex, e.entryIndex, e.ignored);
                event.setCanceled(true); return;
            }
            if (mx >= px + 18 && mx <= px + PW - 2 && my >= ey && my <= ey + EH) {
                event.setCanceled(true); return;
            }
        }
        if (!locked && (mx < px || mx > px + PW || my < py || my > py + ph)) open = false;
        else if (mx >= px && mx <= px + PW && my >= py && my <= py + ph) event.setCanceled(true);
    }

    @SubscribeEvent
    public void onScroll(ScreenEvent.MouseScrolled.Pre event) {
        if (!open || mats.size() <= Math.min(MAX, mats.size())) return;
        Screen screen = event.getScreen();
        if (!(screen instanceof AbstractContainerScreen)) return;
        int mx = lastMx, my = lastMy;
        int[] pos = btnPos(screen);
        int cx = pos[0], cy = pos[1];
        int maxV = Math.min(MAX, mats.size());
        int ph = maxV * EH + 30;
        int[] pp = panelPos(screen, cx, cy, ph);
        int px = pp[0], py = pp[1];
        if (mx < px || mx > px + PW || my < py || my > py + ph) return;
        scrollOffset = Math.max(0, Math.min(scrollOffset - (int)Math.signum(event.getScrollDelta()), mats.size() - maxV));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onScreenClosing(ScreenEvent.Closing event) {
        if (!locked) {
            open = false;
            scrollOffset = 0;
        }
    }

    @SubscribeEvent
    public void onScreenInit(ScreenEvent.Init.Post event) {
        if (locked && event.getScreen() instanceof AbstractContainerScreen) {
            if (hasClipboard()) { refresh(); open = true; }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onInputKey(InputEvent.Key event) {
        if (!open || mats.isEmpty()) return;
        if (event.getAction() != org.lwjgl.glfw.GLFW.GLFW_PRESS) return;
        int key = event.getKey();
        if (key != org.lwjgl.glfw.GLFW.GLFW_KEY_R && key != org.lwjgl.glfw.GLFW.GLFW_KEY_U) return;
        Minecraft mc = Minecraft.getInstance();
        if (!(mc.screen instanceof AbstractContainerScreen screen)) return;
        int mx = lastMx, my = lastMy;
        int[] pos = btnPos(screen);
        int cx = pos[0], cy = pos[1];
        int maxV = Math.min(MAX, mats.size());
        int ph = maxV * EH + 30;
        int[] pp = panelPos(screen, cx, cy, ph);
        int px = pp[0], py = pp[1];
        int listY = py + 18, cs = Math.max(0, Math.min(scrollOffset, mats.size() - maxV));
        for (int i = 0; i < maxV; i++) {
            int idx = i + cs;
            if (idx >= mats.size()) break;
            ClipboardReader.MaterialEntry e = mats.get(idx);
            int ey = listY + i * EH;
            if (mx >= px + 18 && mx <= px + PW - 2 && my >= ey && my <= ey + EH) {
                callJeiShowRecipe(e.stack, key);
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public void onKey(ScreenEvent.KeyPressed event) {
        if (!open || mats.isEmpty()) return;
        int key = event.getKeyCode();
        if (key != org.lwjgl.glfw.GLFW.GLFW_KEY_R && key != org.lwjgl.glfw.GLFW.GLFW_KEY_U) return;
        Screen screen = event.getScreen();
        if (!(screen instanceof AbstractContainerScreen)) return;
        int mx = lastMx, my = lastMy;
        int[] pos = btnPos(screen);
        int cx = pos[0], cy = pos[1];
        int maxV = Math.min(MAX, mats.size());
        int ph = maxV * EH + 30;
        int[] pp = panelPos(screen, cx, cy, ph);
        int px = pp[0], py = pp[1];
        int listY = py + 18, cs = Math.max(0, Math.min(scrollOffset, mats.size() - maxV));
        for (int i = 0; i < maxV; i++) {
            int idx = i + cs;
            if (idx >= mats.size()) break;
            ClipboardReader.MaterialEntry e = mats.get(idx);
            int ey = listY + i * EH;
            if (mx >= px + 18 && mx <= px + PW - 2 && my >= ey && my <= ey + EH) {
                callJeiShowRecipe(e.stack, key);
                event.setCanceled(true);
                return;
            }
        }
    }

    private static void callJeiShowRecipe(ItemStack stack, int key) {
        if (!net.minecraftforge.fml.ModList.get().isLoaded("jei")) return;
        try {
            Class.forName("org.kdvcs.vbm.client.JeiBridge")
                .getMethod("showRecipe", ItemStack.class, int.class)
                .invoke(null, stack, key);
        } catch (Exception ignored) {}
    }
}