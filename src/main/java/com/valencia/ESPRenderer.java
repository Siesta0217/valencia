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

            int[] rect = projectAABB(e.getBoundingBox(), camPos, invRot, tanHalfFov, aspect, viewW, viewH);
            if (rect == null) continue;
            int x1 = rect[0], y1 = rect[1], x2 = rect[2], y2 = rect[3];
            if (x2 - x1 < MIN_BOX_PX || y2 - y1 < MIN_BOX_PX) continue;
            if (x2 < 0 || y2 < 0 || x1 > viewW || y1 > viewH) continue;

            drawBox(g, x1, y1, x2, y2, color, alphaBg);

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

    private static void drawBox(GuiGraphics g, int x1, int y1, int x2, int y2, int color, int alphaBg) {
        int t = Math.max(1, Math.min(3, ESPMod.lineThickness));
        switch (ESPMod.style) {
            case ESPMod.STYLE_FILLED -> {
                g.fill(x1, y1, x2, y2, alphaBg);
                outlineRect(g, x1, y1, x2, y2, t, color);
            }
            case ESPMod.STYLE_OUTLINE -> outlineRect(g, x1, y1, x2, y2, t, color);
            default -> drawCorners(g, x1, y1, x2, y2, t, color);
        }
    }

    private static void outlineRect(GuiGraphics g, int x1, int y1, int x2, int y2, int t, int color) {
        g.fill(x1, y1,     x2, y1 + t, color);
        g.fill(x1, y2 - t, x2, y2,     color);
        g.fill(x1,     y1, x1 + t, y2, color);
        g.fill(x2 - t, y1, x2,     y2, color);
    }

    private static void drawCorners(GuiGraphics g, int x1, int y1, int x2, int y2, int t, int color) {
        int len = Math.max(3, Math.min(10, (x2 - x1) / 4));
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
        int sx = viewW / 2;
        int sy = viewH;
        int dx = x - sx, dy = y - sy;
        int steps = Math.max(Math.abs(dx), Math.abs(dy));
        if (steps == 0 || steps > 4000) return;
        for (int i = 0; i <= steps; i++) {
            int px = sx + (dx * i) / steps;
            int py = sy + (dy * i) / steps;
            g.fill(px, py, px + 1, py + 1, color);
        }
    }

    private static int hpColor(float frac) {
        if (frac > 0.66f) return 0xFF55FF55;
        if (frac > 0.33f) return 0xFFFFAA00;
        return 0xFFFF5555;
    }

    private static int[] projectAABB(
        AABB box, Vec3 camPos, Quaternionf invRot,
        double tanHalfFov, double aspect, int viewW, int viewH
    ) {
        double[][] verts = {
            {box.minX, box.minY, box.minZ}, {box.minX, box.minY, box.maxZ},
            {box.minX, box.maxY, box.minZ}, {box.minX, box.maxY, box.maxZ},
            {box.maxX, box.minY, box.minZ}, {box.maxX, box.minY, box.maxZ},
            {box.maxX, box.maxY, box.minZ}, {box.maxX, box.maxY, box.maxZ},
        };

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        int visible = 0;

        for (double[] v : verts) {
            Vector3f rel = new Vector3f(
                (float)(v[0] - camPos.x),
                (float)(v[1] - camPos.y),
                (float)(v[2] - camPos.z)
            );
            rel.rotate(invRot);
            if (rel.z >= -0.05f) continue;

            int sx = (int)(viewW / 2.0 + ((double) rel.x / (-rel.z * tanHalfFov * aspect)) * viewW / 2.0);
            int sy = (int)(viewH / 2.0 - ((double) rel.y / (-rel.z * tanHalfFov)) * viewH / 2.0);
            if (sx < minX) minX = sx;
            if (sy < minY) minY = sy;
            if (sx > maxX) maxX = sx;
            if (sy > maxY) maxY = sy;
            visible++;
        }

        return visible >= 2 ? new int[]{minX, minY, maxX, maxY} : null;
    }
}
