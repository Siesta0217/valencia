package com.nofall;

import net.minecraft.client.Minecraft;

public class StepMod {
    private static boolean enabled = false;

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    public static boolean isActive() {
        if (!enabled) return false;
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null;
    }
}
