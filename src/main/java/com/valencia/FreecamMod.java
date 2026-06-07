package com.valencia;

import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * Freecam — detach the camera from the body and fly it around, through walls,
 * to scout. Entirely client-side: the body stays frozen where you left it and
 * no extra packets are sent, so it's undetectable by any server/anti-cheat.
 *
 * Pieces:
 *  - {@link com.valencia.mixin.FreecamCameraMixin} overrides the camera position
 *    (rotation stays the player's, so the mouse still aims the freecam).
 *  - {@link com.valencia.mixin.ClientInputMixin} zeroes movement input while
 *    active, so WASD drives only the freecam and the body doesn't walk off.
 *  - {@link #tick()} (from TickMixin) flies the camera from raw key state.
 *
 * Camera is forced to third-person while active so you can see your own body
 * and don't get floating first-person hands.
 */
public final class FreecamMod {

    private static boolean enabled = false;
    public static float speed = 1.0f;        // blocks per tick

    public static double x, y, z;            // camera world position
    private static CameraType prevView;      // restored on disable

    private FreecamMod() {}

    public static boolean isEnabled() { return enabled; }

    public static boolean isActive() {
        if (!enabled) return false;
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null;
    }

    public static void toggle() {
        Minecraft mc = Minecraft.getInstance();
        enabled = !enabled;
        if (enabled) {
            if (mc.player != null) {
                x = mc.player.getX();
                y = mc.player.getEyeY();
                z = mc.player.getZ();
            }
            if (mc.options != null) {
                prevView = mc.options.getCameraType();
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            }
        } else if (mc.options != null && prevView != null) {
            mc.options.setCameraType(prevView);
        }
    }

    /** Fly the detached camera from raw WASD / Space / Shift, relative to look yaw. */
    public static void tick() {
        if (!isActive()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;
        long h = GLFW.glfwGetCurrentContext();
        if (h == 0L) return;

        boolean w  = down(h, GLFW.GLFW_KEY_W),     s  = down(h, GLFW.GLFW_KEY_S);
        boolean a  = down(h, GLFW.GLFW_KEY_A),     d  = down(h, GLFW.GLFW_KEY_D);
        boolean up = down(h, GLFW.GLFW_KEY_SPACE), dn = down(h, GLFW.GLFW_KEY_LEFT_SHIFT);

        float yr = (float) Math.toRadians(mc.player.getYRot());
        double sinY = Math.sin(yr), cosY = Math.cos(yr);
        double mx = 0, mz = 0;
        if (w) { mx -= sinY; mz += cosY; }
        if (s) { mx += sinY; mz -= cosY; }
        if (d) { mx -= cosY; mz -= sinY; }
        if (a) { mx += cosY; mz += sinY; }

        double len = Math.sqrt(mx * mx + mz * mz);
        if (len > 0.001) { x += mx / len * speed; z += mz / len * speed; }
        if (up) y += speed;
        if (dn) y -= speed;
    }

    private static boolean down(long h, int key) {
        return GLFW.glfwGetKey(h, key) == GLFW.GLFW_PRESS;
    }
}
