package com.valencia;

import net.minecraft.client.Minecraft;

public class BHopMod {
    private static boolean enabled = false;
    public static float speedMultiplier = 1.0f;

    /** LowHop: scale jump Y velocity so the player barely leaves the ground. */
    public static boolean lowHop     = false;
    /** Y velocity factor when lowHop is on (0.1 = tiny hop, 1.0 = vanilla). */
    public static float   jumpHeight = 0.5f;

    /** Boost: per-jump horizontal velocity multiplier — compounds across hops. */
    public static float   boost      = 1.0f;

    /** KB Boost: redirect incoming knockback to forward look direction. */
    public static boolean kbBoost    = false;

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    public static boolean isActive() {
        if (!enabled) return false;
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null;
    }
}
