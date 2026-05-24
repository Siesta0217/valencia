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
 * 2D screen-space box ESP. For each ESP-targeted entity we project the 8
 * corners of its {@link Entity#getBoundingBox()} to screen-space and draw
 * an outline rectangle around the union of visible corners.
 *
 * <p>The {@code getBoundingBox()} call deliberately uses the (potentially
 * mixin-inflated) box from {@link com.valencia.mixin.HitboxMixin}, so the
 * ESP rectangle automatically grows / shrinks with the Hitbox module's
 * {@code expand} value — what you see is what melee raycasts will hit.
 *
 * <p>2D is used (instead of 3D wireframes via {@code LevelRenderer}) because
 * 1.21.11's framegraph-based render pipeline makes injecting into world
 * rendering fragile; a 2D overlay on top of the existing HUD is rock-solid
 * and works regardless of shader / depth state.
 */
public class ESPRenderer {

    public static void render(GuiGraphics g) {
        if (!ESPMod.isEnabled() || !ESPMod.showBox) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.position();
        // camera.rotation() rotates view-space → world-space; invert for world→view.
        Quaternionf invRot = new Quaternionf(camera.rotation()).conjugate();

        double fovDeg = mc.options.fov().get().doubleValue();
        double tanHalfFov = Math.tan(Math.toRadians(fovDeg) / 2.0);
        int viewportW = mc.getWindow().getGuiScaledWidth();
        int viewportH = mc.getWindow().getGuiScaledHeight();
        double aspect = (double) mc.getWindow().getWidth() / mc.getWindow().getHeight();

        int color = ESPMod.boxColor;

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!ESPMod.targets(e)) continue;

            AABB box = e.getBoundingBox();
            int[] rect = projectBox(box, camPos, invRot, tanHalfFov, aspect, viewportW, viewportH);
            if (rect == null) continue;

            drawOutline(g, rect[0], rect[1], rect[2], rect[3], color);
        }
    }

    /**
     * Project the 8 corners of {@code box}, return the screen-space
     * bounding rectangle {@code [minX, minY, maxX, maxY]} of the corners
     * that are in front of the camera. Returns {@code null} if no corner
     * is visible (entity entirely behind camera) or the rectangle would
     * be off-screen.
     */
    private static int[] projectBox(
        AABB box, Vec3 camPos, Quaternionf invRot,
        double tanHalfFov, double aspect, int viewW, int viewH
    ) {
        double[] xs = {box.minX, box.maxX};
        double[] ys = {box.minY, box.maxY};
        double[] zs = {box.minZ, box.maxZ};

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
        boolean any = false;

        for (double x : xs) for (double y : ys) for (double z : zs) {
            Vector3f v = new Vector3f(
                (float)(x - camPos.x),
                (float)(y - camPos.y),
                (float)(z - camPos.z)
            );
            v.rotate(invRot);
            // View-space: camera looks down -Z, so visible corners have z < 0.
            if (v.z >= -0.05f) continue;  // behind camera or right on the plane

            double ndcX = (double) v.x / (-v.z * tanHalfFov * aspect);
            double ndcY = (double) v.y / (-v.z * tanHalfFov);
            int sx = (int)(viewW / 2.0 + ndcX * viewW / 2.0);
            int sy = (int)(viewH / 2.0 - ndcY * viewH / 2.0);  // Y flipped to screen-down

            if (sx < minX) minX = sx;
            if (sy < minY) minY = sy;
            if (sx > maxX) maxX = sx;
            if (sy > maxY) maxY = sy;
            any = true;
        }

        if (!any) return null;
        // Reject boxes fully off-screen
        if (maxX < 0 || maxY < 0 || minX > viewW || minY > viewH) return null;
        // Clamp to screen so off-screen overhang doesn't draw absurdly long lines
        minX = Math.max(minX, 0);
        minY = Math.max(minY, 0);
        maxX = Math.min(maxX, viewW);
        maxY = Math.min(maxY, viewH);
        if (maxX - minX < 1 || maxY - minY < 1) return null;
        return new int[]{minX, minY, maxX, maxY};
    }

    /** Draw a 1px rectangle outline via GuiGraphics fill calls. */
    private static void drawOutline(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1,     y1,     x2,     y1 + 1, color);  // top
        g.fill(x1,     y2 - 1, x2,     y2,     color);  // bottom
        g.fill(x1,     y1,     x1 + 1, y2,     color);  // left
        g.fill(x2 - 1, y1,     x2,     y2,     color);  // right
    }
}
