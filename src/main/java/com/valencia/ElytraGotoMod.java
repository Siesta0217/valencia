package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Auto-pilot for elytra: lock yaw to a target XYZ, manage pitch, and auto-fire
 * firework rockets from main/off hand. Every action it performs is a vanilla
 * input (rotation, useItem, tryToStartFallFlying), so a server can't tell it
 * apart from a real player flying.
 *
 * Set target via chat command `.nf goto <x> <y> <z>` — that command toggles
 * the module on. `.nf goto stop` turns it off.
 */
public class ElytraGotoMod {

    private static boolean enabled = false;

    public static double  targetX = 0, targetY = 64, targetZ = 0;
    public static boolean hasTarget = false;

    /** Ticks until we may fire another rocket. */
    public static int rocketCooldown = 0;

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    public static boolean isActive() {
        if (!enabled || !hasTarget) return false;
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null;
    }

    public static void setTarget(double x, double y, double z) {
        targetX = x; targetY = y; targetZ = z;
        hasTarget = true;
        if (!enabled) enabled = true;
    }

    public static void stop() {
        enabled = false;
        hasTarget = false;
    }

    public static double horizontalDistance() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;
        double dx = targetX - mc.player.getX();
        double dz = targetZ - mc.player.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static void tick() {
        if (!isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;

        double dx = targetX - p.getX();
        double dz = targetZ - p.getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        // Arrived (or near enough that the player should land manually)
        if (horizDist < 8) {
            showStatus(p, horizDist, true);
            stop();
            return;
        }

        // Aim yaw at the target on every tick
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        p.setYRot(yaw);
        p.setYHeadRot(yaw);

        // Pitch: cruise level when far, descend / dive when closer to target
        float pitch;
        if (horizDist > 200)      pitch = -3f;    // slight up to maintain altitude
        else if (horizDist > 50)  pitch = 12f;    // gentle descent
        else                      pitch = 28f;    // dive toward target
        p.setXRot(pitch);

        // If we're airborne but not gliding yet, try to deploy elytra
        if (!p.isFallFlying() && !p.onGround() && p.getDeltaMovement().y < 0) {
            try { p.tryToStartFallFlying(); } catch (Throwable ignored) {}
        }

        // Auto-rocket while gliding and still far from destination
        if (p.isFallFlying() && horizDist > 40 && rocketCooldown <= 0) {
            if (fireRocket(p, mc)) rocketCooldown = 60;  // ~3 sec, one rocket cycle
        }
        if (rocketCooldown > 0) rocketCooldown--;

        showStatus(p, horizDist, false);
    }

    private static boolean fireRocket(LocalPlayer p, Minecraft mc) {
        if (mc.gameMode == null) return false;

        ItemStack main = p.getMainHandItem();
        ItemStack off  = p.getOffhandItem();

        InteractionHand hand;
        if (main.is(Items.FIREWORK_ROCKET))      hand = InteractionHand.MAIN_HAND;
        else if (off.is(Items.FIREWORK_ROCKET))  hand = InteractionHand.OFF_HAND;
        else return false;

        try {
            mc.gameMode.useItem(p, hand);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static void showStatus(LocalPlayer p, double dist, boolean done) {
        int nx = (int)(p.getX() / 8);
        int nz = (int)(p.getZ() / 8);
        int ntx = (int)(targetX / 8);
        int ntz = (int)(targetZ / 8);
        String msg;
        if (done) {
            msg = String.format("§a[Goto] arrived (%.0fm)", dist);
        } else {
            double etaSec = dist / 33.0;  // ~33 blocks/sec sustained elytra speed
            msg = String.format("§b[Goto] §f%.0fm  §7ETA §f%.0fs  §8| §7now §f%d,%d §8| §7nether §f%d,%d→%d,%d",
                dist, etaSec, (int)p.getX(), (int)p.getZ(), nx, nz, ntx, ntz);
        }
        p.displayClientMessage(Component.literal(msg), true);  // action bar
    }
}
