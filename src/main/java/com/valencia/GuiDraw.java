package com.valencia;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Stateless ClickGUI drawing kit shared by every layout. Pure helpers (no
 * per-layout state) extracted verbatim from ClickGuiScreen: border/rounded-rect
 * primitives, value formatting, slider math, the Raven half-scale widgets, and
 * per-setting row heights. GuiSkin is passed in so the skin-aware widgets stay
 * decoupled from any single layout.
 */
final class GuiDraw {
    private GuiDraw() {}

    // Raven setting-row heights
    static final int S_SLIDER = 16;
    static final int S_BOOL   = 14;
    static final int S_BIND   = 14;

    // Tenacity / Aurora card setting-row heights
    static final int CARD_SLIDER = 22;
    static final int CARD_BOOL   = 16;
    static final int CARD_BIND   = 16;

    static String fmtVal(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) return String.format("%.0f", v);
        return String.format("%.1f", v);
    }

    static void applySlider(SliderS sl, int mx, int barX, int barW) {
        double pct = Math.max(0, Math.min(1, (double)(mx - barX) / barW));
        double val = sl.min() + pct * (sl.max() - sl.min());
        sl.set().accept(val);
    }

    // ── Border / rounded-rect primitives ─────────────────────────────────────
    static void drawBorder(GuiGraphics g, int x1, int y1, int x2, int y2, int c) {
        g.fill(x1, y1, x2, y1 + 1, c);      // top
        g.fill(x1, y2 - 1, x2, y2, c);      // bottom
        g.fill(x1, y1, x1 + 1, y2, c);      // left
        g.fill(x2 - 1, y1, x2, y2, c);      // right
    }

    /** Tenacity rounded-rect (2px corner shave). */
    static void roundRect(GuiGraphics g, int x1, int y1, int x2, int y2, int c) {
        if (x2 - x1 < 4 || y2 - y1 < 4) { g.fill(x1, y1, x2, y2, c); return; }
        g.fill(x1 + 2, y1,     x2 - 2, y1 + 1, c);
        g.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, c);
        g.fill(x1,     y1 + 2, x2,     y2 - 2, c);
        g.fill(x1 + 1, y2 - 2, x2 - 1, y2 - 1, c);
        g.fill(x1 + 2, y2 - 1, x2 - 2, y2,     c);
    }

    static void roundBorder(GuiGraphics g, int x1, int y1, int x2, int y2, int c) {
        g.fill(x1 + 2, y1,     x2 - 2, y1 + 1, c);
        g.fill(x1 + 2, y2 - 1, x2 - 2, y2,     c);
        g.fill(x1,     y1 + 2, x1 + 1, y2 - 2, c);
        g.fill(x2 - 1, y1 + 2, x2,     y2 - 2, c);
        g.fill(x1 + 1, y1 + 1, x1 + 2, y1 + 2, c);
        g.fill(x2 - 2, y1 + 1, x2 - 1, y1 + 2, c);
        g.fill(x1 + 1, y2 - 2, x1 + 2, y2 - 1, c);
        g.fill(x2 - 2, y2 - 2, x2 - 1, y2 - 1, c);
    }

    // ── Per-setting row heights ──────────────────────────────────────────────
    static int ravenSettH(Setting s) {
        if (s instanceof SliderS) return S_SLIDER;
        if (s instanceof BoolS)   return S_BOOL;
        if (s instanceof KeyS)    return S_BIND;
        return 0;
    }

    static int ravenTotalH(ModEntry m) {
        int h = 0;
        for (Setting s : m.settings) h += ravenSettH(s);
        return h;
    }

    static int cardSettH(Setting s) {
        if (s instanceof SliderS) return CARD_SLIDER;
        if (s instanceof BoolS)   return CARD_BOOL;
        if (s instanceof KeyS)    return CARD_BIND;
        return 0;
    }

    // ── Raven half-scale widgets (skin-aware) ────────────────────────────────
    static void drawSlider(GuiGraphics g, SliderS sl, int x, int y, int w, Font font, int accent, GuiSkin skin) {
        double val = sl.get().getAsDouble();
        String txt = sl.label() + ": " + fmtVal(val);

        g.pose().pushMatrix();
        g.pose().scale(0.5f, 0.5f);
        g.drawString(font, txt, x * 2, y * 2, skin.textOn, false);
        g.pose().popMatrix();

        int barY = y + 6;
        int barH = S_SLIDER - 7;
        double pct = Math.max(0, Math.min(1, (val - sl.min()) / (sl.max() - sl.min())));
        int filled = (int)(w * pct);

        g.fill(x, barY, x + w, barY + barH, skin.sliderTrack);
        drawBorder(g, x, barY, x + w, barY + barH, skin.sliderTrackBorder);

        if (filled > 0) {
            int fc = (skin.sliderFillAlpha << 24) | (accent & 0x00FFFFFF);
            g.fill(x, barY, x + filled, barY + barH, fc);
        }
    }

    static void drawBool(GuiGraphics g, BoolS bs, int x, int y, Font font, GuiSkin skin) {
        boolean on = bs.get().getAsBoolean();

        int bw = 14, bh = 7;
        int by = y + (S_BOOL - bh) / 2;

        g.fill(x, by, x + bw, by + bh, skin.boolTrack);
        drawBorder(g, x, by, x + bw, by + bh, skin.widgetBorder);

        int indW = bw / 2;
        int indX = on ? x + bw - indW : x;
        int indColor = on ? 0xFF55FF55 : 0xFFFF5555;
        g.fill(indX, by, indX + indW, by + bh, indColor);

        g.pose().pushMatrix();
        g.pose().scale(0.5f, 0.5f);
        g.drawString(font, bs.label(), (x + bw + 3) * 2, (y + S_BOOL / 2) * 2, skin.textOn, false);
        g.pose().popMatrix();
    }

    static void drawBind(GuiGraphics g, KeyS ks, int x, int y, Font font, boolean binding, GuiSkin skin) {
        String txt = binding ? "§ePress a key..." : "Bind: §f" + ModConfig.keyName(ks.get().getAsInt());
        g.pose().pushMatrix();
        g.pose().scale(0.5f, 0.5f);
        g.drawString(font, txt, x * 2, (y + S_BIND / 2) * 2, skin.textOn, false);
        g.pose().popMatrix();
    }
}
