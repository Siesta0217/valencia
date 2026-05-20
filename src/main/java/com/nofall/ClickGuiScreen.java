package com.nofall;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

public class ClickGuiScreen extends Screen {

    // Panel position (draggable)
    private int px = 30, py = 40;
    private boolean dragging;
    private int dragDx, dragDy;

    // Layout
    private static final int W        = 158;
    private static final int HEADER_H = 22;
    private static final int ROW_H    = 20;
    private static final int ROW_GAP  = 1;
    private static final int PAD      = 5;

    // Raven-inspired palette — pure ARGB constants, no GL state needed
    private static final int C_SHADOW    = 0x66000000;
    private static final int C_BG        = 0xEE0C0C1C;
    private static final int C_HDR_TOP   = 0xFF200855;
    private static final int C_HDR_BOT   = 0xFF0D0330;
    private static final int C_BORDER    = 0xFF7020CC;
    private static final int C_DIVIDER   = 0xFF5010AA;
    private static final int C_ROW_ALT   = 0x14FFFFFF;
    private static final int C_BAR_ON    = 0xFF00C060;
    private static final int C_BAR_OFF   = 0xFF992020;
    private static final int C_TEXT      = 0xFFEEEEEE;
    private static final int C_ON_TXT    = 0xFF44FF88;
    private static final int C_OFF_TXT   = 0xFFFF5555;
    private static final int C_BADGE_ON  = 0xFF0D3320;
    private static final int C_BADGE_OFF = 0xFF3D1010;

    record Mod(String name, BooleanSupplier enabled, Runnable toggle) {}

    private final Mod[] MODS = {
        new Mod("NoFall",   NoFallMod::isEnabled,   NoFallMod::toggleManual),
        new Mod("XRay",     XRayMod::isEnabled,     XRayMod::toggle),
        new Mod("MaceAura", MaceAuraMod::isEnabled, MaceAuraMod::toggle),
        new Mod("NoSlow",   NoSlowMod::isEnabled,   NoSlowMod::toggle),
    };

    // Per-row hover animation (0.0 → 1.0)
    private final float[] hoverT = new float[MODS.length];

    public ClickGuiScreen() { super(Component.empty()); }

    @Override public boolean isPauseScreen() { return false; }

    // ── Layout ────────────────────────────────────────────────────────────────

    private int panelH() {
        return HEADER_H + MODS.length * (ROW_H + ROW_GAP) + PAD;
    }

    private int rowTop(int i) {
        return py + HEADER_H + i * (ROW_H + ROW_GAP);
    }

    // ── Rendering ─────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        int ph = panelH();

        // Drop shadow
        g.fill(px + 4, py + 4, px + W + 4, py + ph + 4, C_SHADOW);

        // Panel background
        g.fill(px, py, px + W, py + ph, C_BG);

        // 1-px purple border
        border(g, px, py, W, ph, C_BORDER);

        // Header gradient (two fills simulate top→bottom gradient)
        g.fill(px + 1, py + 1,            px + W - 1, py + HEADER_H / 2, C_HDR_TOP);
        g.fill(px + 1, py + HEADER_H / 2, px + W - 1, py + HEADER_H,     C_HDR_BOT);

        // Divider below header
        g.fill(px + 1, py + HEADER_H, px + W - 1, py + HEADER_H + 1, C_DIVIDER);

        // Animated rainbow title (Raven's signature effect)
        drawRainbow(g, "Valencia", px + PAD, py + 7, System.currentTimeMillis());

        // Module rows
        for (int i = 0; i < MODS.length; i++) renderRow(g, i, mx, my);

        super.render(g, mx, my, delta);
    }

    private void renderRow(GuiGraphics g, int i, int mx, int my) {
        Mod m  = MODS[i];
        int ry = rowTop(i);
        boolean on  = m.enabled().getAsBoolean();
        boolean hov = mx >= px && mx < px + W && my >= ry && my < ry + ROW_H;

        // Smooth hover animation (frame-rate dependent, acceptable for UI effects)
        hoverT[i] += ((hov ? 1f : 0f) - hoverT[i]) * 0.2f;

        // Alternating row tint
        if (i % 2 == 0) g.fill(px + 1, ry, px + W - 1, ry + ROW_H, C_ROW_ALT);

        // Hover overlay
        if (hoverT[i] > 0.004f) {
            int a = (int) (hoverT[i] * 0x38);
            g.fill(px + 1, ry, px + W - 1, ry + ROW_H, (a << 24) | 0x00FFFFFF);
        }

        // Left state bar (3 px wide) — Raven-style colour indicator
        g.fill(px + 1, ry, px + 4, ry + ROW_H, on ? C_BAR_ON : C_BAR_OFF);

        // Module name
        g.drawString(font, m.name(), px + PAD + 4, ry + 6, C_TEXT, false);

        // ON / OFF badge (right side)
        String badge = on ? "ON" : "OFF";
        int bw = font.width(badge) + 8;
        int bh = font.lineHeight + 4;
        int bx = px + W - bw - 4;
        int by = ry + (ROW_H - bh) / 2;
        g.fill(bx, by, bx + bw, by + bh, on ? C_BADGE_ON : C_BADGE_OFF);
        border(g, bx, by, bw, bh, on ? C_BAR_ON : C_BAR_OFF);
        g.drawString(font, badge, bx + 4, by + 2, on ? C_ON_TXT : C_OFF_TXT, false);
    }

    // ── Draw helpers ──────────────────────────────────────────────────────────

    private void border(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x,         y,         x + w,     y + 1,     c);
        g.fill(x,         y + h - 1, x + w,     y + h,     c);
        g.fill(x,         y,         x + 1,     y + h,     c);
        g.fill(x + w - 1, y,         x + w,     y + h,     c);
    }

    private void drawRainbow(GuiGraphics g, String text, int x, int y, long t) {
        int cx = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            if (ch.equals(" ")) { cx += font.width(" "); continue; }
            g.drawString(font, ch, cx, y, rainbow(t, i * 120), false);
            cx += font.width(ch);
        }
    }

    private static int rainbow(long t, int offset) {
        float hue = ((t + offset) % 4000) / 4000f;
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.7f, 1.0f);
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }

    // ── Input — 1.21.11 event-based API ───────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        int imx = (int) event.x();
        int imy = (int) event.y();
        int btn = event.button();

        // Header drag
        if (btn == 0 && imx >= px && imx < px + W && imy >= py && imy < py + HEADER_H) {
            dragging = true;
            dragDx = imx - px;
            dragDy = imy - py;
            return true;
        }

        // Module toggle
        for (int i = 0; i < MODS.length; i++) {
            int ry = rowTop(i);
            if (imx >= px && imx < px + W && imy >= ry && imy < ry + ROW_H) {
                MODS[i].toggle().run();
                return true;
            }
        }

        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) dragging = false;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging && event.button() == 0) {
            px = (int) event.x() - dragDx;
            py = (int) event.y() - dragDy;
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // guiKey closes this screen; ESC is handled by super
        if (event.key() == ModConfig.get().guiKey) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }
}
