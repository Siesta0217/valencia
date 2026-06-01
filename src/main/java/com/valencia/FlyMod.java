package com.valencia;

import net.minecraft.client.Minecraft;

/**
 * Motion-based creative-style flight that works on servers.
 *
 * Sets the player's velocity directly every tick from look direction + input
 * (WASD horizontal, Space up, L-Shift down). We can't use the vanilla
 * {@code Abilities.flying} flag on a survival server — the server owns abilities
 * and resyncs them away — so this drives {@code setDeltaMovement} instead, done
 * in {@link com.valencia.mixin.FlyMixin} at {@code aiStep} HEAD.
 *
 * On a vanilla server (allow-flight=false) sustained flight trips the flight
 * kick; pair with NoFall (AntiKick / Always) to stay airborne.
 */
public final class FlyMod {

    private static boolean enabled = false;

    public static float hSpeed = 1.0f;   // horizontal blocks per tick
    public static float vSpeed = 1.0f;   // vertical blocks per tick

    private FlyMod() {}

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    public static boolean isActive() {
        if (!enabled) return false;
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null;
    }
}
