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

public class ESPRenderer {

    private static final int MIN_BOX_PX = 4;
    private static final int MAX_LINE_PX = 4000;

    // 8-corner index: bit 2 = X, bit 1 = Y, bit 0 = Z
    private static final int[][] EDGES = {
        {0,1},{0,2},{0,4},{1,3},{1,5},{2,3},{2,6},{3,7},{4,5},{4,6},{5,7},{6,7}
    };

    // Per-entity scratch buffers — GUI render is single-threaded.
    private static final int[]     XS  = new int[8];
    private static final int[]     YS  = new int[8];
    private static final boolean[] VIS = new boolean[8];
    private static final Vector3f  REL = new Vector3f();

    public static void render(GuiGraphics g) {
        if (!ESPMod.isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.position();
        Quaternionf invRot = new Quaternionf(camera.rotation()).conjugate();

        // Use the engine's actual projection matrix — picks up zoom mods,
        // Lunar FOV overrides, anything that modifies the rendered FOV.
        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Matrix4f proj = mc.gameRenderer.getProjectionMatrix(partialTick);
        double m00 = proj.m00();   // == 1 / (tanHalfFov * aspect)
        double m11 = proj.m11();   // == 1 / tanHalfFov
        Window win = mc.getWindow();
        int viewW = win.getGuiScaledWidth();
        int viewH = win.getGuiScaledHeight();

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

            project8(e.getBoundingBox(), camPos, invRot, m00, m11, viewW, viewH);
            int[] rect = bounds();
            if (rect == null) continue;
            int x1 = rect[0], y1 = rect[1], x2 = rect[2], y2 = rect[3];
            if (x2 - x1 < MIN_BOX_PX || y2 - y1 < MIN_BOX_PX) continue;
            if (x2 < 0 || y2 < 0 || x1 > viewW || y1 > viewH) continue;

            drawShape(g, x1, y1, x2, y2, color, alphaBg, viewW, viewH);

            if (ESPMod.showHp && e instanceof LivingEntity le) {
                drawHpBar(g, le, x1, y1, y2);
            }
            if (ESPMod.showName || ESPMod.showDistance) {
                drawLabel(g, font, e, x1, x2, y1, y2, (int) Math.sqrt(distSq));
            }
            if (ESPMod.showTracer) {
                drawLine(g, viewW / 2, viewH, (x1 + x2) / 2, y2,
                         Math.max(1, Math.min(3, ESPMod.lineThickness)), color);
            }
        }
    }

    private static void drawShape(GuiGraphics g, int x1, int y1, int x2, int y2,
                                  int color, int alphaBg, int viewW, int viewH) {
        int t = Math.max(1, Math.min(3, ESPMod.lineThickness));
        switch (ESPMod.style) {
            case ESPMod.STYLE_HITBOX  -> drawHitbox(g, t, color, viewW, viewH);
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

    private static void drawHitbox(GuiGraphics g, int t, int color, int viewW, int viewH) {
        for (int[] e : EDGES) {
            int ai = e[0], bi = e[1];
            if (!VIS[ai] || !VIS[bi]) continue;
            int ax = XS[ai], ay = YS[ai], bx = XS[bi], by = YS[bi];
            if ((ax < 0 && bx < 0) || (ax > viewW && bx > viewW)) continue;
            if ((ay < 0 && by < 0) || (ay > viewH && by > viewH)) continue;
            drawLine(g, ax, ay, bx, by, t, color);
        }
    }

    // Draw one line as a single rotated quad: matrix translate to start, rotate,
    // emit a (length × t) axis-aligned fill. Cuts a 100-pixel diagonal from
    // ~100 g.fill calls down to 1.
    private static void drawLine(GuiGraphics g, int x0, int y0, int x1, int y1, int t, int color) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        if (dx == 0 && dy == 0) return;

        // Axis-aligned fast paths — no matrix transform needed.
        if (dy == 0) {
            int a = Math.min(x0, x1), b = Math.max(x0, x1);
            if (b - a > MAX_LINE_PX) return;
            g.fill(a, y0, b + t, y0 + t, color);
            return;
        }
        if (dx == 0) {
            int a = Math.min(y0, y1), b = Math.max(y0, y1);
            if (b - a > MAX_LINE_PX) return;
            g.fill(x0, a, x0 + t, b + t, color);
            return;
        }

        double len = Math.sqrt((double) dx * dx + (double) dy * dy);
        if (len > MAX_LINE_PX) return;
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

    private static void project8(
        AABB box, Vec3 camPos, Quaternionf invRot,
        double m00, double m11, int viewW, int viewH
    ) {
        double cx = camPos.x, cy = camPos.y, cz = camPos.z;
        for (int i = 0; i < 8; i++) {
            double x = (i & 4) != 0 ? box.maxX : box.minX;
            double y = (i & 2) != 0 ? box.maxY : box.minY;
            double z = (i & 1) != 0 ? box.maxZ : box.minZ;
            REL.set((float)(x - cx), (float)(y - cy), (float)(z - cz));
            REL.rotate(invRot);
            if (REL.z >= -0.05f) { VIS[i] = false; continue; }
            double invNegZ = 1.0 / -REL.z;
            XS[i]  = (int)(viewW / 2.0 + ((double) REL.x * m00 * invNegZ) * viewW / 2.0);
            YS[i]  = (int)(viewH / 2.0 - ((double) REL.y * m11 * invNegZ) * viewH / 2.0);
            VIS[i] = true;
        }
    }

    private static final int[] BOUNDS_OUT = new int[4];

    private static int[] bounds() {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        int n = 0;
        for (int i = 0; i < 8; i++) {
            if (!VIS[i]) continue;
            int x = XS[i], y = YS[i];
            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            n++;
        }
        if (n < 2) return null;
        BOUNDS_OUT[0] = minX; BOUNDS_OUT[1] = minY;
        BOUNDS_OUT[2] = maxX; BOUNDS_OUT[3] = maxY;
        return BOUNDS_OUT;
    }
}
