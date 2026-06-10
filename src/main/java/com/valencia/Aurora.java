package com.valencia;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Shared Aurora Glass drawing kit — used by the ClickGUI Aurora layout
 * (guiLayout == 3) and the Aurora TargetHUD style.
 *
 * Flowing gradient: a looping cyan → purple → pink palette sampled per 3px
 * strip with a time phase (8s cycle). Liquid glass: low-alpha rounded panel,
 * specular top highlight, bottom inner shade, plus a slow light band sweeping
 * across the surface (GuiGraphics has no real blur — this is the approximation).
 */
public final class Aurora {

    /** Looping palette: cyan → purple → pink → purple → (cyan). */
    private static final int[] STOPS = {0xFF22D3EE, 0xFFA855F7, 0xFFEC4899, 0xFFA855F7};

    private Aurora() {}

    public static float time() { return (System.currentTimeMillis() % 8000L) / 8000f; }

    /** rgb-only lerp (alpha is applied by callers). */
    private static int lerpColor(int c1, int c2, float t) {
        int r = (int)(((c1 >> 16) & 0xFF) + ((((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t));
        int gr = (int)(((c1 >> 8) & 0xFF) + ((((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t));
        int b = (int)((c1 & 0xFF) + (((c2 & 0xFF) - (c1 & 0xFF)) * t));
        return (r << 16) | (gr << 8) | b;
    }

    /** Sample the looping aurora palette at any phase (wraps). Returns rgb. */
    public static int color(float phase) {
        phase -= (float) Math.floor(phase);
        float seg = phase * STOPS.length;
        int i = (int) seg;
        int j = (i + 1) % STOPS.length;
        return lerpColor(STOPS[i % STOPS.length], STOPS[j], seg - i);
    }

    /** Horizontal flowing aurora gradient drawn as 3px strips. */
    public static void fill(GuiGraphics g, int x1, int y1, int x2, int y2, int alpha, float span) {
        int w = x2 - x1;
        if (w <= 0 || y2 <= y1) return;
        float t = time();
        for (int sx = 0; sx < w; sx += 3) {
            int ex = Math.min(sx + 3, w);
            int c = (alpha << 24) | color(t + span * sx / w);
            g.fill(x1 + sx, y1, x1 + ex, y2, c);
        }
    }

    /** Rounded rect via 2px corner shave (GuiGraphics draws rects only). */
    public static void roundRect(GuiGraphics g, int x1, int y1, int x2, int y2, int c) {
        if (x2 - x1 < 4 || y2 - y1 < 4) { g.fill(x1, y1, x2, y2, c); return; }
        g.fill(x1 + 2, y1,     x2 - 2, y1 + 1, c);
        g.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, c);
        g.fill(x1,     y1 + 2, x2,     y2 - 2, c);
        g.fill(x1 + 1, y2 - 2, x2 - 1, y2 - 1, c);
        g.fill(x1 + 2, y2 - 1, x2 - 2, y2,     c);
    }

    /** Translucent glass base + specular top highlight + bottom inner shade. */
    public static void glassPanel(GuiGraphics g, int x1, int y1, int x2, int y2) {
        roundRect(g, x1, y1, x2, y2, 0xB40C1016);
        g.fill(x1 + 3, y1 + 1, x2 - 3, y1 + 2, 0x46FFFFFF);
        g.fill(x1 + 2, y1 + 2, x2 - 2, y1 + 4, 0x1EFFFFFF);
        g.fill(x1 + 2, y1 + 4, x2 - 2, y1 + 7, 0x0CFFFFFF);
        g.fill(x1 + 2, y2 - 3, x2 - 2, y2 - 1, 0x28000000);
    }

    /** Slow light band sweeping the surface — the "liquid" reflection. Drawn
     *  over the content at very low alpha so it reads as glass, not haze. */
    public static void sheen(GuiGraphics g, int x1, int y1, int x2, int y2) {
        float t = (System.currentTimeMillis() % 5200L) / 5200f;
        int span = (x2 - x1) + 80;
        int bx = x1 - 40 + (int)(span * t);
        g.enableScissor(x1 + 2, y1 + 2, x2 - 2, y2 - 2);
        g.fill(bx,      y1, bx + 7,  y2, 0x06FFFFFF);
        g.fill(bx + 7,  y1, bx + 16, y2, 0x0CFFFFFF);
        g.fill(bx + 16, y1, bx + 23, y2, 0x06FFFFFF);
        g.disableScissor();
    }

    /** Aurora-sampled border + a 1px soft outer glow ring. */
    public static void border(GuiGraphics g, int x1, int y1, int x2, int y2) {
        float t = time();
        fill(g, x1 + 2, y1, x2 - 2, y1 + 1, 0xFF, 0.5f);
        fill(g, x1 + 2, y2 - 1, x2 - 2, y2, 0xFF, 0.5f);
        int lc = color(t), rc = color(t + 0.5f);
        g.fill(x1, y1 + 2, x1 + 1, y2 - 2, 0xFF000000 | lc);
        g.fill(x2 - 1, y1 + 2, x2, y2 - 2, 0xFF000000 | rc);
        fill(g, x1 + 2, y1 - 1, x2 - 2, y1, 0x38, 0.5f);
        fill(g, x1 + 2, y2, x2 - 2, y2 + 1, 0x38, 0.5f);
        g.fill(x1 - 1, y1 + 2, x1, y2 - 2, 0x38000000 | lc);
        g.fill(x2, y1 + 2, x2 + 1, y2 - 2, 0x38000000 | rc);
    }
}
