package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class ClickGuiScreen extends Screen {

    // ── Categories ────────────────────────────────────────────────────────────
    private enum Category {
        COMBAT("Combat"),
        MOVEMENT("Movement"),
        VISUALS("Visuals"),
        SETTINGS("Settings");
        final String label;
        Category(String l) { label = l; }
    }

    // ── Settings ──────────────────────────────────────────────────────────────
    interface Setting {}
    record SliderS(String label, DoubleSupplier get, DoubleConsumer set, double min, double max) implements Setting {}
    record BoolS(String label, BooleanSupplier get, Runnable toggle) implements Setting {}
    record KeyS(String label, IntSupplier get, IntConsumer set) implements Setting {}

    // ── Module entry ──────────────────────────────────────────────────────────
    record ModEntry(
        String name, Category cat,
        BooleanSupplier enabled, Runnable toggle,
        boolean toggleable,
        List<Setting> settings
    ) {}

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int COL_W  = 160;
    private static final int COL_GAP = 6;
    private static final int COL_Y  = 30;
    private static final int HDR_H  = 22;
    private static final int ROW_H  = 20;
    private static final int SET_H  = 14;
    private static final int INDENT = 6;

    // ── State ─────────────────────────────────────────────────────────────────
    private final List<ModEntry> mods = new ArrayList<>();
    private final Set<String> expanded = new HashSet<>();
    private String rebindMod = null;
    private int    rebindIdx = -1;
    private String dragMod  = null;
    private int    dragIdx  = -1;

    // ── Waifu ─────────────────────────────────────────────────────────────────
    // (image rendering pending — place waifu.png in config/valencia/)
    private String waifuPath = "";

    // ─────────────────────────────────────────────────────────────────────────

    public ClickGuiScreen() {
        super(Component.empty());
        buildMods();
        loadWaifu();
    }

    @Override public boolean isPauseScreen() { return false; }

    // ── Module list ───────────────────────────────────────────────────────────
    private void buildMods() {
        ModConfig cfg = ModConfig.get();

        // Combat
        mods.add(new ModEntry("KillAura", Category.COMBAT,
            KillAuraMod::isEnabled, KillAuraMod::toggle, true,
            List.of(
                new SliderS("Range",     () -> cfg.killRange,              v -> { cfg.killRange        = (float)v; KillAuraMod.RANGE        = (float)v; cfg.save(); }, 1, 10),
                new SliderS("Atk Range", () -> cfg.killAttackRange,        v -> { cfg.killAttackRange  = (float)v; KillAuraMod.ATTACK_RANGE = (float)v; cfg.save(); }, 1, 8),
                new SliderS("Atk Delay", () -> cfg.killAttackDelay,        v -> { cfg.killAttackDelay  = (int)v;   KillAuraMod.attackDelay  = (int)v;   cfg.save(); }, 2, 20),
                new BoolS("Single",      () -> cfg.killSingle,             () -> { cfg.killSingle      = !cfg.killSingle;      KillAuraMod.singleMode    = cfg.killSingle;      cfg.save(); }),
                new BoolS("Hostile",     () -> cfg.killHostile,            () -> { cfg.killHostile     = !cfg.killHostile;     KillAuraMod.targetHostile = cfg.killHostile;     cfg.save(); }),
                new BoolS("Animals",     () -> cfg.killAnimals,            () -> { cfg.killAnimals     = !cfg.killAnimals;     KillAuraMod.targetAnimals = cfg.killAnimals;     cfg.save(); }),
                new BoolS("Players",     () -> cfg.killPlayers,            () -> { cfg.killPlayers     = !cfg.killPlayers;     KillAuraMod.targetPlayers = cfg.killPlayers;     cfg.save(); }),
                new KeyS("Key",          () -> cfg.killAuraKey,            v -> { cfg.killAuraKey = v; cfg.save(); })
            )));

        mods.add(new ModEntry("MaceAura", Category.COMBAT,
            MaceAuraMod::isEnabled, MaceAuraMod::toggle, true,
            List.of(
                new SliderS("Det Range", () -> cfg.maceDetectRange, v -> { cfg.maceDetectRange = (float)v; MaceAuraMod.RANGE        = (float)v; cfg.save(); }, 1, 12),
                new SliderS("Atk Range", () -> cfg.maceAttackRange, v -> { cfg.maceAttackRange = (float)v; MaceAuraMod.ATTACK_RANGE = (float)v; cfg.save(); }, 1, 8),
                new KeyS("Key",          () -> cfg.maceAuraKey,     v -> { cfg.maceAuraKey = v; cfg.save(); })
            )));

        // Movement
        mods.add(new ModEntry("BHop", Category.MOVEMENT,
            BHopMod::isEnabled, BHopMod::toggle, true,
            List.of(
                new SliderS("Speed", () -> (double)cfg.bhopSpeed, v -> { cfg.bhopSpeed = (float)v; BHopMod.speedMultiplier = (float)v; cfg.save(); }, 0.5, 2.5),
                new KeyS("Key",      () -> cfg.bhopKey,           v -> { cfg.bhopKey = v; cfg.save(); })
            )));

        mods.add(new ModEntry("NoFall", Category.MOVEMENT,
            NoFallMod::isEnabled, NoFallMod::toggleManual, true,
            List.of(new KeyS("Key", () -> cfg.nofallKey, v -> { cfg.nofallKey = v; cfg.save(); }))));

        mods.add(new ModEntry("NoSlow", Category.MOVEMENT,
            NoSlowMod::isEnabled, NoSlowMod::toggle, true,
            List.of(new KeyS("Key", () -> cfg.noSlowKey, v -> { cfg.noSlowKey = v; cfg.save(); }))));

        mods.add(new ModEntry("Step", Category.MOVEMENT,
            StepMod::isEnabled, StepMod::toggle, true,
            List.of(new KeyS("Key", () -> cfg.stepKey, v -> { cfg.stepKey = v; cfg.save(); }))));

        // Visuals
        mods.add(new ModEntry("XRay", Category.VISUALS,
            XRayMod::isEnabled, XRayMod::toggle, true,
            List.of(new KeyS("Key", () -> cfg.xrayKey, v -> { cfg.xrayKey = v; cfg.save(); }))));

        // Settings — not toggleable, always show sliders
        mods.add(new ModEntry("Theme Color", Category.SETTINGS,
            () -> false, () -> {}, false,
            List.of(
                new SliderS("Red",      () -> cfg.accentR, v -> { cfg.accentR = (int)v; cfg.save(); }, 0, 255),
                new SliderS("Green",    () -> cfg.accentG, v -> { cfg.accentG = (int)v; cfg.save(); }, 0, 255),
                new SliderS("Blue",     () -> cfg.accentB, v -> { cfg.accentB = (int)v; cfg.save(); }, 0, 255),
                new SliderS("BG Alpha", () -> cfg.bgAlpha, v -> { cfg.bgAlpha = (int)v; cfg.save(); }, 60, 240)
            )));

        mods.add(new ModEntry("GUI Key", Category.SETTINGS,
            () -> false, () -> {}, false,
            List.of(new KeyS("Key", () -> cfg.guiKey, v -> { cfg.guiKey = v; cfg.save(); }))));
    }

    // ── Waifu ─────────────────────────────────────────────────────────────────
    private void loadWaifu() {
        // Image rendering not yet supported in this build
    }

    // ── Render ────────────────────────────────────────────────────────────────
    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        ModConfig cfg = ModConfig.get();
        int accent = accent(cfg, 255);

        // Full-screen overlay
        g.fill(0, 0, width, height, argb(cfg.bgAlpha, 0, 0, 0));

        // Columns
        int totalW = Category.values().length * COL_W + (Category.values().length - 1) * COL_GAP;
        int startX = (width - totalW) / 2;

        for (Category cat : Category.values()) {
            int cx = startX + cat.ordinal() * (COL_W + COL_GAP);
            renderColumn(g, cat, cx, mx, my, cfg, accent);
        }

        // Waifu
        renderWaifu(g);

        // Hint
        g.drawString(font, "§7Right Ctrl to close", 4, height - 10, 0xFFAAAAAA, false);
    }

    private void renderColumn(GuiGraphics g, Category cat, int cx, int mx, int my, ModConfig cfg, int accent) {
        // Measure column height
        int colH = HDR_H;
        for (ModEntry m : mods) {
            if (m.cat() != cat) continue;
            colH += ROW_H;
            if (expanded.contains(m.name())) colH += m.settings().size() * SET_H + 2;
        }

        // Column background
        g.fill(cx, COL_Y, cx + COL_W, COL_Y + colH, argb(180, 10, 10, 10));

        // Header
        g.fill(cx, COL_Y, cx + COL_W, COL_Y + HDR_H, accent);
        g.drawCenteredString(font, cat.label, cx + COL_W / 2, COL_Y + (HDR_H - 8) / 2, 0xFFFFFFFF);

        int y = COL_Y + HDR_H;

        for (ModEntry m : mods) {
            if (m.cat() != cat) continue;
            y = renderRow(g, m, cx, y, mx, my, cfg, accent);
        }
    }

    private int renderRow(GuiGraphics g, ModEntry m, int cx, int y, int mx, int my, ModConfig cfg, int accent) {
        boolean on   = m.toggleable() && m.enabled().getAsBoolean();
        boolean hov  = mx >= cx && mx < cx + COL_W && my >= y && my < y + ROW_H;
        boolean exp  = expanded.contains(m.name());

        // Row background
        int bg = on  ? accent(cfg, 100)
               : hov ? argb(50, 255, 255, 255)
               :        argb(20, 255, 255, 255);
        g.fill(cx, y, cx + COL_W, y + ROW_H, bg);

        // Left enabled bar
        if (on) g.fill(cx, y, cx + 3, y + ROW_H, accent);

        // Module name
        int textCol = on ? 0xFFFFFFFF : 0xFFAAAAAA;
        g.drawString(font, m.name(), cx + INDENT + (on ? 3 : 0), y + (ROW_H - 8) / 2, textCol, false);

        // Arrow if has settings
        if (!m.settings().isEmpty()) {
            String arr = exp ? "v" : ">";
            g.drawString(font, arr, cx + COL_W - 12, y + (ROW_H - 8) / 2, 0xFF666666, false);
        }

        y += ROW_H;

        // Expanded settings
        if (exp) {
            for (int si = 0; si < m.settings().size(); si++) {
                Setting s = m.settings().get(si);
                g.fill(cx, y, cx + COL_W, y + SET_H, argb(120, 5, 5, 5));

                if (s instanceof SliderS sl)   renderSlider(g, sl, cx, y, cfg);
                else if (s instanceof BoolS bs) renderBool(g, bs, cx, y);
                else if (s instanceof KeyS ks)  renderKey(g, ks, m, si, cx, y);

                y += SET_H;
            }
            // Bottom separator
            g.fill(cx, y, cx + COL_W, y + 2, accent(cfg, 80));
            y += 2;
        }

        return y;
    }

    private void renderSlider(GuiGraphics g, SliderS sl, int cx, int y, ModConfig cfg) {
        int accent = accent(cfg, 255);
        double val = sl.get().getAsDouble();
        double pct = Math.max(0, Math.min(1, (val - sl.min()) / (sl.max() - sl.min())));

        int tx  = cx + INDENT;
        int tw  = COL_W - INDENT * 2 - 28;
        int ty  = y + SET_H / 2;

        g.drawString(font, sl.label(), tx, y + 1, 0xFF888888, false);

        // Track
        g.fill(tx, ty, tx + tw, ty + 2, argb(80, 255, 255, 255));
        // Fill
        g.fill(tx, ty, tx + (int)(tw * pct), ty + 2, accent);
        // Handle
        int hx = tx + (int)(tw * pct);
        g.fill(hx - 2, y + 2, hx + 2, y + SET_H - 2, 0xFFFFFFFF);

        // Value
        String valStr = (sl.max() <= 20 && sl.min() >= 0 && (int)sl.min() == sl.min() && (int)sl.max() == sl.max())
                      ? String.valueOf((int)Math.round(val))
                      : String.format("%.1f", val);
        g.drawString(font, valStr, cx + COL_W - 26, y + 1, 0xFFCCCCCC, false);
    }

    private void renderBool(GuiGraphics g, BoolS bs, int cx, int y) {
        boolean on = bs.get().getAsBoolean();
        g.drawString(font, bs.label(), cx + INDENT, y + 1, 0xFF888888, false);
        String tag = on ? "§aON" : "§cOFF";
        g.drawString(font, tag, cx + COL_W - 28, y + 1, 0xFFFFFFFF, false);
    }

    private void renderKey(GuiGraphics g, KeyS ks, ModEntry m, int si, int cx, int y) {
        boolean waiting = m.name().equals(rebindMod) && rebindIdx == si;
        g.drawString(font, ks.label(), cx + INDENT, y + 1, 0xFF888888, false);
        String keyTxt = waiting ? "§e..." : "§7[" + shortKey(ks.get().getAsInt()) + "]";
        g.drawString(font, keyTxt, cx + COL_W - 40, y + 1, 0xFFCCCCCC, false);
    }

    // ── Waifu rendering ───────────────────────────────────────────────────────
    private void renderWaifu(GuiGraphics g) {
        // Placeholder: show path hint at bottom-left
        g.drawString(font, "§8[waifu: config/valencia/waifu.png]", 4, height - 20, 0xFF555555, false);
    }

    // ── Mouse ─────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean dbl) {
        int mx = (int)event.x(), my = (int)event.y(), btn = event.button();
        if (btn != 0) return super.mouseClicked(event, dbl);

        ModConfig cfg = ModConfig.get();
        int totalW = Category.values().length * COL_W + (Category.values().length - 1) * COL_GAP;
        int startX = (width - totalW) / 2;

        for (Category cat : Category.values()) {
            int cx = startX + cat.ordinal() * (COL_W + COL_GAP);
            int y  = COL_Y + HDR_H;

            for (ModEntry m : mods) {
                if (m.cat() != cat) continue;

                // Module row
                if (mx >= cx && mx < cx + COL_W && my >= y && my < y + ROW_H) {
                    boolean hasSettings = !m.settings().isEmpty();
                    boolean clickArrow  = hasSettings && mx >= cx + COL_W - 16;

                    if (clickArrow || !m.toggleable()) {
                        // Expand/collapse
                        if (expanded.contains(m.name())) expanded.remove(m.name());
                        else expanded.add(m.name());
                    } else {
                        // Toggle module
                        m.toggle().run();
                        saveEnabled(cfg);
                    }
                    return true;
                }
                y += ROW_H;

                // Settings rows
                if (expanded.contains(m.name())) {
                    for (int si = 0; si < m.settings().size(); si++) {
                        if (my >= y && my < y + SET_H && mx >= cx && mx < cx + COL_W) {
                            Setting s = m.settings().get(si);
                            if (s instanceof SliderS sl) {
                                int tx = cx + INDENT;
                                int tw = COL_W - INDENT * 2 - 28;
                                dragMod = m.name(); dragIdx = si;
                                applySlider(sl, mx, tx, tw);
                            } else if (s instanceof BoolS bs) {
                                bs.toggle().run();
                            } else if (s instanceof KeyS) {
                                rebindMod = m.name(); rebindIdx = si;
                            }
                            return true;
                        }
                        y += SET_H;
                    }
                    y += 2; // separator
                }
            }
        }
        return super.mouseClicked(event, dbl);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragMod != null) {
            int mx = (int)event.x();
            int totalW = Category.values().length * COL_W + (Category.values().length - 1) * COL_GAP;
            int startX = (width - totalW) / 2;

            for (ModEntry m : mods) {
                if (!m.name().equals(dragMod)) continue;
                int cx = startX + m.cat().ordinal() * (COL_W + COL_GAP);
                if (dragIdx >= 0 && dragIdx < m.settings().size()) {
                    Setting s = m.settings().get(dragIdx);
                    if (s instanceof SliderS sl) {
                        int tx = cx + INDENT, tw = COL_W - INDENT * 2 - 28;
                        applySlider(sl, mx, tx, tw);
                    }
                }
            }
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragMod = null; dragIdx = -1;
        return super.mouseReleased(event);
    }

    // ── Keyboard ──────────────────────────────────────────────────────────────
    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (rebindMod != null) {
            if (key != GLFW.GLFW_KEY_ESCAPE) {
                for (ModEntry m : mods) {
                    if (!m.name().equals(rebindMod)) continue;
                    if (rebindIdx >= 0 && rebindIdx < m.settings().size()) {
                        Setting s = m.settings().get(rebindIdx);
                        if (s instanceof KeyS ks) ks.set().accept(key);
                    }
                }
            }
            rebindMod = null; rebindIdx = -1;
            return true;
        }
        if (key == ModConfig.get().guiKey) { onClose(); return true; }
        return super.keyPressed(event);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void applySlider(SliderS sl, int mx, int tx, int tw) {
        double pct = Math.max(0, Math.min(1, (double)(mx - tx) / tw));
        double raw = sl.min() + pct * (sl.max() - sl.min());
        // Snap to int if both bounds are integers
        boolean intRange = (int)sl.min() == sl.min() && (int)sl.max() == sl.max() && sl.max() - sl.min() <= 30;
        double val = intRange ? Math.round(raw) : Math.round(raw * 10.0) / 10.0;
        sl.set().accept(val);
    }

    private static int accent(ModConfig cfg, int alpha) {
        return argb(alpha, cfg.accentR, cfg.accentG, cfg.accentB);
    }

    private static int argb(int a, int r, int g, int b) {
        return (Math.min(255, Math.max(0, a)) << 24)
             | (Math.min(255, Math.max(0, r)) << 16)
             | (Math.min(255, Math.max(0, g)) << 8)
             |  Math.min(255, Math.max(0, b));
    }

    private static void saveEnabled(ModConfig cfg) {
        cfg.nofallEnabled   = NoFallMod.isEnabled();
        cfg.xrayEnabled     = XRayMod.isEnabled();
        cfg.maceAuraEnabled = MaceAuraMod.isEnabled();
        cfg.noSlowEnabled   = NoSlowMod.isEnabled();
        cfg.bhopEnabled     = BHopMod.isEnabled();
        cfg.stepEnabled     = StepMod.isEnabled();
        cfg.killAuraEnabled = KillAuraMod.isEnabled();
        cfg.save();
    }

    private static String shortKey(int key) {
        String s = ModConfig.keyName(key);
        return switch (s) {
            case "RIGHT_CONTROL" -> "RCTRL";
            case "LEFT_CONTROL"  -> "LCTRL";
            case "RIGHT_SHIFT"   -> "RSHIFT";
            case "LEFT_SHIFT"    -> "LSHIFT";
            default -> s.length() > 6 ? s.substring(0, 6) : s;
        };
    }
}
