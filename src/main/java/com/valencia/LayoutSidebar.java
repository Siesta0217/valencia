package com.valencia;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * Sidebar layout (guiLayout == 1) — single centered window: a category column,
 * a module list, and a settings pane that reuses the Raven half-scale widgets.
 * Geometry/hit-testing are self-contained; colours come from the active GuiSkin.
 */
final class LayoutSidebar implements GuiLayout {
    private static final int SB_W     = 300;
    private static final int SB_H     = 196;
    private static final int SB_TITLE = 16;
    private static final int SB_SIDE  = 58;    // category column width
    private static final int SB_LIST  = 100;   // module column width
    private static final int SB_CAT_H = 18;
    private static final int SB_ROW_H = 14;

    private final ClickGuiScreen gui;

    private int sbWinX = -1, sbWinY = -1;   // window top-left; -1 = center on first render
    private boolean sbDragging;
    private int sbDragOX, sbDragOY;
    private int sbCatIdx;                    // selected category (index into panels)
    private ModEntry sbExpanded;             // module shown in the settings pane (null = none)
    private int sbScrollMods, sbScrollSett;  // scroll offsets for the two scrollable columns
    private SliderS sbSliderActive;          // slider being dragged
    private int sbSliderX, sbSliderW;

    LayoutSidebar(ClickGuiScreen gui) { this.gui = gui; }

    private void ensureSidebarPos() {
        if (sbWinX < 0) { sbWinX = (gui.screenW() - SB_W) / 2; sbWinY = (gui.screenH() - SB_H) / 2; }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, int accent, Font font) {
        ensureSidebarPos();
        GuiSkin skin = gui.skin;
        List<Panel> panels = gui.panels;
        int x = sbWinX, y = sbWinY, x2 = x + SB_W, y2 = y + SB_H;
        int aTint = accent & 0x00FFFFFF;

        g.fill(x, y, x2, y2, skin.panelBg);

        // ── title bar (drag handle + close) ──
        boolean hoverTitle = mx >= x && mx < x2 - 14 && my >= y && my < y + SB_TITLE;
        g.fill(x, y, x2, y + SB_TITLE, hoverTitle ? skin.headerHover : skin.headerBg);
        g.drawString(font, "Valencia", x + 6, y + (SB_TITLE - font.lineHeight) / 2 + 1, skin.catLabel, skin.nameShadow);
        boolean hoverClose = mx >= x2 - 14 && mx < x2 && my >= y && my < y + SB_TITLE;
        g.drawString(font, "x", x2 - 9, y + (SB_TITLE - font.lineHeight) / 2 + 1, hoverClose ? 0xFFFF5555 : skin.textDim, false);
        g.fill(x, y + SB_TITLE - 1, x2, y + SB_TITLE, skin.headerUnderline);

        int contentY = y + SB_TITLE;
        int sideX2 = x + SB_SIDE, listX1 = sideX2 + 1, listX2 = listX1 + SB_LIST;
        g.fill(sideX2, contentY, sideX2 + 1, y2, skin.borderIdle);
        g.fill(listX2, contentY, listX2 + 1, y2, skin.borderIdle);

        // ── left: category tabs ──
        int cy = contentY + 2;
        for (int i = 0; i < panels.size(); i++) {
            Cat c = panels.get(i).cat;
            boolean sel = i == sbCatIdx;
            boolean hov = mx >= x && mx < sideX2 && my >= cy && my < cy + SB_CAT_H;
            if (sel)      g.fill(x, cy, sideX2, cy + SB_CAT_H, aTint | 0x50000000);
            else if (hov) g.fill(x, cy, sideX2, cy + SB_CAT_H, skin.rowHover);
            if (sel)      g.fill(x, cy, x + 2, cy + SB_CAT_H, accent);
            g.drawString(font, c.label, x + 6, cy + (SB_CAT_H - font.lineHeight) / 2, sel ? skin.textOn : skin.textDim, skin.nameShadow);
            cy += SB_CAT_H;
        }

        // ── middle: module list for the selected category ──
        Panel cat = panels.get(sbCatIdx);
        int listTop = contentY + 2, listBot = y2 - 2, listVis = listBot - listTop;
        int totalList = cat.mods.size() * SB_ROW_H;
        int maxScrollM = Math.max(0, totalList - listVis);
        sbScrollMods = Math.max(0, Math.min(sbScrollMods, maxScrollM));
        int ry = listTop - sbScrollMods;
        for (ModEntry m : cat.mods) {
            if (ry + SB_ROW_H > listTop && ry < listBot) {
                boolean on = m.enabled.getAsBoolean();
                boolean sel = m == sbExpanded;
                boolean hov = mx >= listX1 && mx < listX2 && my >= Math.max(ry, listTop) && my < Math.min(ry + SB_ROW_H, listBot);
                if (sel)      g.fill(listX1, ry, listX2, ry + SB_ROW_H, aTint | 0x40000000);
                else if (hov) g.fill(listX1, ry, listX2, ry + SB_ROW_H, skin.rowHover);
                int dotY = ry + (SB_ROW_H - 6) / 2;
                g.fill(listX1 + 4, dotY, listX1 + 10, dotY + 6, on ? 0xFF55FF55 : 0xFF606060);
                int tc = m.toggleable ? (on ? skin.textOn : skin.textDim) : skin.textOff;
                g.drawString(font, m.name, listX1 + 14, ry + (SB_ROW_H - font.lineHeight) / 2, tc, skin.nameShadow);
                if (!m.settings.isEmpty())
                    g.drawString(font, "›", listX2 - 8, ry + (SB_ROW_H - font.lineHeight) / 2, skin.textDim, false);
            }
            ry += SB_ROW_H;
        }
        if (totalList > listVis) {
            int barH = Math.max(8, listVis * listVis / totalList);
            int barY = listTop + (int)((listVis - barH) * ((float) sbScrollMods / maxScrollM));
            g.fill(listX2 - 2, barY, listX2, barY + barH, skin.scrollBar);
        }

        // ── right: settings for the selected module (reuses Raven widgets) ──
        int setX = listX2 + 5, setRight = x2 - 5, setW = setRight - setX;
        int setTop = contentY + 4, setBot = y2 - 3;
        if (sbExpanded == null) {
            g.drawString(font, "Pick a module ‹", listX2 + 6, contentY + 6, skin.textOff, false);
        } else {
            ModEntry m = sbExpanded;
            boolean on = m.enabled.getAsBoolean();
            g.drawString(font, m.name, setX, setTop, on ? skin.textOn : skin.textDim, skin.nameShadow);
            if (m.toggleable) {
                String st = on ? "ON" : "OFF";
                g.drawString(font, st, setRight - font.width(st), setTop, on ? 0xFF55FF55 : 0xFFFF5555, false);
            }
            int sListTop = setTop + 12;
            int totalSet = GuiDraw.ravenTotalH(m), setVis = setBot - sListTop;
            int maxScrollS = Math.max(0, totalSet - setVis);
            sbScrollSett = Math.max(0, Math.min(sbScrollSett, maxScrollS));
            int dy = sListTop - sbScrollSett;
            for (Setting s : m.settings) {
                int sh = GuiDraw.ravenSettH(s);
                if (dy + sh > sListTop && dy < setBot) {
                    if (s instanceof SliderS sl)    GuiDraw.drawSlider(g, sl, setX, dy, setW, font, accent, skin);
                    else if (s instanceof BoolS bs) GuiDraw.drawBool(g, bs, setX, dy, font, skin);
                    else if (s instanceof KeyS ks)  GuiDraw.drawBind(g, ks, setX, dy, font, gui.rebindTarget == ks, skin);
                }
                dy += sh;
            }
            if (totalSet > setVis) {
                int barH = Math.max(8, setVis * setVis / totalSet);
                int barY = sListTop + (int)((setVis - barH) * ((float) sbScrollSett / maxScrollS));
                g.fill(x2 - 2, barY, x2, barY + barH, skin.scrollBar);
            }
        }

        boolean hoverWin = mx >= x && mx < x2 && my >= y && my < y2;
        GuiDraw.drawBorder(g, x, y, x2, y2, hoverWin ? (aTint | 0x80000000) : skin.borderIdle);
    }

    @Override
    public boolean mouseClicked(int mx, int my, int btn) {
        ensureSidebarPos();
        List<Panel> panels = gui.panels;
        int x = sbWinX, y = sbWinY, x2 = x + SB_W, y2 = y + SB_H;
        if (mx < x || mx >= x2 || my < y || my >= y2) return false;

        if (my < y + SB_TITLE) {                       // title bar
            if (btn == 0 && mx >= x2 - 14) { gui.onClose(); return true; }
            if (btn == 0) { sbDragging = true; sbDragOX = x - mx; sbDragOY = y - my; }
            return true;
        }

        int contentY = y + SB_TITLE;
        int sideX2 = x + SB_SIDE, listX1 = sideX2 + 1, listX2 = listX1 + SB_LIST;

        if (mx < sideX2) {                             // category tabs
            int cy = contentY + 2;
            for (int i = 0; i < panels.size(); i++) {
                if (my >= cy && my < cy + SB_CAT_H) { sbCatIdx = i; sbExpanded = null; sbScrollMods = 0; sbScrollSett = 0; break; }
                cy += SB_CAT_H;
            }
            return true;
        }

        if (mx < listX2) {                             // module list
            int listTop = contentY + 2, listBot = y2 - 2;
            int ry = listTop - sbScrollMods;
            for (ModEntry m : panels.get(sbCatIdx).mods) {
                if (my >= Math.max(ry, listTop) && my < Math.min(ry + SB_ROW_H, listBot)) {
                    if (mx < listX1 + 12) { if (m.toggleable) m.toggle.run(); }   // dot → toggle
                    else { sbExpanded = (sbExpanded == m) ? null : m; sbScrollSett = 0; }  // row → select
                    break;
                }
                ry += SB_ROW_H;
            }
            return true;
        }

        if (sbExpanded != null) {                      // settings pane
            ModEntry m = sbExpanded;
            int setX = listX2 + 5, setRight = x2 - 5, setW = setRight - setX;
            int setTop = contentY + 4, setBot = y2 - 3;
            if (my >= setTop && my < setTop + 10) {
                if (btn == 0 && m.toggleable && mx >= setRight - 24) m.toggle.run();
                return true;
            }
            int sListTop = setTop + 12;
            int dy = sListTop - sbScrollSett;
            for (Setting s : m.settings) {
                int sh = GuiDraw.ravenSettH(s);
                if (my >= Math.max(dy, sListTop) && my < Math.min(dy + sh, setBot)) {
                    if (btn == 0) sbHandleSettClick(s, mx, setX, setW);
                    break;
                }
                dy += sh;
            }
        }
        return true;
    }

    private void sbHandleSettClick(Setting s, int mx, int setX, int setW) {
        if (s instanceof SliderS sl) { sbSliderActive = sl; sbSliderX = setX; sbSliderW = setW; GuiDraw.applySlider(sl, mx, setX, setW); }
        else if (s instanceof BoolS bs) bs.toggle().run();
        else if (s instanceof KeyS ks) gui.rebindTarget = ks;
    }

    @Override
    public boolean mouseDragged(int mx, int my) {
        if (sbDragging) { sbWinX = sbDragOX + mx; sbWinY = sbDragOY + my; return true; }
        if (sbSliderActive != null) { GuiDraw.applySlider(sbSliderActive, mx, sbSliderX, sbSliderW); return true; }
        return false;
    }

    @Override
    public void mouseReleased() { sbDragging = false; sbSliderActive = null; }

    @Override
    public boolean mouseScrolled(int mx, int my, double scrollY) {
        ensureSidebarPos();
        int x = sbWinX, y = sbWinY, x2 = x + SB_W, y2 = y + SB_H;
        if (mx < x || mx >= x2 || my < y || my >= y2) return false;
        int sideX2 = x + SB_SIDE, listX1 = sideX2 + 1, listX2 = listX1 + SB_LIST;
        if (mx >= listX1 && mx < listX2) { sbScrollMods -= (int)(scrollY * 8); return true; }
        if (mx >= listX2 && sbExpanded != null) { sbScrollSett -= (int)(scrollY * 8); return true; }
        return true;
    }
}
