package com.valencia;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class ESPRenderer {

    public static void render(GuiGraphics g) {
        if (!ESPMod.isEnabled()) return;
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

        int color = ESPMod.boxColor();

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!ESPMod.targets(e)) continue;

            AABB box = e.getBoundingBox();
            int[] rect = projectAABB(box, camPos, invRot, tanHalfFov, aspect, viewW, viewH);
            if (rect == null) continue;

            int x1 = rect[0], y1 = rect[1], x2 = rect[2], y2 = rect[3];

            g.fill(x1, y1, x2, y1 + 1, color);
            g.fill(x1, y2 - 1, x2, y2, color);
            g.fill(x1, y1, x1 + 1, y2, color);
            g.fill(x2 - 1, y1, x2, y2, color);
        }
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
        boolean any = false;

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
            any = true;
        }

        return any ? new int[]{minX, minY, maxX, maxY} : null;
    }
}
