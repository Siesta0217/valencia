package com.valencia;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

/**
 * World-to-screen projection backed by vanilla's own
 * {@link GameRenderer#projectPointToScreen(Vec3)}.
 *
 * Why this exists: previous revisions did the camera-relative + invRot +
 * perspective math by hand. The math was correct on paper but the result
 * never lined up in this Lunar 1.21.11 build — likely because the camera
 * orientation / projection matrix returned by Mojang here doesn't compose
 * exactly the way upstream Yarn-mapped 1.21 does. The fix is to stop
 * fighting it: vanilla already projects world points to NDC for its own
 * waypoint system (see {@code TrackedWaypoint.Projector}), and exposing
 * that path makes our overlays use the same matrix vanilla actually
 * renders the world with.
 *
 * Vanilla returns NDC-style coords where:
 *   x ∈ [-1, +1]  left → right
 *   y ∈ [-1, +1]  bottom → top  (OpenGL convention)
 *   z              depth, sign meaningless when point is behind camera
 *
 * Behind-camera detection is done independently via a forward-vector
 * dot product — NDC alone can't tell us "behind" because the perspective
 * divide flips sign and gives a plausible-looking but wrong screen coord.
 */
public final class Projection {

    /** Minimum forward distance, in blocks, for a point to count as "in front". */
    private static final double NEAR_FORWARD_BLOCKS = 0.05;

    /** Coordinates this far off-screen are pathological; clamp to keep ints sane. */
    private static final int SCREEN_CLAMP = 100_000;

    private Projection() {}

    public static final class Frame {
        public final Minecraft mc;
        public final GameRenderer gameRenderer;
        public final Camera camera;
        public final double camX, camY, camZ;
        /** Camera forward, unit vector. Used for behind-camera rejection. */
        public final double fwdX, fwdY, fwdZ;
        public final int viewW, viewH;

        private Frame(Minecraft mc) {
            this.mc = mc;
            this.gameRenderer = mc.gameRenderer;
            this.camera = mc.gameRenderer.getMainCamera();
            Vec3 pos = camera.position();
            this.camX = pos.x; this.camY = pos.y; this.camZ = pos.z;
            Vector3fc fwd = camera.forwardVector();
            this.fwdX = fwd.x(); this.fwdY = fwd.y(); this.fwdZ = fwd.z();
            Window win = mc.getWindow();
            this.viewW = win.getGuiScaledWidth();
            this.viewH = win.getGuiScaledHeight();
        }
    }

    /** Snapshot the current camera + viewport for this HUD render. */
    public static Frame begin(float partialTick) {
        return new Frame(Minecraft.getInstance());
    }

    /**
     * Project a world-space point. Returns false if behind the camera
     * (forward distance < NEAR_FORWARD_BLOCKS) or off-screen by more
     * than the clamp range.
     */
    public static boolean projectPoint(Frame f, double wx, double wy, double wz, int[] out2) {
        double dx = wx - f.camX;
        double dy = wy - f.camY;
        double dz = wz - f.camZ;
        double forward = dx * f.fwdX + dy * f.fwdY + dz * f.fwdZ;
        if (forward < NEAR_FORWARD_BLOCKS) return false;

        Vec3 ndc = f.gameRenderer.projectPointToScreen(new Vec3(wx, wy, wz));
        out2[0] = clampScreen((ndc.x + 1.0) * 0.5 * f.viewW);
        out2[1] = clampScreen((1.0 - ndc.y) * 0.5 * f.viewH);
        return true;
    }

    /**
     * 2D-projected representation of a 3D AABB.
     * Reusable — every projectAabb() overwrites in place.
     */
    public static final class ScreenAabb {
        /** True if at least one of the 8 corners is in front of the camera. */
        public boolean visible;

        /** Tight 2D bounding rect over all visible corners (+ clipped edge midpoints). */
        public int minX, minY, maxX, maxY;

        /** 12 edges × {ax, ay, bx, by}. Only entries flagged in validMask are drawable. */
        public final int[] edges = new int[48];

        /** Bit i set ⇒ edges[i*4..i*4+3] is a drawable line segment. */
        public int validMask;
    }

    /**
     * Project an AABB. Strategy:
     *   1. For each of 8 corners, compute forward-distance + project to
     *      screen if in front. Mark visibility per corner.
     *   2. For each of 12 edges, if both endpoints visible → draw as-is.
     *      If exactly one visible → clip the world-space edge against the
     *      camera near plane and project the clipped endpoint.
     *      If neither visible → skip.
     *   3. Tight 2D rect = min/max over all (clipped) projected endpoints.
     */
    public static void projectAabb(Frame f, AABB box, ScreenAabb out) {
        // Build 8 corners. Bit 2 = X high, bit 1 = Y high, bit 0 = Z high.
        for (int i = 0; i < 8; i++) {
            CORNER_WX[i] = (i & 4) != 0 ? box.maxX : box.minX;
            CORNER_WY[i] = (i & 2) != 0 ? box.maxY : box.minY;
            CORNER_WZ[i] = (i & 1) != 0 ? box.maxZ : box.minZ;
        }

        // Project visible corners, record forward-distance for clipping.
        boolean anyVisible = false;
        for (int i = 0; i < 8; i++) {
            double dx = CORNER_WX[i] - f.camX;
            double dy = CORNER_WY[i] - f.camY;
            double dz = CORNER_WZ[i] - f.camZ;
            double fwd = dx * f.fwdX + dy * f.fwdY + dz * f.fwdZ;
            CORNER_FWD[i] = fwd;
            if (fwd >= NEAR_FORWARD_BLOCKS) {
                Vec3 ndc = f.gameRenderer.projectPointToScreen(
                    new Vec3(CORNER_WX[i], CORNER_WY[i], CORNER_WZ[i])
                );
                CORNER_SX[i] = clampScreen((ndc.x + 1.0) * 0.5 * f.viewW);
                CORNER_SY[i] = clampScreen((1.0 - ndc.y) * 0.5 * f.viewH);
                CORNER_VIS[i] = true;
                anyVisible = true;
            } else {
                CORNER_VIS[i] = false;
            }
        }

        out.validMask = 0;
        out.visible = false;
        out.minX = Integer.MAX_VALUE;
        out.minY = Integer.MAX_VALUE;
        out.maxX = Integer.MIN_VALUE;
        out.maxY = Integer.MIN_VALUE;
        if (!anyVisible) return;

        int[] e = out.edges;
        for (int i = 0; i < 12; i++) {
            int ai = AABB_EDGES[i * 2];
            int bi = AABB_EDGES[i * 2 + 1];
            int o = i * 4;
            int ax, ay, bx, by;

            if (CORNER_VIS[ai] && CORNER_VIS[bi]) {
                ax = CORNER_SX[ai]; ay = CORNER_SY[ai];
                bx = CORNER_SX[bi]; by = CORNER_SY[bi];
            } else if (CORNER_VIS[ai] || CORNER_VIS[bi]) {
                // Clip in world space against the camera near plane.
                int frontIdx = CORNER_VIS[ai] ? ai : bi;
                int backIdx  = CORNER_VIS[ai] ? bi : ai;
                double fFront = CORNER_FWD[frontIdx];
                double fBack  = CORNER_FWD[backIdx];
                // Lerp factor along edge so that forward-distance == NEAR.
                double t = (fFront - NEAR_FORWARD_BLOCKS) / (fFront - fBack);
                if (t < 0.0 || t > 1.0) continue;
                double cx = CORNER_WX[frontIdx] + (CORNER_WX[backIdx] - CORNER_WX[frontIdx]) * t;
                double cy = CORNER_WY[frontIdx] + (CORNER_WY[backIdx] - CORNER_WY[frontIdx]) * t;
                double cz = CORNER_WZ[frontIdx] + (CORNER_WZ[backIdx] - CORNER_WZ[frontIdx]) * t;
                Vec3 ndc = f.gameRenderer.projectPointToScreen(new Vec3(cx, cy, cz));
                int csx = clampScreen((ndc.x + 1.0) * 0.5 * f.viewW);
                int csy = clampScreen((1.0 - ndc.y) * 0.5 * f.viewH);
                ax = CORNER_SX[frontIdx]; ay = CORNER_SY[frontIdx];
                bx = csx;                 by = csy;
            } else {
                continue;
            }

            e[o]     = ax;
            e[o + 1] = ay;
            e[o + 2] = bx;
            e[o + 3] = by;
            out.validMask |= 1 << i;
            out.visible = true;
            if (ax < out.minX) out.minX = ax;
            if (ax > out.maxX) out.maxX = ax;
            if (ay < out.minY) out.minY = ay;
            if (ay > out.maxY) out.maxY = ay;
            if (bx < out.minX) out.minX = bx;
            if (bx > out.maxX) out.maxX = bx;
            if (by < out.minY) out.minY = by;
            if (by > out.maxY) out.maxY = by;
        }
    }

    // ─── internals ──────────────────────────────────────────────────────

    /** Edge endpoints by corner index (bit-packed). 12 edges × 2 corners. */
    private static final int[] AABB_EDGES = {
        0,1, 0,2, 0,4, 1,3, 1,5, 2,3, 2,6, 3,7, 4,5, 4,6, 5,7, 6,7
    };

    // Per-frame scratch — HUD render is single-threaded.
    private static final double[] CORNER_WX  = new double[8];
    private static final double[] CORNER_WY  = new double[8];
    private static final double[] CORNER_WZ  = new double[8];
    private static final double[] CORNER_FWD = new double[8];
    private static final int[]    CORNER_SX  = new int[8];
    private static final int[]    CORNER_SY  = new int[8];
    private static final boolean[] CORNER_VIS = new boolean[8];

    private static int clampScreen(double v) {
        if (v < -SCREEN_CLAMP) return -SCREEN_CLAMP;
        if (v >  SCREEN_CLAMP) return  SCREEN_CLAMP;
        return (int) v;
    }
}
