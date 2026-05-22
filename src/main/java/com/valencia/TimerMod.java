package com.valencia;

import net.minecraft.client.Minecraft;

public class TimerMod {

    private static boolean enabled = false;

    /** Speed multiplier: 1.0 = vanilla, 2.0 = double player tick rate, etc. */
    public static float speed = 2.0f;

    /** Carry-over for fractional speeds (e.g. 1.5 -> tick once extra every 2 ticks). */
    public static float accumulator = 0.0f;

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    public static boolean isActive() {
        if (!enabled) return false;
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null;
    }
}
