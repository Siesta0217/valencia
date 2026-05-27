package com.valencia;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * World-to-screen projection for HUD overlays.
 *
 * Coordinate spaces:
 *   World  — Minecraft level coords.
 *   View   — World minus camera, rotated by inverse camera orientation.
 *            Forward is -Z, up is +Y, right is +X.
 *   Screen — GUI-scaled pixels. Origin top-left, +Y down.
 *
 * Every HUD overlay should call begin() once per frame, then reuse the
 * returned Frame for all per-entity projections. Projection helpers handle
 * near-plane clipping; callers never deal with raw view-space math.
 *
 * Thread model: the HUD render path is single-threaded. Internal scratch
 * is shared via static fields and is NOT safe for concurrent calls.
 */
public final class Projection {

    /** Anything with view-space z >= NEAR_Z is behind / on the near plane. */
    public static final float NEAR_Z = -0.05f;

    /** Coordinates this far off-screen are pathological; caller should skip. */
    private static final int SCREEN_CLAMP = 100_000;

    private Projection() {}

    public static final class Frame {
        public final double camX, camY, camZ;
        public final Quaternionf invRot;
        public final double m00, m11;
        public final int viewW, viewH;
        public final float halfW, halfH;

        private Frame(double camX, double camY, double camZ,
                      Quaternionf invRot,
                      double m00, double m11,
                      int viewW, int viewH) {
            this.camX = camX; this.camY = camY; this.camZ = camZ;
            this.invRot = invRot;
            this.m00 = m00; this.m11 = m11;
            this.viewW = viewW; this.viewH = viewH;
            this.halfW = viewW * 0.5f;
            this.halfH = viewH * 0.5f;
        }
    }

    /** Snapshot the current camera and projection for this frame. */
    public static Frame begin(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 cam = camera.position();
        Quaternionf invRot = new Quaternionf(camera.rotation()).conjugate();
        Matrix4f proj = mc.gameRenderer.getProjectionMatrix(partialTick);
        Window win = mc.getWindow();
        return new Frame(
            cam.x, cam.y, cam.z,
            invRot,
            proj.m00(), proj.m11(),
            win.getGuiScaledWidth(), win.getGuiScaledHeight()
        );
    }

    /** Project a world-space point. Returns false if behind the near plane. */
    public static boolean projectPoint(Frame f, double wx, double wy, double wz, int[] out2) {
        Vector3f v = POINT_SCRATCH;
        v.set((float)(wx - f.camX), (float)(wy - f.camY), (float)(wz - f.camZ));
        v.rotate(f.invRot);
        if (v.z >= NEAR_Z) return false;
        double invNegZ = 1.0 / -v.z;
        out2[0] = clampScreen(f.halfW + (v.x * f.m00 * invNegZ) * f.halfW);
        out2[1] = clampScreen(f.halfH - (v.y * f.m11 * invNegZ) * f.halfH);
        return true;
    }

    /**
     * 2D-projected representation of a 3D AABB.
     * Reuse one instance per renderer to avoid GC churn — the same object
     * is rewritten by every projectAabb() call.
     */
    public static final class ScreenAabb {
        /** True if at least one edge survives near-plane clipping. */
        public boolean visible;

        /** Tight 2D bounding rect of all visible (clipped) edge endpoints. */
        public int minX, minY, maxX, maxY;

        /** 12 edges × {ax, ay, bx, by}. Only entries flagged in validMask are drawable. */
        public final int[] edges = new int[48];

        /** Bit i set ⇒ edges[i*4 .. i*4+3] is a drawable line segment. */
        public int validMask;
    }

    /**
     * Project an AABB into screen space.
     *
     * Method: compute 8 view-space corners → clip each of the 12 edges
     * against the near plane → project both endpoints → record edge and
     * extend the 2D bounding rect with both endpoints. An edge fully
     * behind the near plane is skipped entirely.
     *
     * Clipping each edge separately is correct for both the per-edge
     * "Hitbox" style and the rect-only "Corners/Outline/Filled" styles:
     * the union of clipped endpoints is the tight 2D bound of the
     * intersection of the box with the visible half-space.
     */
    public static void projectAabb(Frame f, AABB box, ScreenAabb out) {
        Vector3f[] c = CORNER_SCRATCH;
        for (int i = 0; i < 8; i++) {
            double x = (i & 4) != 0 ? box.maxX : box.minX;
            double y = (i & 2) != 0 ? box.maxY : box.minY;
            double z = (i & 1) != 0 ? box.maxZ : box.minZ;
            Vector3f v = c[i];
            v.set((float)(x - f.camX), (float)(y - f.camY), (float)(z - f.camZ));
            v.rotate(f.invRot);
        }

        out.validMask = 0;
        out.visible = false;
        out.minX = Integer.MAX_VALUE;
        out.minY = Integer.MAX_VALUE;
        out.maxX = Integer.MIN_VALUE;
        out.maxY = Integer.MIN_VALUE;

        int[] e = out.edges;
        for (int i = 0; i < 12; i++) {
            int ai = AABB_EDGES[i * 2];
            int bi = AABB_EDGES[i * 2 + 1];
            int o = i * 4;
            if (!clipAndProjectEdge(f, c[ai], c[bi], e, o)) continue;
            out.validMask |= 1 << i;
            out.visible = true;
            if (e[o]     < out.minX) out.minX = e[o];
            if (e[o]     > out.maxX) out.maxX = e[o];
            if (e[o + 1] < out.minY) out.minY = e[o + 1];
            if (e[o + 1] > out.maxY) out.maxY = e[o + 1];
            if (e[o + 2] < out.minX) out.minX = e[o + 2];
            if (e[o + 2] > out.maxX) out.maxX = e[o + 2];
            if (e[o + 3] < out.minY) out.minY = e[o + 3];
            if (e[o + 3] > out.maxY) out.maxY = e[o + 3];
        }
    }

    // ─── internals ──────────────────────────────────────────────────────

    /** Corner index bits — bit 2 = X high, bit 1 = Y high, bit 0 = Z high. */
    private static final int[] AABB_EDGES = {
        0,1, 0,2, 0,4, 1,3, 1,5, 2,3, 2,6, 3,7, 4,5, 4,6, 5,7, 6,7
    };

    private static final Vector3f[] CORNER_SCRATCH = new Vector3f[8];
    private static final Vector3f POINT_SCRATCH = new Vector3f();
    static { for (int i = 0; i < 8; i++) CORNER_SCRATCH[i] = new Vector3f(); }

    /** Clip edge a→b against view-space z = NEAR_Z, project, write 4 ints at edges[off..off+3]. */
    private static boolean clipAndProjectEdge(Frame f, Vector3f a, Vector3f b, int[] edges, int off) {
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

        edges[off]     = projectX(f, ax, az);
        edges[off + 1] = projectY(f, ay, az);
        edges[off + 2] = projectX(f, bx, bz);
        edges[off + 3] = projectY(f, by, bz);
        return true;
    }

    private static int projectX(Frame f, float vx, float vz) {
        return clampScreen(f.halfW + (vx * f.m00 / -vz) * f.halfW);
    }

    private static int projectY(Frame f, float vy, float vz) {
        return clampScreen(f.halfH - (vy * f.m11 / -vz) * f.halfH);
    }

    private static int clampScreen(double v) {
        if (v < -SCREEN_CLAMP) return -SCREEN_CLAMP;
        if (v >  SCREEN_CLAMP) return  SCREEN_CLAMP;
        return (int) v;
    }
}
