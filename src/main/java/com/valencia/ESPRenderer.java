package com.valencia;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * 3D-look hitbox ESP. For each ESP-targeted entity we project the 8 corners
 * of {@link Entity#getBoundingBox()} to screen-space and draw the 12 edges
 * of the AABB connecting them — same visual as vanilla F3+B hitboxes.
 *
 * <p>The {@code getBoundingBox()} call goes through {@link com.valencia.mixin.HitboxMixin}
 * so when the Hitbox module inflates the box, the ESP wireframe grows with
 * it — what you see is what melee raycasts hit.
 *
 * <p>We project to 2D and draw via {@link GuiGraphics#fill} (Bresenham line)
 * instead of using actual 3D line rendering because 1.21.11's framegraph
 * pipeline ({@code LevelRenderer.addLateDebugPass} etc.) is fragile to
 * mixins; routing the draw through HudMixin's already-validated render path
 * is rock-solid regardless of shader / depth state.
 *
 * <p>Edges where either endpoint is behind the camera are skipped (vs.
 * proper near-plane clipping) for simplicity — when you walk INTO an
 * entity you'll see some edges disappear, but at normal viewing distance
 * the full wireframe shows.
 */
public class ESPRenderer {

    /** 12 edges of an AABB, expressed as pairs of corner indices [0..7]
     *  where bit 2 = X axis, bit 1 = Y axis, bit 0 = Z axis. */
    private static final int[][] EDGES = {
        {0, 1}, {0, 2}, {0, 4},   // edges from min corner
        {1, 3}, {1, 5},
        {2, 3}, {2, 6},
        {3, 7},
        {4, 5}, {4, 6},
        {5, 7}, {6, 7}
    };

    public static void render(GuiGraphics g) {
        if (!ESPMod.isEnabled() || !ESPMod.showBox) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.position();
        // camera.rotation() rotates view-space → world-space; invert for world → view.
        Quaternionf invRot = new Quaternionf(camera.rotation()).conjugate();

        double fovDeg = mc.options.fov().get().doubleValue();
        double tanHalfFov = Math.tan(Math.toRadians(fovDeg) / 2.0);
        int viewW = mc.getWindow().getGuiScaledWidth();
        int viewH = mc.getWindow().getGuiScaledHeight();
        double aspect = (double) mc.getWindow().getWidth() / mc.getWindow().getHeight();

        int color = ESPMod.boxColor;

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!ESPMod.targets(e)) continue;

            AABB box = e.getBoundingBox();
            int[][] corners = projectCorners(box, camPos, invRot, tanHalfFov, aspect, viewW, viewH);
            if (corners == null) continue;

            drawWireframe(g, corners, color, viewW, viewH);
        }
    }

    /** Project the 8 AABB corners. Returns int[8][2] of screen coords;
     *  entries for corners behind the camera are marked with {@code Integer.MIN_VALUE}.
     *  Returns {@code null} if every corner is behind the camera. */
    private static int[][] projectCorners(
        AABB box, Vec3 camPos, Quaternionf invRot,
        double tanHalfFov, double aspect, int viewW, int viewH
    ) {
        double[][] vertices = {
            {box.minX, box.minY, box.minZ},  // 0: --- (X-/Y-/Z-)
            {box.minX, box.minY, box.maxZ},  // 1: --+
            {box.minX, box.maxY, box.minZ},  // 2: -+-
            {box.minX, box.maxY, box.maxZ},  // 3: -++
            {box.maxX, box.minY, box.minZ},  // 4: +--
            {box.maxX, box.minY, box.maxZ},  // 5: +-+
            {box.maxX, box.maxY, box.minZ},  // 6: ++-
            {box.maxX, box.maxY, box.maxZ},  // 7: +++
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

            // View space: camera looks down -Z. Visible corners have z < 0.
            if (rel.z >= -0.05f) {
                out[i][0] = Integer.MIN_VALUE;
                out[i][1] = Integer.MIN_VALUE;
                continue;
            }

            double ndcX = (double) rel.x / (-rel.z * tanHalfFov * aspect);
            double ndcY = (double) rel.y / (-rel.z * tanHalfFov);
            out[i][0] = (int)(viewW / 2.0 + ndcX * viewW / 2.0);
            out[i][1] = (int)(viewH / 2.0 - ndcY * viewH / 2.0);  // flip Y to screen-down
            anyVisible = true;
        }
        return anyVisible ? out : null;
    }

    private static void drawWireframe(GuiGraphics g, int[][] corners, int color, int viewW, int viewH) {
        for (int[] edge : EDGES) {
            int[] a = corners[edge[0]];
            int[] b = corners[edge[1]];
            if (a[0] == Integer.MIN_VALUE || b[0] == Integer.MIN_VALUE) continue;  // behind camera
            drawLine(g, a[0], a[1], b[0], b[1], color, viewW, viewH);
        }
    }

    /** Bresenham 1px line via 1×1 fills. Capped at 4000 steps so that
     *  off-screen endpoints don't degenerate into a fill-storm. */
    private static void drawLine(GuiGraphics g, int x0, int y0, int x1, int y1, int color, int viewW, int viewH) {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        // Cheap early-out: if both endpoints are off the same edge, skip entirely.
        if ((x0 < 0 && x1 < 0) || (y0 < 0 && y1 < 0)) return;
        if ((x0 > viewW && x1 > viewW) || (y0 > viewH && y1 > viewH)) return;
        // Hard cap to keep one stray edge from melting the frame.
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
