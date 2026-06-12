package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Aurora Glass layout (guiLayout == 3) — iOS liquid-glass single window: a
 * translucent panel with a specular highlight and a sweeping light band, plus a
 * flowing aurora gradient through the title bar, tab underline, enabled rows,
 * pills and slider fills. The aurora primitives live in the shared {@link Aurora}
 * kit; this layout is self-contained otherwise.
 */
final class LayoutAurora implements GuiLayout {
    private static final int AU_W = 280, AU_H = 250;
    private static final int AU_TITLE = 22;
    private static final int AU_TABS  = 18;
    private static final int AU_CARD_H = 22;
    private static final int AU_BOOL = 16, AU_BIND = 16;

    private final ClickGuiScreen gui;

    private int auWinX = -1, auWinY = -1;    // window top-left; -1 = center on first render
    private boolean auDragging;
    private int auDragOX, auDragOY;
    private int auCatIdx;                     // selected category tab
    private ModEntry auExpanded;             // inline-expanded module card (null = none)
    private int auScroll;                     // card-list scroll offset
    private SliderS auSliderActive;          // slider being dragged
    private int auSliderX, auSliderW;
    private final Map<String, Float> knob = new HashMap<>();  // pill knob slide anim

    LayoutAurora(ClickGuiScreen gui) { this.gui = gui; }

    // Thin wrappers over the shared Aurora kit keep call sites short.
    private void auroraFill(GuiGraphics g, int x1, int y1, int x2, int y2, int alpha, float span) {
        Aurora.fill(g, x1, y1, x2, y2, alpha, span);
    }
    private void auGlassPanel(GuiGraphics g, int x1, int y1, int x2, int y2) { Aurora.glassPanel(g, x1, y1, x2, y2); }
    private void auSheen(GuiGraphics g, int x1, int y1, int x2, int y2)      { Aurora.sheen(g, x1, y1, x2, y2); }
    private void auBorder(GuiGraphics g, int x1, int y1, int x2, int y2)     { Aurora.border(g, x1, y1, x2, y2); }

    private void ensureAuPos() {
        if (auWinX < 0) { auWinX = (gui.screenW() - AU_W) / 2; auWinY = (gui.screenH() - AU_H) / 2; }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, int accent, Font font) {
        ensureAuPos();
        List<Panel> panels = gui.panels;
        int x = auWinX, y = auWinY, x2 = x + AU_W, y2 = y + AU_H;

        auGlassPanel(g, x, y, x2, y2);

        // ── title bar: dark glass with white branding; the aurora gradient
        // lives in a thin underline instead of a full bright bar (which read
        // as cheap RGB lighting and fought every other element for attention).
        g.drawString(font, "Valencia", x + 8, y + (AU_TITLE - font.lineHeight) / 2 + 1, 0xFFFFFFFF, true);
        boolean hovClose = mx >= x2 - 16 && mx < x2 && my >= y && my < y + AU_TITLE;
        g.drawString(font, "x", x2 - 11, y + (AU_TITLE - font.lineHeight) / 2 + 1, hovClose ? 0xFFFF6B6B : 0xFF8B90A0, false);
        auroraFill(g, x + 6, y + AU_TITLE - 1, x2 - 6, y + AU_TITLE, 0xE6, 0.5f);

        // ── tabs with aurora underline on the active one ──
        int tabY = y + AU_TITLE + 2;
        int tx = x + 8;
        for (int i = 0; i < panels.size(); i++) {
            String lbl = panels.get(i).cat.label;
            int tw = font.width(lbl);
            boolean sel = i == auCatIdx;
            boolean hov = mx >= tx - 2 && mx < tx + tw + 2 && my >= tabY && my < tabY + AU_TABS;
            g.drawString(font, lbl, tx, tabY + (AU_TABS - font.lineHeight) / 2, sel ? 0xFFFFFFFF : (hov ? 0xFFD0D4E0 : 0xFF8B90A0), sel);
            if (sel) auroraFill(g, tx - 1, tabY + AU_TABS - 2, tx + tw + 1, tabY + AU_TABS - 1, 0xFF, 0.25f);
            tx += tw + 10;
        }

        // ── module cards (scrollable, clipped) ──
        int listX1 = x + 6, listX2 = x2 - 6, listW = listX2 - listX1;
        int listTop = tabY + AU_TABS + 2, listBot = y2 - 6, listVis = listBot - listTop;
        int total = auContentH(panels.get(auCatIdx));
        int maxScroll = Math.max(0, total - listVis);
        auScroll = Math.max(0, Math.min(auScroll, maxScroll));

        g.enableScissor(listX1, listTop, listX2, listBot);
        int ry = listTop - auScroll;
        for (ModEntry m : panels.get(auCatIdx).mods) {
            int ch = AU_CARD_H + (m == auExpanded ? auExpandedH(m) : 0);
            if (ry + ch > listTop && ry < listBot) auDrawCard(g, m, listX1, ry, listW, mx, my, font);
            ry += ch + 3;
        }
        g.disableScissor();

        if (total > listVis) {
            int barH = Math.max(10, listVis * listVis / total);
            int barY = listTop + (int)((listVis - barH) * ((float) auScroll / maxScroll));
            g.fill(listX2 - 1, barY, listX2 + 1, barY + barH, 0x66FFFFFF);
        }

        auSheen(g, x, y, x2, y2);
        auBorder(g, x, y, x2, y2);
    }

    private void auDrawCard(GuiGraphics g, ModEntry m, int x, int y, int w, int mx, int my, Font font) {
        int ch = AU_CARD_H + (m == auExpanded ? auExpandedH(m) : 0);
        boolean on = m.enabled.getAsBoolean();
        boolean hovRow = mx >= x && mx < x + w && my >= y && my < y + AU_CARD_H;

        GuiDraw.roundRect(g, x, y, x + w, y + ch, hovRow ? 0x2EFFFFFF : 0x1AFFFFFF);
        if (on) {
            // Subtle wash — the enabled state is carried by the left bar, the
            // pill and the white text; a louder wash turned the list into
            // wall-to-wall gradient.
            auroraFill(g, x + 1, y + 1, x + w - 1, y + AU_CARD_H - 1, 0x26, 0.35f);
            auroraFill(g, x, y + 3, x + 2, y + AU_CARD_H - 3, 0xFF, 0.1f);
        }
        if (m == auExpanded) g.fill(x + 1, y + AU_CARD_H, x + w - 1, y + ch - 1, 0x28000000);

        g.drawString(font, m.name, x + 8, y + (AU_CARD_H - font.lineHeight) / 2, on ? 0xFFFFFFFF : 0xFFC2C6D2, on);

        int pillW = 20, pillX = x + w - 7 - pillW;
        if (m.toggleable) auPill(g, pillX, y + AU_CARD_H / 2, on, knobProg("au:" + m.name, on));
        if (!m.settings.isEmpty()) {
            String chev = m == auExpanded ? "-" : "+";
            int chx = m.toggleable ? pillX - 9 : x + w - 12;
            g.drawString(font, chev, chx, y + (AU_CARD_H - font.lineHeight) / 2, 0xFF9BA0B0, false);
        }

        if (m == auExpanded) {
            int sy = y + AU_CARD_H + 2, sx = x + 10, sw = w - 18;
            for (Setting s : m.settings) {
                if (s instanceof SliderS sl)    auSlider(g, sl, sx, sy + 2, sw, font);
                else if (s instanceof BoolS bs) auBool(g, bs, sx, sy, sw, font, m);
                else if (s instanceof KeyS ks)  auBind(g, ks, sx, sy + 2, sw, font, gui.rebindTarget == ks);
                sy += GuiDraw.cardSettH(s);
            }
        }
    }

    private void auPill(GuiGraphics g, int x, int cy, boolean on, float prog) {
        int w = 20, h = 11, y = cy - h / 2;
        GuiDraw.roundRect(g, x, y, x + w, y + h, on ? 0xFF14181F : 0xFF262A34);
        if (on) auroraFill(g, x + 1, y + 1, x + w - 1, y + h - 1, 0xE6, 0.4f);
        int kd = h - 4;
        int kx = x + 2 + (int)((w - 4 - kd) * prog);
        GuiDraw.roundRect(g, kx, y + 2, kx + kd, y + 2 + kd, 0xFFFFFFFF);
    }

    private void auSlider(GuiGraphics g, SliderS sl, int x, int y, int w, Font font) {
        double val = sl.get().getAsDouble();
        g.drawString(font, sl.label(), x, y, 0xFFC2C6D2, false);
        String vs = GuiDraw.fmtVal(val);
        g.drawString(font, vs, x + w - font.width(vs), y, 0xFFFFFFFF, false);
        int barY = y + 12, barH = 4;
        double pct = Math.max(0, Math.min(1, (val - sl.min()) / (sl.max() - sl.min())));
        int filled = (int)(w * pct);
        GuiDraw.roundRect(g, x, barY, x + w, barY + barH, 0xFF202531);
        if (filled > 0) auroraFill(g, x, barY, x + filled, barY + barH, 0xFF, 0.35f);
        int tx = x + filled, tr = 4, tcy = barY + barH / 2;
        GuiDraw.roundRect(g, tx - tr, tcy - tr, tx + tr, tcy + tr, 0xFFFFFFFF);
    }

    private void auBool(GuiGraphics g, BoolS bs, int x, int y, int w, Font font, ModEntry owner) {
        boolean on = bs.get().getAsBoolean();
        g.drawString(font, bs.label(), x, y + (AU_BOOL - font.lineHeight) / 2, 0xFFC2C6D2, false);
        auPill(g, x + w - 20, y + AU_BOOL / 2, on, knobProg("au:" + owner.name + ":" + bs.label(), on));
    }

    private void auBind(GuiGraphics g, KeyS ks, int x, int y, int w, Font font, boolean binding) {
        g.drawString(font, "Key", x, y + (AU_BIND - font.lineHeight) / 2, 0xFFC2C6D2, false);
        String v = binding ? "..." : ModConfig.keyName(ks.get().getAsInt());
        int cw = font.width(v) + 8, cx = x + w - cw, ccy = y + AU_BIND / 2;
        GuiDraw.roundRect(g, cx, ccy - 6, x + w, ccy + 6, 0x33FFFFFF);
        g.drawString(font, v, cx + 4, ccy - font.lineHeight / 2, binding ? 0xFFFFD050 : 0xFFFFFFFF, false);
    }

    private float knobProg(String key, boolean on) {
        float target = on ? 1f : 0f;
        float cur = knob.getOrDefault(key, target);
        cur += (target - cur) * 0.3f;
        if (Math.abs(target - cur) < 0.01f) cur = target;
        knob.put(key, cur);
        return cur;
    }

    private int auExpandedH(ModEntry m) {
        int h = 4;
        for (Setting s : m.settings) h += GuiDraw.cardSettH(s);
        return h;
    }

    private int auContentH(Panel p) {
        int h = 0;
        for (ModEntry m : p.mods) h += AU_CARD_H + (m == auExpanded ? auExpandedH(m) : 0) + 3;
        return h;
    }

    @Override
    public boolean mouseClicked(int mx, int my, int btn) {
        ensureAuPos();
        Font font = Minecraft.getInstance().font;
        List<Panel> panels = gui.panels;
        int x = auWinX, y = auWinY, x2 = x + AU_W, y2 = y + AU_H;
        if (mx < x || mx >= x2 || my < y || my >= y2) return false;

        if (my < y + AU_TITLE) {                        // title bar
            if (btn == 0 && mx >= x2 - 16) { gui.onClose(); return true; }
            if (btn == 0) { auDragging = true; auDragOX = x - mx; auDragOY = y - my; }
            return true;
        }

        int tabY = y + AU_TITLE + 2;
        if (my >= tabY && my < tabY + AU_TABS) {        // tabs
            int tx = x + 8;
            for (int i = 0; i < panels.size(); i++) {
                int tw = font.width(panels.get(i).cat.label);
                if (mx >= tx - 2 && mx < tx + tw + 2) { auCatIdx = i; auExpanded = null; auScroll = 0; break; }
                tx += tw + 10;
            }
            return true;
        }

        int listX1 = x + 6, listX2 = x2 - 6, listW = listX2 - listX1;
        int listTop = tabY + AU_TABS + 2, listBot = y2 - 6;
        if (mx < listX1 || mx >= listX2 || my < listTop || my >= listBot) return true;

        int ry = listTop - auScroll;
        for (ModEntry m : panels.get(auCatIdx).mods) {
            int ch = AU_CARD_H + (m == auExpanded ? auExpandedH(m) : 0);
            if (my >= ry && my < ry + AU_CARD_H) {      // header row
                int pillW = 20, pillX = listX1 + listW - 7 - pillW;
                if (btn == 0 && m.toggleable && mx >= pillX - 2 && mx < pillX + pillW + 2) m.toggle.run();
                else if (!m.settings.isEmpty()) auExpanded = (m == auExpanded) ? null : m;
                else if (btn == 0 && m.toggleable) m.toggle.run();
                return true;
            }
            if (m == auExpanded && my >= ry + AU_CARD_H && my < ry + ch) {   // settings
                int sy = ry + AU_CARD_H + 2, sx = listX1 + 10, sw = listW - 18;
                for (Setting s : m.settings) {
                    int sh = GuiDraw.cardSettH(s);
                    if (my >= sy && my < sy + sh) {
                        if (btn == 0) {
                            if (s instanceof SliderS sl) { auSliderActive = sl; auSliderX = sx; auSliderW = sw; GuiDraw.applySlider(sl, mx, sx, sw); }
                            else if (s instanceof BoolS bs) bs.toggle().run();
                            else if (s instanceof KeyS ks) gui.rebindTarget = ks;
                        }
                        break;
                    }
                    sy += sh;
                }
                return true;
            }
            ry += ch + 3;
        }
        return true;
    }

    @Override
    public boolean mouseDragged(int mx, int my) {
        if (auDragging) { auWinX = auDragOX + mx; auWinY = auDragOY + my; return true; }
        if (auSliderActive != null) { GuiDraw.applySlider(auSliderActive, mx, auSliderX, auSliderW); return true; }
        return false;
    }

    @Override
    public void mouseReleased() { auDragging = false; auSliderActive = null; }

    @Override
    public boolean mouseScrolled(int mx, int my, double scrollY) {
        ensureAuPos();
        List<Panel> panels = gui.panels;
        int x = auWinX, y = auWinY, x2 = x + AU_W, y2 = y + AU_H;
        if (mx < x || mx >= x2 || my < y || my >= y2) return false;
        int listTop = y + AU_TITLE + 2 + AU_TABS + 2;
        int listVis = (y2 - 6) - listTop;
        int maxScroll = Math.max(0, auContentH(panels.get(auCatIdx)) - listVis);
        auScroll = Math.max(0, Math.min(auScroll - (int)(scrollY * 12), maxScroll));
        return true;
    }
}
