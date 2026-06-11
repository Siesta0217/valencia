package com.valencia;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tenacity layout (guiLayout == 2) — rounded window: category rail on the left,
 * rounded module cards with sliding-pill toggles and inline accordion settings.
 * Custom widgets with a knob-slide animation; colours come from the active skin
 * (best with Dark / Tenacity).
 */
final class LayoutTenacity implements GuiLayout {
    private static final int TEN_W = 344, TEN_H = 234;
    private static final int TEN_TITLE = 24;
    private static final int TEN_RAIL  = 84;
    private static final int TEN_CAT_H = 22;
    private static final int TEN_CARD_H = 22;
    private static final int TEN_BOOL = 16, TEN_BIND = 16;

    private final ClickGuiScreen gui;

    private int tnWinX = -1, tnWinY = -1;    // window top-left; -1 = center on first render
    private boolean tnDragging;
    private int tnDragOX, tnDragOY;
    private int tnCatIdx;                     // selected category (index into panels)
    private ModEntry tnExpanded;             // inline-expanded module card (null = none)
    private int tnScroll;                     // card-list scroll offset
    private SliderS tnSliderActive;          // slider being dragged
    private int tnSliderX, tnSliderW;
    private final Map<String, Float> knob = new HashMap<>();  // pill knob slide anim

    LayoutTenacity(ClickGuiScreen gui) { this.gui = gui; }

    private void ensureTenPos() {
        if (tnWinX < 0) { tnWinX = (gui.screenW() - TEN_W) / 2; tnWinY = (gui.screenH() - TEN_H) / 2; }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, int accent, Font font) {
        ensureTenPos();
        GuiSkin skin = gui.skin;
        List<Panel> panels = gui.panels;
        int x = tnWinX, y = tnWinY, x2 = x + TEN_W, y2 = y + TEN_H;
        int aT = accent & 0x00FFFFFF;

        GuiDraw.roundRect(g, x, y, x2, y2, skin.panelBg);

        // ── title bar ──
        g.drawString(font, "Valencia", x + 10, y + (TEN_TITLE - font.lineHeight) / 2, skin.catLabel, skin.nameShadow);
        boolean hovClose = mx >= x2 - 16 && mx < x2 && my >= y && my < y + TEN_TITLE;
        g.drawString(font, "x", x2 - 12, y + (TEN_TITLE - font.lineHeight) / 2, hovClose ? 0xFFFF5555 : skin.textDim, false);
        g.fill(x + 10, y + TEN_TITLE - 1, x2 - 10, y + TEN_TITLE, accent);

        int contentY = y + TEN_TITLE;
        int railX2 = x + TEN_RAIL;
        g.fill(railX2, contentY + 2, railX2 + 1, y2 - 4, skin.borderIdle);   // rail divider

        // ── left rail: category tabs ──
        int cy = contentY + 6;
        for (int i = 0; i < panels.size(); i++) {
            Cat c = panels.get(i).cat;
            boolean sel = i == tnCatIdx;
            boolean hov = mx >= x + 6 && mx < railX2 - 4 && my >= cy && my < cy + TEN_CAT_H;
            if (sel)      GuiDraw.roundRect(g, x + 6, cy, railX2 - 4, cy + TEN_CAT_H, aT | 0x40000000);
            else if (hov) GuiDraw.roundRect(g, x + 6, cy, railX2 - 4, cy + TEN_CAT_H, skin.rowHover);
            if (sel) g.fill(x + 6, cy + 4, x + 8, cy + TEN_CAT_H - 4, accent);
            g.drawString(font, c.label, x + 14, cy + (TEN_CAT_H - font.lineHeight) / 2, sel ? skin.textOn : skin.textDim, skin.nameShadow);
            cy += TEN_CAT_H + 2;
        }

        // ── right: module cards (scrollable, clipped) ──
        int listX1 = railX2 + 6, listX2 = x2 - 6, listW = listX2 - listX1;
        int listTop = contentY + 6, listBot = y2 - 6, listVis = listBot - listTop;
        int total = tnContentH(panels.get(tnCatIdx));
        int maxScroll = Math.max(0, total - listVis);
        tnScroll = Math.max(0, Math.min(tnScroll, maxScroll));

        g.enableScissor(listX1, listTop, listX2, listBot);
        int ry = listTop - tnScroll;
        for (ModEntry m : panels.get(tnCatIdx).mods) {
            int ch = TEN_CARD_H + (m == tnExpanded ? tnExpandedH(m) : 0);
            if (ry + ch > listTop && ry < listBot) tnDrawCard(g, m, listX1, ry, listW, mx, my, font, accent);
            ry += ch + 4;
        }
        g.disableScissor();

        if (total > listVis) {
            int barH = Math.max(10, listVis * listVis / total);
            int barY = listTop + (int)((listVis - barH) * ((float) tnScroll / maxScroll));
            GuiDraw.roundRect(g, listX2 - 2, barY, listX2, barY + barH, skin.scrollBar);
        }

        GuiDraw.roundBorder(g, x, y, x2, y2, skin.borderIdle);
    }

    private void tnDrawCard(GuiGraphics g, ModEntry m, int x, int y, int w, int mx, int my, Font font, int accent) {
        GuiSkin skin = gui.skin;
        int ch = TEN_CARD_H + (m == tnExpanded ? tnExpandedH(m) : 0);
        boolean on = m.enabled.getAsBoolean();
        boolean hovRow = mx >= x && mx < x + w && my >= y && my < y + TEN_CARD_H;

        GuiDraw.roundRect(g, x, y, x + w, y + ch, hovRow ? skin.headerHover : skin.expandedOffBg);
        if (m == tnExpanded) g.fill(x + 1, y + TEN_CARD_H, x + w - 1, y + ch - 1, skin.settingsBg);
        if (on) g.fill(x, y + 3, x + 2, y + TEN_CARD_H - 3, accent);

        g.drawString(font, m.name, x + 8, y + (TEN_CARD_H - font.lineHeight) / 2, on ? skin.textOn : skin.textDim, skin.nameShadow);

        int pillW = 18, pillX = x + w - 8 - pillW, pillCy = y + TEN_CARD_H / 2;
        if (m.toggleable) tnPill(g, pillX, pillCy, on, knobProg(m.name, on), accent);
        if (!m.settings.isEmpty()) {
            String chev = m == tnExpanded ? "-" : "+";
            int chx = m.toggleable ? pillX - 10 : x + w - 12;
            g.drawString(font, chev, chx, y + (TEN_CARD_H - font.lineHeight) / 2, skin.textDim, false);
        }

        if (m == tnExpanded) {
            int sy = y + TEN_CARD_H + 2, sx = x + 12, sw = w - 22;
            for (Setting s : m.settings) {
                if (s instanceof SliderS sl)    tnSlider(g, sl, sx, sy + 2, sw, font, accent);
                else if (s instanceof BoolS bs) tnBool(g, bs, sx, sy, sw, font, accent);
                else if (s instanceof KeyS ks)  tnBind(g, ks, sx, sy + 2, sw, font, gui.rebindTarget == ks);
                sy += GuiDraw.cardSettH(s);
            }
        }
    }

    private void tnPill(GuiGraphics g, int x, int cy, boolean on, float prog, int accent) {
        GuiSkin skin = gui.skin;
        int w = 18, h = 10, y = cy - h / 2;
        GuiDraw.roundRect(g, x, y, x + w, y + h, on ? (accent & 0x00FFFFFF) | 0xCC000000 : skin.boolTrack);
        int kd = h - 4, kx = x + 2 + (int)((w - 4 - kd) * prog);
        GuiDraw.roundRect(g, kx, y + 2, kx + kd, y + 2 + kd, 0xFFFFFFFF);
    }

    private void tnSlider(GuiGraphics g, SliderS sl, int x, int y, int w, Font font, int accent) {
        GuiSkin skin = gui.skin;
        double val = sl.get().getAsDouble();
        g.drawString(font, sl.label(), x, y, skin.textDim, skin.nameShadow);
        String vs = GuiDraw.fmtVal(val);
        g.drawString(font, vs, x + w - font.width(vs), y, skin.textOn, skin.nameShadow);
        int barY = y + 12, barH = 4;
        double pct = Math.max(0, Math.min(1, (val - sl.min()) / (sl.max() - sl.min())));
        int filled = (int)(w * pct);
        GuiDraw.roundRect(g, x, barY, x + w, barY + barH, skin.sliderTrack);
        if (filled > 0) GuiDraw.roundRect(g, x, barY, x + filled, barY + barH, (accent & 0x00FFFFFF) | 0xFF000000);
        int tx = x + filled, tr = 4, tcy = barY + barH / 2;
        GuiDraw.roundRect(g, tx - tr, tcy - tr, tx + tr, tcy + tr, 0xFFFFFFFF);
    }

    private void tnBool(GuiGraphics g, BoolS bs, int x, int y, int w, Font font, int accent) {
        GuiSkin skin = gui.skin;
        boolean on = bs.get().getAsBoolean();
        g.drawString(font, bs.label(), x, y + (TEN_BOOL - font.lineHeight) / 2, skin.textDim, skin.nameShadow);
        int pillW = 18;
        tnPill(g, x + w - pillW, y + TEN_BOOL / 2, on, knobProg(m_key(bs), on), accent);
    }

    private void tnBind(GuiGraphics g, KeyS ks, int x, int y, int w, Font font, boolean binding) {
        GuiSkin skin = gui.skin;
        g.drawString(font, "Key", x, y + (TEN_BIND - font.lineHeight) / 2, skin.textDim, skin.nameShadow);
        String v = binding ? "..." : ModConfig.keyName(ks.get().getAsInt());
        int cw = font.width(v) + 8, cx = x + w - cw, ccy = y + TEN_BIND / 2;
        GuiDraw.roundRect(g, cx, ccy - 6, x + w, ccy + 6, skin.sliderTrack);
        g.drawString(font, v, cx + 4, ccy - font.lineHeight / 2, binding ? 0xFFFFD050 : skin.textOn, false);
    }

    private String m_key(BoolS bs) { return (tnExpanded != null ? tnExpanded.name : "") + ":" + bs.label(); }

    private float knobProg(String key, boolean on) {
        float target = on ? 1f : 0f;
        float cur = knob.getOrDefault(key, target);
        cur += (target - cur) * 0.3f;
        if (Math.abs(target - cur) < 0.01f) cur = target;
        knob.put(key, cur);
        return cur;
    }

    private int tnExpandedH(ModEntry m) {
        int h = 4;
        for (Setting s : m.settings) h += GuiDraw.cardSettH(s);
        return h;
    }

    private int tnContentH(Panel p) {
        int h = 0;
        for (ModEntry m : p.mods) h += TEN_CARD_H + (m == tnExpanded ? tnExpandedH(m) : 0) + 4;
        return h;
    }

    @Override
    public boolean mouseClicked(int mx, int my, int btn) {
        ensureTenPos();
        List<Panel> panels = gui.panels;
        int x = tnWinX, y = tnWinY, x2 = x + TEN_W, y2 = y + TEN_H;
        if (mx < x || mx >= x2 || my < y || my >= y2) return false;

        if (my < y + TEN_TITLE) {                       // title bar
            if (btn == 0 && mx >= x2 - 16) { gui.onClose(); return true; }
            if (btn == 0) { tnDragging = true; tnDragOX = x - mx; tnDragOY = y - my; }
            return true;
        }

        int contentY = y + TEN_TITLE, railX2 = x + TEN_RAIL;
        if (mx < railX2) {                              // rail tabs
            int cy = contentY + 6;
            for (int i = 0; i < panels.size(); i++) {
                if (my >= cy && my < cy + TEN_CAT_H) { tnCatIdx = i; tnExpanded = null; tnScroll = 0; break; }
                cy += TEN_CAT_H + 2;
            }
            return true;
        }

        int listX1 = railX2 + 6, listX2 = x2 - 6, listW = listX2 - listX1;
        int listTop = contentY + 6, listBot = y2 - 6;
        if (mx < listX1 || mx >= listX2 || my < listTop || my >= listBot) return true;

        int ry = listTop - tnScroll;
        for (ModEntry m : panels.get(tnCatIdx).mods) {
            int ch = TEN_CARD_H + (m == tnExpanded ? tnExpandedH(m) : 0);
            if (my >= ry && my < ry + TEN_CARD_H) {     // header row
                int pillW = 18, pillX = listX1 + listW - 8 - pillW;
                if (btn == 0 && m.toggleable && mx >= pillX - 2 && mx < pillX + pillW + 2) m.toggle.run();
                else if (!m.settings.isEmpty()) tnExpanded = (m == tnExpanded) ? null : m;
                else if (btn == 0 && m.toggleable) m.toggle.run();
                return true;
            }
            if (m == tnExpanded && my >= ry + TEN_CARD_H && my < ry + ch) {   // settings
                int sy = ry + TEN_CARD_H + 2, sx = listX1 + 12, sw = listW - 22;
                for (Setting s : m.settings) {
                    int sh = GuiDraw.cardSettH(s);
                    if (my >= sy && my < sy + sh) { if (btn == 0) tnSettClick(s, mx, sx, sw); break; }
                    sy += sh;
                }
                return true;
            }
            ry += ch + 4;
        }
        return true;
    }

    private void tnSettClick(Setting s, int mx, int sx, int sw) {
        if (s instanceof SliderS sl) { tnSliderActive = sl; tnSliderX = sx; tnSliderW = sw; GuiDraw.applySlider(sl, mx, sx, sw); }
        else if (s instanceof BoolS bs) bs.toggle().run();
        else if (s instanceof KeyS ks) gui.rebindTarget = ks;
    }

    @Override
    public boolean mouseDragged(int mx, int my) {
        if (tnDragging) { tnWinX = tnDragOX + mx; tnWinY = tnDragOY + my; return true; }
        if (tnSliderActive != null) { GuiDraw.applySlider(tnSliderActive, mx, tnSliderX, tnSliderW); return true; }
        return false;
    }

    @Override
    public void mouseReleased() { tnDragging = false; tnSliderActive = null; }

    @Override
    public boolean mouseScrolled(int mx, int my, double scrollY) {
        ensureTenPos();
        List<Panel> panels = gui.panels;
        int x = tnWinX, y = tnWinY, x2 = x + TEN_W, y2 = y + TEN_H;
        if (mx < x || mx >= x2 || my < y || my >= y2) return false;
        if (mx >= x + TEN_RAIL) {
            int listVis = (y2 - 6) - (y + TEN_TITLE + 6);
            int maxScroll = Math.max(0, tnContentH(panels.get(tnCatIdx)) - listVis);
            tnScroll = Math.max(0, Math.min(tnScroll - (int)(scrollY * 12), maxScroll));
        }
        return true;
    }
}
