package com.valencia;

import net.minecraft.client.Minecraft;

/**
 * AutoWalk — holds the forward key so you keep walking hands-free (tunnel
 * mining, AFK travel). Drives {@code Options.keyUp} through the normal input
 * system, so it respects open screens and Freecam's input freeze automatically.
 */
public final class AutoWalkMod {

    private static boolean enabled = false;
    private static boolean holding = false;

    private AutoWalkMod() {}

    public static boolean isEnabled() { return enabled; }
    public static void toggle() { enabled = !enabled; if (!enabled) release(); }

    public static boolean isActive() {
        if (!enabled) return false;
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        boolean want = isActive() && mc.screen == null;
        if (want) { mc.options.keyUp.setDown(true); holding = true; }
        else if (holding) release();
    }

    private static void release() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) mc.options.keyUp.setDown(false);
        holding = false;
    }
}
