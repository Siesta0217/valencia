package com.valencia;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Glass layout (guiLayout == 4) — a ground-up rebuild of the scattered-panel
 * ("分列式") style, not a reskin of {@link LayoutPanels}. Same familiar feel
 * (one draggable panel per category, left-click toggle, right-click expand,
 * +/- collapse) but modern geometry: wider panels, full-size crisp labels,
 * rounded iOS liquid-glass panels (translucent + specular + sweeping sheen),
 * a sky-blue glow border, pill toggles, and thumb sliders.
 *
 * Fixed sky-blue palette (independent of the user accent), drawn via the shared
 * {@link Aurora} glass kit. Reuses the {@link Panel} model state (x/y/open/
 * expanded/scrollOff) so it lives happily alongside the other layouts.
 */
final class LayoutGlass implements GuiLayout {
    private static final int PANEL_W   = 118;
    private static final int HDR       = 18;   // header height
    private static final int ROW_H     = 16;   // module row height
    private static final int PAD       = 6;    // inner horizontal padding
    private static final int MAX_SET_H = 168;  // expanded-settings cap before scroll
    private static final int S_SLIDER  = 22, S_BOOL = 16, S_BIND = 16;

    private static final int SKY  = Aurora.SKY;
    private static final int SKYA = 0xFF000000 | SKY;

    private final ClickGuiScreen gui;

    private boolean layoutDone = false;
    private Panel sliderPanel;
    private int   sliderIdx = -1;
    private final Map<String, Float> knob = new HashMap<>();   // pill slide animation

    LayoutGlass(ClickGuiScreen gui) { this.gui = gui; }

    private void ensureLayout() {
        if (layoutDone) return;
        int gap = 6, xOff = gap, yOff = 6;
        for (Panel p : gui.panels) {
            p.x = xOff; p.y = yOff;
            xOff += PANEL_W + gap;
            if (xOff + PANEL_W > gui.screenW()) { xOff = gap; yOff += 130; }
        }
        layoutDone = true;
    }

    // ── Heights ──────────────────────────────────────────────────────────────
    private int settH(Setting s) {
        if (s instanceof SliderS) return S_SLIDER;
        if (s instanceof BoolS)   return S_BOOL;
        if (s instanceof KeyS)    return S_BIND;
        return 0;
    }
    private int totalSettH(ModEntry m) {
        int h = 0; for (Setting s : m.settings) h += settH(s); return h;
    }
    private int panelH(Panel p) {
        if (!p.open) return HDR;
        if (p.expanded != null) return HDR + ROW_H + Math.min(totalSettH(p.expanded), MAX_SET_H);
        return HDR + p.mods.size() * ROW_H;
    }

    // ── Render ───────────────────────────────────────────────────────────────
    @Override
    public void render(GuiGraphics g, int mx, int my, int accent, Font font) {
        ensureLayout();
        for (Panel p : gui.panels) renderPanel(g, p, mx, my, font);
    }

    private void renderPanel(GuiGraphics g, Panel p, int mx, int my, Font font) {
        int ph = panelH(p);
        int x1 = p.x, y1 = p.y, x2 = p.x + PANEL_W, y2 = p.y + ph;

        Aurora.glassPanel(g, x1, y1, x2, y2);
        g.fill(x1 + 1, y1 + 1, x2 - 1, y2 - 1, 0x12000000 | SKY);   // faint sky wash

        // ── header ──
        boolean hoverHdr = mx >= x1 && mx < x2 && my >= y1 && my < y1 + HDR;
        if (hoverHdr) g.fill(x1 + 1, y1 + 1, x2 - 1, y1 + HDR, 0x22FFFFFF);
        g.drawString(font, p.cat.label, x1 + PAD, y1 + (HDR - font.lineHeight) / 2, 0xFFFFFFFF, true);
        String sym = p.open ? "–" : "+";
        g.drawString(font, sym, x2 - PAD - font.width(sym), y1 + (HDR - font.lineHeight) / 2, 0xFFFFFFFF, false);
        Aurora.fill(g, x1 + PAD, y1 + HDR - 1, x2 - PAD, y1 + HDR, 0xE6, 0.4f);   // aurora-sky underline

        if (p.open) {
            if (p.expanded != null) drawExpanded(g, p, mx, my, font);
            else                    drawModList(g, p, mx, my, font);
            Aurora.sheen(g, x1, y1, x2, y2);
        }
        Aurora.glassBorder(g, x1, y1, x2, y2, SKY);
    }

    private void drawModList(GuiGraphics g, Panel p, int mx, int my, Font font) {
        int yo = p.y + HDR;
        for (ModEntry m : p.mods) {
            boolean on = m.enabled.getAsBoolean();
            boolean hover = mx >= p.x && mx < p.x + PANEL_W && my >= yo && my < yo + ROW_H;

            if (on)         g.fill(p.x + 1, yo, p.x + PANEL_W - 1, yo + ROW_H, 0x26000000 | SKY);
            else if (hover) g.fill(p.x + 1, yo, p.x + PANEL_W - 1, yo + ROW_H, 0x1FFFFFFF);
            if (on)         g.fill(p.x, yo + 2, p.x + 2, yo + ROW_H - 2, SKYA);   // accent left bar

            int tc = on ? 0xFFFFFFFF : (m.toggleable ? 0xFFC2D4E4 : 0xFF8A93A3);
            g.drawString(font, m.name, p.x + PAD, yo + (ROW_H - font.lineHeight) / 2, tc, on);

            if (m.toggleable)
                pill(g, p.x + PANEL_W - PAD - 18, yo + ROW_H / 2, on, knobProg(m.name, on));
            else if (!m.settings.isEmpty())
                g.drawString(font, "›", p.x + PANEL_W - PAD - 4, yo + (ROW_H - font.lineHeight) / 2, 0xFF9BB0C4, false);

            yo += ROW_H;
        }
    }

    private void drawExpanded(GuiGraphics g, Panel p, int mx, int my, Font font) {
        ModEntry m = p.expanded;
        int yo = p.y + HDR;
        boolean on = m.enabled.getAsBoolean();

        // « name row (left-click toggles, right-click collapses)
        if (on) g.fill(p.x + 1, yo, p.x + PANEL_W - 1, yo + ROW_H, 0x33000000 | SKY);
        g.drawString(font, "‹ " + m.name, p.x + PAD, yo + (ROW_H - font.lineHeight) / 2, on ? 0xFFFFFFFF : 0xFFC2D4E4, true);
        if (m.toggleable) pill(g, p.x + PANEL_W - PAD - 18, yo + ROW_H / 2, on, knobProg(m.name, on));
        yo += ROW_H;

        int settStart = yo;
        int total = totalSettH(m);
        int visH = Math.min(total, MAX_SET_H);
        int maxScroll = Math.max(0, total - MAX_SET_H);
        if (p.scrollOff > maxScroll) p.scrollOff = maxScroll;
        if (p.scrollOff < 0) p.scrollOff = 0;

        g.enableScissor(p.x + 1, settStart, p.x + PANEL_W - 1, settStart + visH);
        int sx = p.x + PAD, sw = PANEL_W - PAD * 2;
        int drawY = settStart - p.scrollOff;
        for (Setting s : m.settings) {
            int sh = settH(s);
            if (drawY + sh > settStart && drawY < settStart + visH) {
                if (s instanceof SliderS sl)    drawSlider(g, sl, sx, drawY, sw, font);
                else if (s instanceof BoolS bs) drawBool(g, bs, sx, drawY, sw, font, m);
                else if (s instanceof KeyS ks)  drawBind(g, ks, sx, drawY, sw, font, gui.rebindTarget == ks);
            }
            drawY += sh;
        }
        g.disableScissor();

        if (total > MAX_SET_H) {
            int barH = Math.max(10, visH * visH / total);
            int barY = settStart + (int)((visH - barH) * ((float) p.scrollOff / maxScroll));
            g.fill(p.x + PANEL_W - 2, barY, p.x + PANEL_W - 1, barY + barH, 0x99000000 | SKY);
        }
    }

    // ── Widgets (sky-blue, full size) ────────────────────────────────────────
    private void pill(GuiGraphics g, int x, int cy, boolean on, float prog) {
        int w = 18, h = 10, y = cy - h / 2;
        GuiDraw.roundRect(g, x, y, x + w, y + h, on ? 0xFF14181F : 0xFF2A2F3A);
        if (on) Aurora.fill(g, x + 1, y + 1, x + w - 1, y + h - 1, 0xE6, 0.4f);
        int kd = h - 4, kx = x + 2 + (int)((w - 4 - kd) * prog);
        GuiDraw.roundRect(g, kx, y + 2, kx + kd, y + 2 + kd, 0xFFFFFFFF);
    }

    private void drawSlider(GuiGraphics g, SliderS sl, int x, int y, int w, Font font) {
        double val = sl.get().getAsDouble();
        g.drawString(font, sl.label(), x, y, 0xFFC2D4E4, false);
        String vs = GuiDraw.fmtVal(val);
        g.drawString(font, vs, x + w - font.width(vs), y, 0xFFFFFFFF, false);
        int barY = y + 12, barH = 4;
        double pct = Math.max(0, Math.min(1, (val - sl.min()) / (sl.max() - sl.min())));
        int filled = (int)(w * pct);
        GuiDraw.roundRect(g, x, barY, x + w, barY + barH, 0xFF20303E);
        if (filled > 0) Aurora.fill(g, x, barY, x + filled, barY + barH, 0xFF, 0.3f);
        int tx = x + filled, tr = 4, tcy = barY + barH / 2;
        GuiDraw.roundRect(g, tx - tr, tcy - tr, tx + tr, tcy + tr, 0xFFFFFFFF);
    }

    private void drawBool(GuiGraphics g, BoolS bs, int x, int y, int w, Font font, ModEntry owner) {
        boolean on = bs.get().getAsBoolean();
        g.drawString(font, bs.label(), x, y + (S_BOOL - font.lineHeight) / 2, 0xFFC2D4E4, false);
        pill(g, x + w - 18, y + S_BOOL / 2, on, knobProg(owner.name + ":" + bs.label(), on));
    }

    private void drawBind(GuiGraphics g, KeyS ks, int x, int y, int w, Font font, boolean binding) {
        g.drawString(font, "Key", x, y + (S_BIND - font.lineHeight) / 2, 0xFFC2D4E4, false);
        String v = binding ? "..." : ModConfig.keyName(ks.get().getAsInt());
        int cw = font.width(v) + 8, cx = x + w - cw, ccy = y + S_BIND / 2;
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

    // ── Input (mirrors the draw layout exactly) ──────────────────────────────
    @Override
    public boolean mouseClicked(int mx, int my, int btn) {
        ensureLayout();
        List<Panel> panels = gui.panels;
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel p = panels.get(i);
            int ph = panelH(p);
            if (mx < p.x || mx >= p.x + PANEL_W || my < p.y || my >= p.y + ph) continue;

            panels.remove(i); panels.add(p);   // bring to front

            // header: +/- on the right, drag elsewhere
            if (my < p.y + HDR) {
                if (btn == 0 && mx >= p.x + PANEL_W - 14) { p.open = !p.open; p.expanded = null; p.scrollOff = 0; }
                else if (btn == 0) { p.dragging = true; p.dragOX = p.x - mx; p.dragOY = p.y - my; }
                return true;
            }
            if (!p.open) return true;

            // expanded settings view
            if (p.expanded != null) {
                int yo = p.y + HDR;
                if (my >= yo && my < yo + ROW_H) {      // « name row
                    if (btn == 0 && p.expanded.toggleable) p.expanded.toggle.run();
                    if (btn == 1) { p.expanded = null; p.scrollOff = 0; }
                    return true;
                }
                yo += ROW_H;
                int settStart = yo, sx = p.x + PAD, sw = PANEL_W - PAD * 2;
                int drawY = settStart - p.scrollOff;
                for (int si = 0; si < p.expanded.settings.size(); si++) {
                    Setting s = p.expanded.settings.get(si);
                    int sh = settH(s);
                    if (my >= Math.max(drawY, settStart) && my < Math.min(drawY + sh, settStart + MAX_SET_H)) {
                        if (btn == 0) {
                            if (s instanceof SliderS sl) { sliderPanel = p; sliderIdx = si; GuiDraw.applySlider(sl, mx, sx, sw); }
                            else if (s instanceof BoolS bs) bs.toggle().run();
                            else if (s instanceof KeyS ks) gui.rebindTarget = ks;
                        }
                        return true;
                    }
                    drawY += sh;
                }
                return true;
            }

            // module list: left = toggle, right = expand settings
            int yo = p.y + HDR;
            for (ModEntry m : p.mods) {
                if (my >= yo && my < yo + ROW_H) {
                    if (btn == 0 && m.toggleable) m.toggle.run();
                    if (btn == 1 && !m.settings.isEmpty()) { p.expanded = m; p.scrollOff = 0; }
                    return true;
                }
                yo += ROW_H;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(int mx, int my) {
        for (Panel p : gui.panels) {
            if (p.dragging) { p.x = p.dragOX + mx; p.y = p.dragOY + my; return true; }
        }
        if (sliderPanel != null && sliderPanel.expanded != null && sliderIdx >= 0
                && sliderIdx < sliderPanel.expanded.settings.size()) {
            Setting s = sliderPanel.expanded.settings.get(sliderIdx);
            if (s instanceof SliderS sl) GuiDraw.applySlider(sl, mx, sliderPanel.x + PAD, PANEL_W - PAD * 2);
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
                    int total = totalSettH(p.expanded);
                    if (total > MAX_SET_H) {
                        p.scrollOff -= (int)(scrollY * 10);
                        int maxScroll = total - MAX_SET_H;
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
