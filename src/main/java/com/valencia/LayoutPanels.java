package com.valencia;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * Panels layout (guiLayout == 0, default) — Raven B++ style: draggable category
 * panels, left-click toggle, right-click expand settings, gradient enabled rows,
 * half-scale labels. Owns the per-panel positions (laid out lazily on first
 * render) and the slider-drag state.
 */
final class LayoutPanels implements GuiLayout {
    private static final int HDR       = 15;   // category header height
    private static final int MOD_H     = 20;   // module row height (Raven=20)
    private static final int MAX_SET_H = 260;  // max visible settings height before scroll
    private static final int PANEL_W   = 92;   // panel width (Raven=92)

    private final ClickGuiScreen gui;

    private boolean layoutDone = false;
    private Panel sliderPanel;
    private int   sliderIdx = -1;

    LayoutPanels(ClickGuiScreen gui) { this.gui = gui; }

    /** Lay out panels left-to-right, wrapping by screen width. Runs once. */
    private void ensureLayout() {
        if (layoutDone) return;
        int gap = 5;
        int xOff = gap, yOff = 5;
        for (Panel p : gui.panels) {
            p.x = xOff;
            p.y = yOff;
            xOff += PANEL_W + gap;
            if (xOff + PANEL_W > gui.screenW()) {
                xOff = gap;
                yOff += 120;
            }
        }
        layoutDone = true;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, int accent, Font font) {
        ensureLayout();
        for (Panel p : gui.panels) renderPanel(g, p, mx, my, accent, font);
    }

    private void renderPanel(GuiGraphics g, Panel p, int mx, int my, int accent, Font font) {
        GuiSkin skin = gui.skin;
        int ph = panelH(p);
        int x1 = p.x, y1 = p.y, x2 = p.x + PANEL_W, y2 = p.y + ph;

        // Liquid skin ignores the user accent and drives everything sky-blue.
        int acc = skin.liquid ? (0xFF000000 | Aurora.SKY) : accent;

        // ── panel background ──
        if (skin.liquid) {
            Aurora.glassPanel(g, x1, y1, x2, y2);                       // translucent glass + specular
            g.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0x12000000 | Aurora.SKY);  // faint sky wash
        } else {
            g.fill(x1, y1, x2, y2, skin.panelBg);
        }

        // ── header ──
        boolean hoverHdr = mx >= x1 && mx < x2 && my >= y1 && my < y1 + HDR;
        g.fill(x1, y1, x2, y1 + HDR, hoverHdr ? skin.headerHover : skin.headerBg);

        g.drawString(font, p.cat.label, x1 + 3, y1 + (HDR - font.lineHeight) / 2 + 1, skin.catLabel, skin.liquid);

        String sym = p.open ? "-" : "+";
        int symColor = p.open ? 0xFF55FF55 : 0xFFFF5555;
        g.drawString(font, sym, x2 - font.width(sym) - 3, y1 + (HDR - font.lineHeight) / 2 + 1, symColor, false);

        g.fill(x1, y1 + HDR - 1, x2, y1 + HDR, skin.headerUnderline);

        if (!p.open) {
            if (skin.liquid) Aurora.glassBorder(g, x1, y1, x2, y2, Aurora.SKY);
            return;
        }

        if (p.expanded != null)
            drawExpanded(g, p, mx, my, font, acc);
        else
            drawModList(g, p, mx, my, font, acc);

        if (skin.liquid) {
            Aurora.sheen(g, x1, y1, x2, y2);
            Aurora.glassBorder(g, x1, y1, x2, y2, Aurora.SKY);
        } else {
            boolean hoverPanel = mx >= x1 && mx < x2 && my >= y1 && my < y2;
            int borderColor = hoverPanel ? (accent & 0x00FFFFFF) | 0x80000000 : skin.borderIdle;
            GuiDraw.drawBorder(g, x1, y1, x2, y2, borderColor);
        }
    }

    private void drawModList(GuiGraphics g, Panel p, int mx, int my, Font font, int accent) {
        GuiSkin skin = gui.skin;
        int yo = p.y + HDR;
        for (ModEntry m : p.mods) {
            boolean hover = mx >= p.x && mx < p.x + PANEL_W && my >= yo && my < yo + MOD_H;
            boolean on = m.enabled.getAsBoolean();

            if (on) {
                drawEnabledBg(g, p.x, yo, p.x + PANEL_W, yo + MOD_H, accent);
            } else if (hover) {
                g.fill(p.x, yo, p.x + PANEL_W, yo + MOD_H, skin.rowHover);
            }

            int tc = on ? skin.textOn : (m.toggleable ? skin.textDim : skin.textOff);
            int tw = font.width(m.name);
            g.drawString(font, m.name, p.x + (PANEL_W - tw) / 2, yo + (MOD_H - font.lineHeight) / 2, tc, skin.nameShadow);

            yo += MOD_H;
        }
    }

    private void drawExpanded(GuiGraphics g, Panel p, int mx, int my, Font font, int accent) {
        GuiSkin skin = gui.skin;
        ModEntry m = p.expanded;
        int yo = p.y + HDR;

        boolean on = m.enabled.getAsBoolean();
        if (on) {
            drawEnabledBg(g, p.x, yo, p.x + PANEL_W, yo + MOD_H, accent);
        } else {
            g.fill(p.x, yo, p.x + PANEL_W, yo + MOD_H, skin.expandedOffBg);
        }
        g.drawString(font, "« " + m.name, p.x + 3, yo + (MOD_H - font.lineHeight) / 2, on ? skin.textOn : skin.textDim, skin.nameShadow);
        yo += MOD_H;

        int settStart = yo;
        int totalSettH = GuiDraw.ravenTotalH(m);
        int visH = Math.min(totalSettH, MAX_SET_H);
        g.fill(p.x, settStart, p.x + PANEL_W, settStart + visH, skin.settingsBg);

        int maxScroll = Math.max(0, totalSettH - MAX_SET_H);
        if (p.scrollOff > maxScroll) p.scrollOff = maxScroll;
        if (p.scrollOff < 0) p.scrollOff = 0;

        int sx = p.x + 4;
        int sw = PANEL_W - 8;
        int drawY = settStart - p.scrollOff;
        for (int i = 0; i < m.settings.size(); i++) {
            Setting s = m.settings.get(i);
            int sh = GuiDraw.ravenSettH(s);

            if (drawY + sh > settStart && drawY < settStart + visH) {
                if (s instanceof SliderS sl) {
                    GuiDraw.drawSlider(g, sl, sx, drawY, sw, font, accent, skin);
                } else if (s instanceof BoolS bs) {
                    GuiDraw.drawBool(g, bs, sx, drawY, font, skin);
                } else if (s instanceof KeyS ks) {
                    GuiDraw.drawBind(g, ks, sx, drawY, font, gui.rebindTarget == ks, skin);
                }
            }
            drawY += sh;
        }

        if (totalSettH > MAX_SET_H) {
            float scrollPct = (float) p.scrollOff / maxScroll;
            int barH = Math.max(8, visH * visH / totalSettH);
            int barY = settStart + (int)((visH - barH) * scrollPct);
            g.fill(p.x + PANEL_W - 2, barY, p.x + PANEL_W, barY + barH, skin.scrollBar);
        }
    }

    /** Approximate vertical gradient via two halves (cheap, no GL shading). */
    private void drawGradientV(GuiGraphics g, int x1, int y1, int x2, int y2, int topC, int botC) {
        int midY = (y1 + y2) / 2;
        g.fill(x1, y1, x2, midY, topC);
        g.fill(x1, midY, x2, y2, botC);
    }

    /** Enabled-row background: accent vertical gradient (Dark/Glass) or flat tint (Light). */
    private void drawEnabledBg(GuiGraphics g, int x1, int y1, int x2, int y2, int accent) {
        GuiSkin skin = gui.skin;
        if (skin.enabledRowGradient) {
            int topC = (accent & 0x00FFFFFF) | 0xB0000000;
            int botC = (accent & 0x00FFFFFF) | 0x60000000;
            drawGradientV(g, x1, y1, x2, y2, topC, botC);
        } else {
            g.fill(x1, y1, x2, y2, (accent & 0x00FFFFFF) | (skin.enabledFlatAlpha << 24));
        }
    }

    private int panelH(Panel p) {
        if (!p.open) return HDR;
        if (p.expanded != null) {
            int totalS = GuiDraw.ravenTotalH(p.expanded);
            return HDR + MOD_H + Math.min(totalS, MAX_SET_H);
        }
        return HDR + p.mods.size() * MOD_H;
    }

    @Override
    public boolean mouseClicked(int mx, int my, int btn) {
        ensureLayout();
        List<Panel> panels = gui.panels;
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel p = panels.get(i);
            int ph = panelH(p);
            if (mx < p.x || mx >= p.x + PANEL_W || my < p.y || my >= p.y + ph) continue;

            // bring to front
            panels.remove(i);
            panels.add(p);

            // ── header click ──
            if (my < p.y + HDR) {
                int symX = p.x + PANEL_W - 12;
                if (btn == 0 && mx >= symX) {
                    p.open = !p.open;
                    p.expanded = null;
                    p.scrollOff = 0;
                } else if (btn == 0) {
                    p.dragging = true;
                    p.dragOX = p.x - mx;
                    p.dragOY = p.y - my;
                }
                return true;
            }

            if (!p.open) return true;

            // ── expanded settings view ──
            if (p.expanded != null) {
                int yo = p.y + HDR;
                if (my >= yo && my < yo + MOD_H) {
                    if (btn == 0 && p.expanded.toggleable) p.expanded.toggle.run();
                    if (btn == 1) { p.expanded = null; p.scrollOff = 0; }
                    return true;
                }
                yo += MOD_H;

                int settStart = yo;
                int drawY = settStart - p.scrollOff;
                for (int si = 0; si < p.expanded.settings.size(); si++) {
                    Setting s = p.expanded.settings.get(si);
                    int sh = GuiDraw.ravenSettH(s);
                    int realY = drawY;

                    if (my >= Math.max(realY, settStart) && my < Math.min(realY + sh, settStart + MAX_SET_H)) {
                        if (btn == 0) handleSettClick(p, si, s, mx, realY);
                        return true;
                    }
                    drawY += sh;
                }
                return true;
            }

            // ── module list (left=toggle, right=expand settings) ──
            int yo = p.y + HDR;
            for (ModEntry m : p.mods) {
                if (my >= yo && my < yo + MOD_H) {
                    if (btn == 0 && m.toggleable) m.toggle.run();
                    if (btn == 1 && !m.settings.isEmpty()) {
                        p.expanded = (p.expanded == m) ? null : m;
                        p.scrollOff = 0;
                    }
                    return true;
                }
                yo += MOD_H;
            }
            return true;
        }
        return false;
    }

    private void handleSettClick(Panel p, int idx, Setting s, int mx, int settingY) {
        if (s instanceof SliderS sl) {
            sliderPanel = p; sliderIdx = idx;
            GuiDraw.applySlider(sl, mx, p.x + 4, PANEL_W - 8);
        } else if (s instanceof BoolS bs) {
            bs.toggle().run();
        } else if (s instanceof KeyS ks) {
            gui.rebindTarget = ks;
        }
    }

    @Override
    public boolean mouseDragged(int mx, int my) {
        for (Panel p : gui.panels) {
            if (p.dragging) {
                p.x = p.dragOX + mx;
                p.y = p.dragOY + my;
                return true;
            }
        }
        if (sliderPanel != null && sliderPanel.expanded != null && sliderIdx >= 0) {
            Setting s = sliderPanel.expanded.settings.get(sliderIdx);
            if (s instanceof SliderS sl) GuiDraw.applySlider(sl, mx, sliderPanel.x + 4, PANEL_W - 8);
            return true;
        }
        return false;
    }

    @Override
    public void mouseReleased() {
        for (Panel p : gui.panels) p.dragging = false;
        sliderPanel = null; sliderIdx = -1;
    }

    @Override
    public boolean mouseScrolled(int mx, int my, double scrollY) {
        for (int i = gui.panels.size() - 1; i >= 0; i--) {
            Panel p = gui.panels.get(i);
            int ph = panelH(p);
            if (mx >= p.x && mx < p.x + PANEL_W && my >= p.y && my < p.y + ph) {
                if (p.open && p.expanded != null) {
                    int totalS = GuiDraw.ravenTotalH(p.expanded);
                    if (totalS > MAX_SET_H) {
                        p.scrollOff -= (int)(scrollY * 6);
                        int maxScroll = totalS - MAX_SET_H;
                        if (p.scrollOff < 0) p.scrollOff = 0;
                        if (p.scrollOff > maxScroll) p.scrollOff = maxScroll;
                    }
                }
                return true;
            }
        }
        return false;
    }
}
