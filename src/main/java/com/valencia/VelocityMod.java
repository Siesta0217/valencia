package com.valencia;

import net.minecraft.client.Minecraft;

public class VelocityMod {
    private static boolean enabled = false;

    // Knockback strength scale — applied to the delta vanilla computes.
    // 0   = full immunity (don't move at all on hit)
    // 100 = vanilla behavior
    // 200 = double knockback (fun in PvP, less so when sniped off a bridge)
    public static int horizontal = 0;
    public static int vertical   = 0;

    public static boolean isEnabled() { return enabled; }
    public static void toggle() { enabled = !enabled; }
    public static boolean isActive() {
        if (!enabled) return false;
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null;
    }
}
