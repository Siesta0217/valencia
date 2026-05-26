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
import org.joml.Matrix3x2fStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * ESP renderer with proper near-plane clipping.
 *
 * Each AABB has 8 corners and 12 edges. When the camera is close to or
 * inside the AABB some corners end up behind the camera (view-space
 * z >= 0). Naively projecting them produces wild screen coords and
 * full-screen lines. We instead clip every edge at the near plane in
 * view space, then project the (possibly clipped) endpoints. The 2D
 * bounding rect used by the non-Hitbox styles is derived from the same
 * clipped endpoints so all four styles agree on what's on screen.
 */
public class ESPRenderer {

    private static final int   MIN_BOX_PX = 4;
    private static final float NEAR_Z     = -0.1f;

    // 8-corner index: bit 2 = X, bit 1 = Y, bit 0 = Z
    private static final int[][] EDGES = {
        {0,1},{0,2},{0,4},{1,3},{1,5},{2,3},{2,6},{3,7},{4,5},{4,6},{5,7},{6,7}
    };

    // Per-frame scratch — GUI render is single-threaded.
    private static final Vector3f[] CORNERS = new Vector3f[8];
    static { for (int i = 0; i < 8; i++) CORNERS[i] = new Vector3f(); }

    // Output of clipEdge: two projected endpoints for the current edge.
    private static int OUT_AX, OUT_AY, OUT_BX, OUT_BY;

    public static void render(GuiGraphics g) {
        if (!ESPMod.isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.position();
        Quaternionf invRot = new Quaternionf(camera.rotation()).conjugate();

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Matrix4f proj = mc.gameRenderer.getProjectionMatrix(partialTick);
        double m00 = proj.m00();
        double m11 = proj.m11();
        Window win = mc.getWindow();
        int viewW = win.getGuiScaledWidth();
        int viewH = win.getGuiScaledHeight();

        // Diagonal cap — any line longer than this is geometry pathology, skip.
        int maxLine = 2 * (viewW + viewH);
        Font font = mc.font;
        double maxDistSq = (double) ESPMod.maxDistance * ESPMod.maxDistance;
        int style = ESPMod.style;

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!ESPMod.targets(e)) continue;
            if (!e.isAlive()) continue;

            double dxw = e.getX() - camPos.x;
            double dyw = e.getY() - camPos.y;
            double dzw = e.getZ() - camPos.z;
            double distSq = dxw * dxw + dyw * dyw + dzw * dzw;
            if (distSq > maxDistSq) continue;

            computeCorners(e.getBoundingBox(), camPos, invRot);

            // Single edge loop: clip → project → update bounds → optionally draw hitbox edge.
            int color   = ESPMod.colorFor(e);
            int alphaBg = (color & 0x00FFFFFF) | 0x40000000;
            int t       = Math.max(1, Math.min(3, ESPMod.lineThickness));

            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
            int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
            boolean anyEdge = false;

            for (int[] edge : EDGES) {
                if (!clipEdge(CORNERS[edge[0]], CORNERS[edge[1]], m00, m11, viewW, viewH)) continue;
                anyEdge = true;
                if (OUT_AX < minX) minX = OUT_AX;
                if (OUT_AY < minY) minY = OUT_AY;
                if (OUT_AX > maxX) maxX = OUT_AX;
                if (OUT_AY > maxY) maxY = OUT_AY;
                if (OUT_BX < minX) minX = OUT_BX;
                if (OUT_BY < minY) minY = OUT_BY;
                if (OUT_BX > maxX) maxX = OUT_BX;
                if (OUT_BY > maxY) maxY = OUT_BY;

                if (style == ESPMod.STYLE_HITBOX) {
                    // Cheap reject: edge entirely off the same screen side.
                    if ((OUT_AX < 0 && OUT_BX < 0) || (OUT_AX > viewW && OUT_BX > viewW)) continue;
                    if ((OUT_AY < 0 && OUT_BY < 0) || (OUT_AY > viewH && OUT_BY > viewH)) continue;
                    drawLine(g, OUT_AX, OUT_AY, OUT_BX, OUT_BY, t, color, maxLine);
                }
            }

            if (!anyEdge) continue;
            if (maxX - minX < MIN_BOX_PX || maxY - minY < MIN_BOX_PX) continue;
            if (maxX < 0 || maxY < 0 || minX > viewW || minY > viewH) continue;

            switch (style) {
                case ESPMod.STYLE_HITBOX -> { /* already drawn above */ }
                case ESPMod.STYLE_FILLED -> { g.fill(minX, minY, maxX, maxY, alphaBg); outlineRect(g, minX, minY, maxX, maxY, t, color); }
                case ESPMod.STYLE_OUTLINE -> outlineRect(g, minX, minY, maxX, maxY, t, color);
                default -> drawCorners(g, minX, minY, maxX, maxY, t, color);
            }

            if (ESPMod.showHp && e instanceof LivingEntity le) drawHpBar(g, le, minX, minY, maxY);
            if (ESPMod.showName || ESPMod.showDistance) drawLabel(g, font, e, minX, maxX, minY, maxY, (int) Math.sqrt(distSq));
            if (ESPMod.showTracer) drawLine(g, viewW / 2, viewH, (minX + maxX) / 2, maxY, t, color, maxLine);
        }
    }

    private static void computeCorners(AABB box, Vec3 camPos, Quaternionf invRot) {
        double cx = camPos.x, cy = camPos.y, cz = camPos.z;
        for (int i = 0; i < 8; i++) {
            double x = (i & 4) != 0 ? box.maxX : box.minX;
            double y = (i & 2) != 0 ? box.maxY : box.minY;
            double z = (i & 1) != 0 ? box.maxZ : box.minZ;
            CORNERS[i].set((float)(x - cx), (float)(y - cy), (float)(z - cz));
            CORNERS[i].rotate(invRot);
        }
    }

    /**
     * Clip an edge against the near plane (view-space z = NEAR_Z) and
     * project both endpoints. Returns false if the entire edge is
     * behind the near plane. On success writes OUT_AX/AY/BX/BY.
     */
    private static boolean clipEdge(Vector3f a, Vector3f b, double m00, double m11, int viewW, int viewH) {
        boolean aFront = a.z < NEAR_Z;
        boolean bFront = b.z < NEAR_Z;
        if (!aFront && !bFront) return false;

        float ax = a.x, ay = a.y, az = a.z;
        float bx = b.x, by = b.y, bz = b.z;
        if (!aFront) {
            float u = (NEAR_Z - b.z) / (a.z - b.z);
            ax = b.x + u * (a.x - b.x);
            ay = b.y + u * (a.y - b.y);
            az = NEAR_Z;
        } else if (!bFront) {
            float u = (NEAR_Z - a.z) / (b.z - a.z);
            bx = a.x + u * (b.x - a.x);
            by = a.y + u * (b.y - a.y);
            bz = NEAR_Z;
        }

        OUT_AX = projectX(ax, az, m00, viewW);
        OUT_AY = projectY(ay, az, m11, viewH);
        OUT_BX = projectX(bx, bz, m00, viewW);
        OUT_BY = projectY(by, bz, m11, viewH);
        return true;
    }

    private static int projectX(float rx, float rz, double m00, int viewW) {
        double v = viewW / 2.0 + ((double) rx * m00 / -rz) * viewW / 2.0;
        if (v < -100000) return -100000;
        if (v >  100000) return  100000;
        return (int) v;
    }

    private static int projectY(float ry, float rz, double m11, int viewH) {
        double v = viewH / 2.0 - ((double) ry * m11 / -rz) * viewH / 2.0;
        if (v < -100000) return -100000;
        if (v >  100000) return  100000;
        return (int) v;
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

    /**
     * Draw a line as a single rotated quad.
     * Axis-aligned fast paths bypass atan2 + matrix push.
     */
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
        float hp  = le.getHealth();
        float max = le.getMaxHealth();
        if (max <= 0) return;
        float frac = Math.max(0f, Math.min(1f, hp / max));

        int barX2 = x1 - 2;
        int barX1 = barX2 - 3;
        if (barX1 < 0) return;
        int barH  = y2 - y1;
        int filled = (int)(barH * frac);

        g.fill(barX1 - 1, y1 - 1, barX2 + 1, y2 + 1, 0xFF000000);
        g.fill(barX1, y1, barX2, y2, 0xFF202020);
        g.fill(barX1, y2 - filled, barX2, y2, hpColor(frac));
    }

    private static void drawLabel(GuiGraphics g, Font font, Entity e, int x1, int x2, int y1, int y2, int distM) {
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
}
