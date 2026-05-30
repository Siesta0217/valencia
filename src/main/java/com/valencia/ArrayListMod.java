package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

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

    private record Entry(String label, BooleanSupplier on) {}

    // Modules worth surfacing. Pure-HUD readouts (TargetHUD, DimCoord) and the
    // ArrayList itself are intentionally excluded.
    private static final List<Entry> ENTRIES = List.of(
        new Entry("KillAura",   KillAuraMod::isEnabled),
        new Entry("MaceAura",   MaceAuraMod::isEnabled),
        new Entry("SpearAura",  SpearAuraMod::isEnabled),
        new Entry("CritHit",    CritMod::isEnabled),
        new Entry("Hitbox",     HitboxMod::isEnabled),
        new Entry("AutoTotem",  AutoTotemMod::isEnabled),
        new Entry("Scaffold",   ScaffoldMod::isEnabled),
        new Entry("BHop",       BHopMod::isEnabled),
        new Entry("Velocity",   VelocityMod::isEnabled),
        new Entry("Step",       StepMod::isEnabled),
        new Entry("Timer",      TimerMod::isEnabled),
        new Entry("FastPlace",  FastPlaceMod::isEnabled),
        new Entry("NoSlow",     NoSlowMod::isEnabled),
        new Entry("NoFall",     NoFallMod::isEnabled),
        new Entry("NoCrash",    NoCrashMod::isEnabled),
        new Entry("AutoFish",   AutoFishMod::isEnabled),
        new Entry("ElytraGoto", ElytraGotoMod::isEnabled),
        new Entry("XRay",       XRayMod::isEnabled),
        new Entry("ESP",        ESPMod::isEnabled),
        new Entry("NameTag",    NameTagMod::isEnabled)
    );

    private ArrayListMod() {}

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    public static void render(GuiGraphics g) {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        Font font = mc.font;
        ModConfig cfg = ModConfig.get();

        // Collect active labels, longest first → staircase.
        List<String> active = new ArrayList<>();
        for (Entry e : ENTRIES) if (e.on().getAsBoolean()) active.add(e.label());
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
