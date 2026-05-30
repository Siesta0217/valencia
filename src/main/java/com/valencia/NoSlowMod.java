package com.valencia;

import net.minecraft.client.Minecraft;

public class NoSlowMod {
    private static boolean enabled = false;
    public static boolean isEnabled() { return enabled; }
    public static void toggle() { enabled = !enabled; }

    /** Consistent with the other modules: enabled and in a live world. The
     *  NoSlow redirects only fire during player ticks, so the player/level
     *  guards are always satisfied there — this just matches the shared shape. */
    public static boolean isActive() {
        if (!enabled) return false;
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null;
    }
}
