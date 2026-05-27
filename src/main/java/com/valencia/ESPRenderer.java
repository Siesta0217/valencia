package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;

/**
 * 2D HUD overlay for entity ESP.
 *
 * Pipeline per frame:
 *   1. Projection.begin() — snapshot camera + projection matrix.
 *   2. For each targeted entity:
 *        a. Build a render-interpolated AABB.
 *        b. Projection.projectAabb() — produces 2D rect + per-edge segments.
 *        c. Reject if invisible / too small / fully off-screen.
 *        d. Draw box according to selected style.
 *        e. Optional HP bar / name / distance / tracer.
 *
 * Drawing is intentionally trivial — every shape is g.fill() of an
 * axis-aligned rect, except diagonal Hitbox edges which use a single
 * rotated quad via Matrix3x2fStack.
 */
public final class ESPRenderer {

    /** Minimum 2D footprint to bother rendering. Avoids 1×1 dots at huge distances. */
    private static final int MIN_BOX_PX = 4;

    /** Vanilla F3+B eye-forward line colour (soft blue). */
    private static final int EYE_LINE_COLOR = 0xFF55AAFF;

    /** Distance in blocks the eye-forward indicator extends from the entity's eyes. */
    private static final double EYE_LINE_BLOCKS = 2.0;

    /** Per-frame scratch — HUD render is single-threaded. */
    private static final Projection.ScreenAabb SCRATCH = new Projection.ScreenAabb();
    private static final int[] EYE_FROM = new int[2];
    private static final int[] EYE_TO   = new int[2];

    private ESPRenderer() {}

    public static void render(GuiGraphics g) {
        if (!ESPMod.isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Projection.Frame frame = Projection.begin(partialTick);

        Font font = mc.font;
        final int viewW = frame.viewW;
        final int viewH = frame.viewH;
        final int crosshairX = viewW / 2;
        final int crosshairY = viewH / 2;
        // Any line longer than the screen diagonal × 2 is pathological geometry.
        final int maxLine = 2 * (viewW + viewH);
        final double maxDistSq = (double) ESPMod.maxDistance * ESPMod.maxDistance;
        final int style = ESPMod.style;

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!ESPMod.targets(e)) continue;
            if (!e.isAlive()) continue;

            Vec3 renderPos = e.getPosition(partialTick);
            double dxw = renderPos.x - frame.camX;
            double dyw = renderPos.y - frame.camY;
            double dzw = renderPos.z - frame.camZ;
            double distSq = dxw * dxw + dyw * dyw + dzw * dzw;
            if (distSq > maxDistSq) continue;

            AABB renderBox = e.getBoundingBox().move(
                renderPos.x - e.getX(),
                renderPos.y - e.getY(),
                renderPos.z - e.getZ()
            );

            Projection.ScreenAabb sa = SCRATCH;
            Projection.projectAabb(frame, renderBox, sa);
            if (!sa.visible) continue;

            int w = sa.maxX - sa.minX;
            int h = sa.maxY - sa.minY;
            if (w < MIN_BOX_PX || h < MIN_BOX_PX) continue;
            if (sa.maxX < 0 || sa.maxY < 0 || sa.minX > viewW || sa.minY > viewH) continue;

            int color = ESPMod.colorFor(e);
            int thickness = clampInt(ESPMod.lineThickness, 1, 3);

            int drawMinX = clampInt(sa.minX, 0, viewW);
            int drawMinY = clampInt(sa.minY, 0, viewH);
            int drawMaxX = clampInt(sa.maxX, 0, viewW);
            int drawMaxY = clampInt(sa.maxY, 0, viewH);
            if (drawMaxX - drawMinX < MIN_BOX_PX || drawMaxY - drawMinY < MIN_BOX_PX) continue;

            switch (style) {
                case ESPMod.STYLE_HITBOX  -> {
                    drawHitbox(g, sa, viewW, viewH, thickness, color, maxLine);
                    if (e instanceof LivingEntity le) {
                        drawEyeForwardLine(g, frame, le, partialTick, maxLine);
                    }
                }
                case ESPMod.STYLE_FILLED  -> drawFilled (g, drawMinX, drawMinY, drawMaxX, drawMaxY, thickness, color);
                case ESPMod.STYLE_OUTLINE -> drawOutline(g, drawMinX, drawMinY, drawMaxX, drawMaxY, thickness, color);
                default                   -> drawCorners(g, drawMinX, drawMinY, drawMaxX, drawMaxY, thickness, color);
            }

            if (ESPMod.showHp && e instanceof LivingEntity le) {
                drawHpBar(g, le, drawMinX, drawMinY, drawMaxY);
            }
            if (ESPMod.showName || ESPMod.showDistance) {
                drawLabels(g, font, e, drawMinX, drawMaxX, drawMinY, drawMaxY, (int) Math.sqrt(distSq));
            }
            if (ESPMod.showTracer) {
                int targetX = (drawMinX + drawMaxX) / 2;
                int targetY = (drawMinY + drawMaxY) / 2;
                drawLine(g, crosshairX, crosshairY, targetX, targetY, thickness, color, maxLine);
            }
        }
    }

    // ─── styles ─────────────────────────────────────────────────────────

    /**
     * Vanilla F3+B style eye-direction indicator: a short line from the entity's
     * eyes pointing along its look vector. Two world points → projectPoint × 2 →
     * one diagonal line. Skipped silently if either endpoint is behind the camera.
     */
    private static void drawEyeForwardLine(GuiGraphics g, Projection.Frame f,
                                            LivingEntity le, float partialTick, int maxLine) {
        Vec3 eye = le.getEyePosition(partialTick);
        Vec3 look = le.getViewVector(partialTick);
        double tx = eye.x + look.x * EYE_LINE_BLOCKS;
        double ty = eye.y + look.y * EYE_LINE_BLOCKS;
        double tz = eye.z + look.z * EYE_LINE_BLOCKS;
        if (!Projection.projectPoint(f, eye.x, eye.y, eye.z, EYE_FROM)) return;
        if (!Projection.projectPoint(f, tx,    ty,    tz,    EYE_TO))   return;
        drawLine(g, EYE_FROM[0], EYE_FROM[1], EYE_TO[0], EYE_TO[1], 1, EYE_LINE_COLOR, maxLine);
    }

    private static void drawHitbox(GuiGraphics g, Projection.ScreenAabb sa,
                                   int viewW, int viewH, int thickness, int color, int maxLine) {
        int[] e = sa.edges;
        int mask = sa.validMask;
        for (int i = 0; i < 12; i++) {
            if ((mask & (1 << i)) == 0) continue;
            int o = i * 4;
            int ax = e[o], ay = e[o + 1], bx = e[o + 2], by = e[o + 3];
            // Trivial reject — edge entirely off one side of the screen.
            if ((ax < 0 && bx < 0) || (ax > viewW && bx > viewW)) continue;
            if ((ay < 0 && by < 0) || (ay > viewH && by > viewH)) continue;
            drawLine(g, ax, ay, bx, by, thickness, color, maxLine);
        }
    }

    private static void drawOutline(GuiGraphics g, int x1, int y1, int x2, int y2, int t, int color) {
        g.fill(x1,     y1,     x2,     y1 + t, color);
        g.fill(x1,     y2 - t, x2,     y2,     color);
        g.fill(x1,     y1,     x1 + t, y2,     color);
        g.fill(x2 - t, y1,     x2,     y2,     color);
    }

    private static void drawFilled(GuiGraphics g, int x1, int y1, int x2, int y2, int t, int color) {
        int fillColor = (color & 0x00FFFFFF) | 0x40000000;
        g.fill(x1, y1, x2, y2, fillColor);
        drawOutline(g, x1, y1, x2, y2, t, color);
    }

    private static void drawCorners(GuiGraphics g, int x1, int y1, int x2, int y2, int t, int color) {
        int lenH = clampInt((x2 - x1) / 4, 3, 10);
        int lenV = clampInt((y2 - y1) / 4, 3, 10);
        // top-left
        g.fill(x1,         y1,         x1 + lenH, y1 + t,    color);
        g.fill(x1,         y1,         x1 + t,    y1 + lenV, color);
        // top-right
        g.fill(x2 - lenH,  y1,         x2,        y1 + t,    color);
        g.fill(x2 - t,     y1,         x2,        y1 + lenV, color);
        // bottom-left
        g.fill(x1,         y2 - t,     x1 + lenH, y2,        color);
        g.fill(x1,         y2 - lenV,  x1 + t,    y2,        color);
        // bottom-right
        g.fill(x2 - lenH,  y2 - t,     x2,        y2,        color);
        g.fill(x2 - t,     y2 - lenV,  x2,        y2,        color);
    }

    // ─── primitives ─────────────────────────────────────────────────────

    /** Single line as one rectangle (axis-aligned) or one rotated quad (diagonal). */
    private static void drawLine(GuiGraphics g, int x0, int y0, int x1, int y1, int t, int color, int maxLine) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        if (dx == 0 && dy == 0) return;

        if (dy == 0) {
            int a = Math.min(x0, x1), b = Math.max(x0, x1);
            if (b - a > maxLine) return;
            g.fill(a, y0, b + t, y0 + t, color);
            return;
        }
        if (dx == 0) {
            int a = Math.min(y0, y1), b = Math.max(y0, y1);
            if (b - a > maxLine) return;
            g.fill(x0, a, x0 + t, b + t, color);
            return;
        }

        double len = Math.sqrt((double) dx * dx + (double) dy * dy);
        if (len > maxLine) return;
        float angle = (float) Math.atan2(dy, dx);

        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(x0, y0);
        pose.rotate(angle);
        g.fill(0, 0, (int) Math.ceil(len), t, color);
        pose.popMatrix();
    }

    private static void drawHpBar(GuiGraphics g, LivingEntity le, int x1, int y1, int y2) {
        float hp = le.getHealth();
        float max = le.getMaxHealth();
        if (max <= 0f) return;
        float frac = Math.max(0f, Math.min(1f, hp / max));

        int barX2 = x1 - 2;
        int barX1 = barX2 - 3;
        if (barX1 < 0) return;
        int barH = y2 - y1;
        int filled = (int) (barH * frac);

        g.fill(barX1 - 1, y1 - 1, barX2 + 1, y2 + 1, 0xFF000000);
        g.fill(barX1,     y1,     barX2,     y2,     0xFF202020);
        g.fill(barX1, y2 - filled, barX2, y2, hpColor(frac));
    }

    private static void drawLabels(GuiGraphics g, Font font, Entity e,
                                    int x1, int x2, int y1, int y2, int distM) {
        if (ESPMod.showName) {
            String name = e.getName().getString();
            int w = font.width(name);
            int tx = (x1 + x2) / 2 - w / 2;
            int ty = y1 - 10;
            g.fill(tx - 2, ty - 1, tx + w + 2, ty + 9, 0x90000000);
            g.drawString(font, name, tx, ty, 0xFFFFFFFF, false);
        }
        if (ESPMod.showDistance) {
            String distStr = distM + "m";
            int w = font.width(distStr);
            int tx = (x1 + x2) / 2 - w / 2;
            int ty = y2 + 2;
            g.fill(tx - 2, ty - 1, tx + w + 2, ty + 9, 0x90000000);
            g.drawString(font, distStr, tx, ty, 0xFFCCCCCC, false);
        }
    }

    private static int hpColor(float frac) {
        if (frac > 0.66f) return 0xFF55FF55;
        if (frac > 0.33f) return 0xFFFFAA00;
        return 0xFFFF5555;
    }

    private static int clampInt(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
