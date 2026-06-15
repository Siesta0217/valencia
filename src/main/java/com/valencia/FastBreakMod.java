package com.valencia;

import net.minecraft.client.Minecraft;

/**
 * FastBreak — break the single block you're mining as fast as the client lets
 * us, by maxing the client-side destroy progress each tick (see FastBreakMixin).
 *
 * Reality check: the SERVER independently validates break time. On a vanilla /
 * anti-cheat server it will reject an early break and resend the block (it
 * "grows back" = ghost block). This is fully effective in singleplayer / on
 * lenient servers, and harmless (just ineffective) on strict ones.
 */
public final class FastBreakMod {

    private static boolean enabled = false;

    private FastBreakMod() {}

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    public static boolean isActive() {
        if (!enabled) return false;
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null && mc.gameMode != null;
    }
}
