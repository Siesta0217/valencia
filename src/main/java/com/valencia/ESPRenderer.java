package com.valencia;

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

    private static final int[][] EDGES = {
        {0, 1}, {0, 2}, {0, 4},
        {1, 3}, {1, 5},
        {2, 3}, {2, 6},
        {3, 7},
        {4, 5}, {4, 6},
        {5, 7}, {6, 7}
    };

    public static void render(GuiGraphics g) {
        if (!ESPMod.isEnabled()) return;
        if (!ESPMod.showBox && !ESPMod.showName && !ESPMod.showHealth) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.position();
        Quaternionf invRot = new Quaternionf(camera.rotation()).conjugate();

        double fovDeg = mc.options.fov().get().doubleValue();
        double tanHalfFov = Math.tan(Math.toRadians(fovDeg) / 2.0);
        int viewW = mc.getWindow().getGuiScaledWidth();
        int viewH = mc.getWindow().getGuiScaledHeight();
        double aspect = (double) mc.getWindow().getWidth() / mc.getWindow().getHeight();

        Font font = mc.font;

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!ESPMod.targets(e)) continue;

            AABB box = e.getBoundingBox();
            int[][] corners = projectCorners(box, camPos, invRot, tanHalfFov, aspect, viewW, viewH);
            if (corners == null) continue;

            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
            boolean anyVisible = false;
            for (int[] c : corners) {
                if (c[0] == Integer.MIN_VALUE) continue;
                anyVisible = true;
                if (c[0] < minX) minX = c[0];
                if (c[1] < minY) minY = c[1];
                if (c[0] > maxX) maxX = c[0];
                if (c[1] > maxY) maxY = c[1];
            }
            if (!anyVisible) continue;

            int color = ESPMod.boxColor;

            if (ESPMod.showBox) {
                if (ESPMod.cornerBox) {
                    drawCornerBox(g, minX, minY, maxX, maxY, color);
                } else {
                    drawWireframe(g, corners, color, viewW, viewH);
                }
            }

            if (ESPMod.showName) {
                String name = e.getName().getString();
                int tw = font.width(name);
                int nx = (minX + maxX) / 2 - tw / 2;
                int ny = minY - 12;
                g.fill(nx - 2, ny - 1, nx + tw + 2, ny + 9, 0xAA000000);
                g.drawString(font, name, nx, ny, ESPMod.nameColor, true);
            }

            if (ESPMod.showHealth && e instanceof LivingEntity le) {
                float hp = le.getHealth();
                float maxHp = le.getMaxHealth();
                if (maxHp <= 0) maxHp = 1;
                float pct = Math.min(1.0f, hp / maxHp);

                int barX = minX - 4;
                int barTop = minY;
                int barBot = maxY;
                int barH = barBot - barTop;
                if (barH < 4) barH = 4;

                g.fill(barX - 1, barTop - 1, barX + 2, barBot + 1, 0xAA000000);

                int filledH = (int)(barH * pct);
                int hpColor = lerpColor(ESPMod.healthColorLow, ESPMod.healthColorHigh, pct);
                g.fill(barX, barBot - filledH, barX + 1, barBot, hpColor);

                if (hp < maxHp) {
                    String hpStr = String.format("%.0f", hp);
                    g.drawString(font, hpStr, barX - font.width(hpStr) - 2, barBot - filledH - 4, hpColor, true);
                }
            }
        }
    }

    private static void drawCornerBox(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        int w = x2 - x1;
        int h = y2 - y1;
        int cw = Math.max(3, w / 4);
        int ch = Math.max(3, h / 4);
        int brd = 0xAA000000;

        // top-left
        g.fill(x1 - 1, y1 - 1, x1 + cw + 1, y1 + 2, brd);
        g.fill(x1 - 1, y1 - 1, x1 + 2, y1 + ch + 1, brd);
        g.fill(x1, y1, x1 + cw, y1 + 1, color);
        g.fill(x1, y1, x1 + 1, y1 + ch, color);
        // top-right
        g.fill(x2 - cw - 1, y1 - 1, x2 + 1, y1 + 2, brd);
        g.fill(x2 - 2, y1 - 1, x2 + 1, y1 + ch + 1, brd);
        g.fill(x2 - cw, y1, x2, y1 + 1, color);
        g.fill(x2 - 1, y1, x2, y1 + ch, color);
        // bottom-left
        g.fill(x1 - 1, y2 - 2, x1 + cw + 1, y2 + 1, brd);
        g.fill(x1 - 1, y2 - ch - 1, x1 + 2, y2 + 1, brd);
        g.fill(x1, y2 - 1, x1 + cw, y2, color);
        g.fill(x1, y2 - ch, x1 + 1, y2, color);
        // bottom-right
        g.fill(x2 - cw - 1, y2 - 2, x2 + 1, y2 + 1, brd);
        g.fill(x2 - 2, y2 - ch - 1, x2 + 1, y2 + 1, brd);
        g.fill(x2 - cw, y2 - 1, x2, y2, color);
        g.fill(x2 - 1, y2 - ch, x2, y2, color);
    }

    private static int lerpColor(int cA, int cB, float t) {
        int aA = (cA >> 24) & 0xFF, rA = (cA >> 16) & 0xFF, gA = (cA >> 8) & 0xFF, bA = cA & 0xFF;
        int aB = (cB >> 24) & 0xFF, rB = (cB >> 16) & 0xFF, gB = (cB >> 8) & 0xFF, bB = cB & 0xFF;
        int a = aA + (int)((aB - aA) * t);
        int r = rA + (int)((rB - rA) * t);
        int gr = gA + (int)((gB - gA) * t);
        int b = bA + (int)((bB - bA) * t);
        return (a << 24) | (r << 16) | (gr << 8) | b;
    }

    private static int[][] projectCorners(
        AABB box, Vec3 camPos, Quaternionf invRot,
        double tanHalfFov, double aspect, int viewW, int viewH
    ) {
        double[][] vertices = {
            {box.minX, box.minY, box.minZ},
            {box.minX, box.minY, box.maxZ},
            {box.minX, box.maxY, box.minZ},
            {box.minX, box.maxY, box.maxZ},
            {box.maxX, box.minY, box.minZ},
            {box.maxX, box.minY, box.maxZ},
            {box.maxX, box.maxY, box.minZ},
            {box.maxX, box.maxY, box.maxZ},
        };

        int[][] out = new int[8][2];
        boolean anyVisible = false;
        for (int i = 0; i < 8; i++) {
            double[] v = vertices[i];
            Vector3f rel = new Vector3f(
                (float)(v[0] - camPos.x),
                (float)(v[1] - camPos.y),
                (float)(v[2] - camPos.z)
            );
            rel.rotate(invRot);

            if (rel.z >= -0.05f) {
                out[i][0] = Integer.MIN_VALUE;
                out[i][1] = Integer.MIN_VALUE;
                continue;
            }

            double ndcX = (double) rel.x / (-rel.z * tanHalfFov * aspect);
            double ndcY = (double) rel.y / (-rel.z * tanHalfFov);
            out[i][0] = (int)(viewW / 2.0 + ndcX * viewW / 2.0);
            out[i][1] = (int)(viewH / 2.0 - ndcY * viewH / 2.0);
            anyVisible = true;
        }
        return anyVisible ? out : null;
    }

    private static void drawWireframe(GuiGraphics g, int[][] corners, int color, int viewW, int viewH) {
        for (int[] edge : EDGES) {
            int[] a = corners[edge[0]];
            int[] b = corners[edge[1]];
            if (a[0] == Integer.MIN_VALUE || b[0] == Integer.MIN_VALUE) continue;
            drawLine(g, a[0], a[1], b[0], b[1], color, viewW, viewH);
        }
    }

    private static void drawLine(GuiGraphics g, int x0, int y0, int x1, int y1, int color, int viewW, int viewH) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        if ((x0 < 0 && x1 < 0) || (y0 < 0 && y1 < 0)) return;
        if ((x0 > viewW && x1 > viewW) || (y0 > viewH && y1 > viewH)) return;
        if (dx > 4000 || dy > 4000) return;

        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int steps = 0;
        while (steps++ < 4000) {
            if (x0 >= 0 && x0 < viewW && y0 >= 0 && y0 < viewH) {
                g.fill(x0, y0, x0 + 1, y0 + 1, color);
            }
            if (x0 == x1 && y0 == y1) break;
            int e2 = err << 1;
            if (e2 > -dy) { err -= dy; x0 += sx; }
            if (e2 <  dx) { err += dx; y0 += sy; }
        }
    }
}
