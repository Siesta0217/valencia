package com.valencia;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ESPRenderer {

    private static final int MIN_BOX_PX = 4;

    // 8-corner index: bit 2 = X, bit 1 = Y, bit 0 = Z
    private static final int[][] EDGES = {
        {0,1},{0,2},{0,4},{1,3},{1,5},{2,3},{2,6},{3,7},{4,5},{4,6},{5,7},{6,7}
    };

    public static void render(GuiGraphics g) {
        if (!ESPMod.isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.position();
        Quaternionf invRot = new Quaternionf(camera.rotation()).conjugate();

        double fovDeg = mc.options.fov().get().doubleValue();
        double tanHalfFov = Math.tan(Math.toRadians(fovDeg) / 2.0);
        Window win = mc.getWindow();
        int viewW = win.getGuiScaledWidth();
        int viewH = win.getGuiScaledHeight();
        double aspect = (double) win.getWidth() / win.getHeight();

        int color    = ESPMod.boxColor();
        int alphaBg  = (color & 0x00FFFFFF) | 0x40000000;
        Font font    = mc.font;

        double maxDistSq = (double) ESPMod.maxDistance * ESPMod.maxDistance;

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!ESPMod.targets(e)) continue;

            double dx = e.getX() - camPos.x;
            double dy = e.getY() - camPos.y;
            double dz = e.getZ() - camPos.z;
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > maxDistSq) continue;

            int[][] pts = project8(e.getBoundingBox(), camPos, invRot, tanHalfFov, aspect, viewW, viewH);
            int[] rect = bounds(pts);
            if (rect == null) continue;
            int x1 = rect[0], y1 = rect[1], x2 = rect[2], y2 = rect[3];
            if (x2 - x1 < MIN_BOX_PX || y2 - y1 < MIN_BOX_PX) continue;
            if (x2 < 0 || y2 < 0 || x1 > viewW || y1 > viewH) continue;

            drawShape(g, pts, x1, y1, x2, y2, color, alphaBg, viewW, viewH);

            if (ESPMod.showHp && e instanceof LivingEntity le) {
                drawHpBar(g, le, x1, y1, y2);
            }
            if (ESPMod.showName || ESPMod.showDistance) {
                double dist = Math.sqrt(distSq);
                drawLabel(g, font, e, x1, x2, y1, y2, dist);
            }
            if (ESPMod.showTracer) {
                drawTracer(g, (x1 + x2) / 2, y2, viewW, viewH, color);
            }
        }
    }

    private static void drawShape(GuiGraphics g, int[][] pts, int x1, int y1, int x2, int y2,
                                  int color, int alphaBg, int viewW, int viewH) {
        int t = Math.max(1, Math.min(3, ESPMod.lineThickness));
        switch (ESPMod.style) {
            case ESPMod.STYLE_HITBOX  -> drawHitbox(g, pts, t, color, viewW, viewH);
            case ESPMod.STYLE_FILLED  -> { g.fill(x1, y1, x2, y2, alphaBg); outlineRect(g, x1, y1, x2, y2, t, color); }
            case ESPMod.STYLE_OUTLINE -> outlineRect(g, x1, y1, x2, y2, t, color);
            default                   -> drawCorners(g, x1, y1, x2, y2, t, color);
        }
    }

    private static void outlineRect(GuiGraphics g, int x1, int y1, int x2, int y2, int t, int color) {
        g.fill(x1, y1,     x2, y1 + t, color);
        g.fill(x1, y2 - t, x2, y2,     color);
        g.fill(x1,     y1, x1 + t, y2, color);
        g.fill(x2 - t, y1, x2,     y2, color);
    }

    private static void drawCorners(GuiGraphics g, int x1, int y1, int x2, int y2, int t, int color) {
        int len  = Math.max(3, Math.min(10, (x2 - x1) / 4));
        int lenV = Math.max(3, Math.min(10, (y2 - y1) / 4));

        g.fill(x1, y1,         x1 + len, y1 + t,    color);
        g.fill(x1, y1,         x1 + t,   y1 + lenV, color);
        g.fill(x2 - len, y1,   x2,       y1 + t,    color);
        g.fill(x2 - t,   y1,   x2,       y1 + lenV, color);
        g.fill(x1, y2 - t,     x1 + len, y2,        color);
        g.fill(x1, y2 - lenV,  x1 + t,   y2,        color);
        g.fill(x2 - len, y2 - t,    x2, y2,         color);
        g.fill(x2 - t,   y2 - lenV, x2, y2,         color);
    }

    private static void drawHitbox(GuiGraphics g, int[][] pts, int t, int color, int viewW, int viewH) {
        for (int[] e : EDGES) {
            int[] a = pts[e[0]];
            int[] b = pts[e[1]];
            if (a == null || b == null) continue;
            // cheap reject: both endpoints share an off-screen side
            if ((a[0] < 0 && b[0] < 0) || (a[0] > viewW && b[0] > viewW)) continue;
            if ((a[1] < 0 && b[1] < 0) || (a[1] > viewH && b[1] > viewH)) continue;
            drawLine(g, a[0], a[1], b[0], b[1], t, color);
        }
    }

    private static void drawLine(GuiGraphics g, int x0, int y0, int x1, int y1, int t, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int steps = Math.max(dx, dy);
        if (steps == 0 || steps > 4000) return;
        int sx = Integer.signum(x1 - x0);
        int sy = Integer.signum(y1 - y0);
        int err = dx - dy;
        int x = x0, y = y0;
        for (int i = 0; i <= steps; i++) {
            g.fill(x, y, x + t, y + t, color);
            int e2 = err << 1;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 <  dx) { err += dx; y += sy; }
        }
    }

    private static void drawHpBar(GuiGraphics g, LivingEntity le, int x1, int y1, int y2) {
        float hp  = le.getHealth();
        float max = le.getMaxHealth();
        if (max <= 0) return;
        float frac = Math.max(0f, Math.min(1f, hp / max));

        int barX2 = x1 - 2;
        int barX1 = barX2 - 3;
        int barH  = y2 - y1;
        int filled = (int)(barH * frac);

        g.fill(barX1 - 1, y1 - 1, barX2 + 1, y2 + 1, 0xFF000000);
        g.fill(barX1, y1, barX2, y2, 0xFF202020);
        g.fill(barX1, y2 - filled, barX2, y2, hpColor(frac));
    }

    private static void drawLabel(GuiGraphics g, Font font, Entity e, int x1, int x2, int y1, int y2, double dist) {
        String name = ESPMod.showName ? e.getName().getString() : "";
        String distStr = ESPMod.showDistance ? String.format("%.0fm", dist) : "";

        if (ESPMod.showName && !name.isEmpty()) {
            int w = font.width(name);
            int tx = (x1 + x2) / 2 - w / 2;
            int ty = y1 - 10;
            g.fill(tx - 2, ty - 1, tx + w + 2, ty + 9, 0x90000000);
            g.drawString(font, name, tx, ty, 0xFFFFFFFF, false);
        }
        if (ESPMod.showDistance && !distStr.isEmpty()) {
            int w = font.width(distStr);
            int tx = (x1 + x2) / 2 - w / 2;
            int ty = y2 + 2;
            g.fill(tx - 2, ty - 1, tx + w + 2, ty + 9, 0x90000000);
            g.drawString(font, distStr, tx, ty, 0xFFCCCCCC, false);
        }
    }

    private static void drawTracer(GuiGraphics g, int x, int y, int viewW, int viewH, int color) {
        int t = Math.max(1, Math.min(3, ESPMod.lineThickness));
        drawLine(g, viewW / 2, viewH, x, y, t, color);
    }

    private static int hpColor(float frac) {
        if (frac > 0.66f) return 0xFF55FF55;
        if (frac > 0.33f) return 0xFFFFAA00;
        return 0xFFFF5555;
    }

    private static int[][] project8(
        AABB box, Vec3 camPos, Quaternionf invRot,
        double tanHalfFov, double aspect, int viewW, int viewH
    ) {
        int[][] pts = new int[8][];
        for (int i = 0; i < 8; i++) {
            double x = (i & 4) != 0 ? box.maxX : box.minX;
            double y = (i & 2) != 0 ? box.maxY : box.minY;
            double z = (i & 1) != 0 ? box.maxZ : box.minZ;
            Vector3f rel = new Vector3f(
                (float)(x - camPos.x),
                (float)(y - camPos.y),
                (float)(z - camPos.z)
            );
            rel.rotate(invRot);
            if (rel.z >= -0.05f) { pts[i] = null; continue; }
            int sx = (int)(viewW / 2.0 + ((double) rel.x / (-rel.z * tanHalfFov * aspect)) * viewW / 2.0);
            int sy = (int)(viewH / 2.0 - ((double) rel.y / (-rel.z * tanHalfFov)) * viewH / 2.0);
            pts[i] = new int[]{ sx, sy };
        }
        return pts;
    }

    private static int[] bounds(int[][] pts) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        int n = 0;
        for (int[] p : pts) {
            if (p == null) continue;
            if (p[0] < minX) minX = p[0];
            if (p[1] < minY) minY = p[1];
            if (p[0] > maxX) maxX = p[0];
            if (p[1] > maxY) maxY = p[1];
            n++;
        }
        return n >= 2 ? new int[]{minX, minY, maxX, maxY} : null;
    }
}
