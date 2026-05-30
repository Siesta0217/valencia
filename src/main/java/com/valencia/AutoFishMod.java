package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Auto-fishing: watch the local player's fishing bobber for the bite signal
 * (sudden downward velocity on the {@link FishingHook}), right-click to reel
 * in, then right-click again after a short delay to recast.
 *
 * Both right-clicks go through {@code MultiPlayerGameMode#useItem}, the exact
 * same path a human right-click takes — server can't distinguish.
 */
public class AutoFishMod {

    private static boolean enabled = false;

    /** Velocity Y threshold for "fish bit" — bobber sinks fast when triggered. */
    public static float biteVy = -0.04f;

    /** Ticks to wait after reel before re-casting. */
    public static int recastDelay = 12;

    /** Ticks of cooldown after a right-click (prevents double-fire on lag). */
    private static int actionCooldown = 0;
    private static int recastTimer    = 0;

    /** Log a failed useItem at most once so a broken rod/use doesn't spam. */
    private static boolean loggedError = false;

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    public static boolean isActive() {
        if (!enabled) return false;
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null;
    }

    public static void tick() {
        if (!isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.gameMode == null) return;

        // Must be holding a fishing rod
        InteractionHand hand;
        if (p.getMainHandItem().is(Items.FISHING_ROD))     hand = InteractionHand.MAIN_HAND;
        else if (p.getOffhandItem().is(Items.FISHING_ROD)) hand = InteractionHand.OFF_HAND;
        else return;

        if (actionCooldown > 0) { actionCooldown--; return; }

        // Pending recast after a reel
        if (recastTimer > 0) {
            recastTimer--;
            if (recastTimer == 0) {
                rightClick(mc, p, hand);
                actionCooldown = 6;
            }
            return;
        }

        FishingHook hook = p.fishing;
        if (hook == null) {
            // No active bobber — recast (covers initial start + lost bobber)
            rightClick(mc, p, hand);
            actionCooldown = 10;
            return;
        }

        // Bite detection: bobber's Y velocity drops below threshold
        Vec3 v = hook.getDeltaMovement();
        if (v.y < biteVy) {
            rightClick(mc, p, hand);         // reel in
            actionCooldown = 4;
            recastTimer    = recastDelay;    // schedule recast
        }
    }

    private static void rightClick(Minecraft mc, LocalPlayer p, InteractionHand hand) {
        try {
            mc.gameMode.useItem(p, hand);
        } catch (Throwable t) {
            if (!loggedError) {
                loggedError = true;
                System.err.println("[Valencia] AutoFish useItem failed, disabling further log: " + t);
            }
        }
    }
}
