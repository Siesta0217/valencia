package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Top-right "ArrayList" HUD — the classic enabled-module list, right-aligned
 * and sorted by text width (longest on top) for the staircase look.
 *
 * Each visible row gets an optional dark backplate and a coloured tab on the
 * far-right edge. Colour is either the ClickGUI accent or a vertical rainbow
 * (Astolfo-style hue cycle, same as the ClickGUI version string).
 */
public final class ArrayListMod {

    private static boolean enabled = false;

    /** Vertical rainbow hue cycle instead of the flat accent colour. */
    public static boolean rainbow = true;
    /** Dark backplate behind each row. */
    public static boolean background = true;

    // Keyless modules the HUD still surfaces — derived from the registry's
    // surfaceInArrayList flag (Hitbox/NoCrash/AutoFish/ElytraGoto/ESP), so
    // this list can never drift from the real roster. Pure-HUD readouts
    // (TargetHUD/DimCoord) and the ArrayList itself stay excluded.
    private static final List<Keybinds.ModuleEntry> EXTRAS =
        Modules.ALL.stream()
            .filter(Modules.ModuleDef::surfaceInArrayList)
            .map(d -> new Keybinds.ModuleEntry(d.label(), d.enabled()))
            .toList();

    private ArrayListMod() {}

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    public static void render(GuiGraphics g) {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        Font font = mc.font;
        ModConfig cfg = ModConfig.get();

        // Collect active labels (key-toggleable modules from Keybinds + the
        // keybind-less extras), longest first → staircase.
        List<String> active = new ArrayList<>();
        for (Keybinds.ModuleEntry e : Keybinds.TOGGLE_ENTRIES)
            if (e.enabled().getAsBoolean()) active.add(e.label());
        for (Keybinds.ModuleEntry e : EXTRAS)
            if (e.enabled().getAsBoolean()) active.add(e.label());
        if (active.isEmpty()) return;
        active.sort((a, b) -> font.width(b) - font.width(a));

        int viewW = mc.getWindow().getGuiScaledWidth();
        int lineH = font.lineHeight + 2;
        int bgAlpha = clamp(cfg.bgAlpha, 60, 220);
        int accent = 0xFF000000
            | ((cfg.accentR & 0xFF) << 16)
            | ((cfg.accentG & 0xFF) << 8)
            |  (cfg.accentB & 0xFF);

        int y = 2;
        for (int i = 0; i < active.size(); i++) {
            String label = active.get(i);
            int tw = font.width(label);
            int x  = viewW - tw - 5;             // 5px: 2 text pad + 1 tab + slack
            int color = rainbow ? hue(i) : accent;

            if (background) {
                g.fill(x - 3, y - 1, viewW, y + lineH - 1, bgAlpha << 24);
            }
            g.drawString(font, label, x, y, color, true);
            // Colour tab on the far-right edge.
            g.fill(viewW - 1, y - 1, viewW, y + lineH - 1, color);
            y += lineH;
        }
    }

    /** Astolfo-style per-row rainbow (hue shifts down the list and over time). */
    private static int hue(int row) {
        float speed = 4890f;
        float h = ((System.currentTimeMillis() % (long) speed) + row * 120f) / speed;
        return Color.HSBtoRGB(h % 1f, 0.55f, 1.0f) | 0xFF000000;
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
