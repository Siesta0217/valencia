package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

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

    /** Dimension the target was set in (Object since ResourceKey isn't on the
     *  compile classpath in this Lunar build — Object#equals handles it). */
    public static Object  targetDim = null;

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
        Minecraft mc = Minecraft.getInstance();
        targetDim = (mc.level != null) ? mc.level.dimension() : null;
        hasTarget = true;
        if (!enabled) enabled = true;
    }

    public static void stop() {
        enabled = false;
        hasTarget = false;
        targetDim = null;
    }

    public static boolean inWrongDimension() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || targetDim == null) return false;
        return !targetDim.equals(mc.level.dimension());
    }

    public static boolean isNether() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        return mc.level.dimension().toString().contains("the_nether");
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
        if (p == null || mc.level == null) return;

        // Death / low HP abort — stops further packets being sent while dying
        if (!p.isAlive() || p.isDeadOrDying()) {
            p.displayClientMessage(Component.literal("§c[Goto] died — auto-pilot off"), false);
            stop();
            return;
        }
        if (p.getHealth() <= 4.0f) {  // 2 hearts
            p.displayClientMessage(Component.literal("§e[Goto] HP low — auto-pilot off"), false);
            stop();
            return;
        }

        // Dimension mismatch — pause (don't clear, in case user portals back)
        if (inWrongDimension()) {
            p.displayClientMessage(Component.literal(
                "§e[Goto] wrong dimension — paused, portal back to resume"), true);
            return;
        }

        double dx = targetX - p.getX();
        double dz = targetZ - p.getZ();
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        // Arrived
        if (horizDist < 8) {
            showStatus(p, horizDist, true);
            stop();
            return;
        }

        // Aim yaw at the target on every tick
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        p.setYRot(yaw);
        p.setYHeadRot(yaw);

        // Distance-banded base pitch (cruise → descend → dive)
        float pitch;
        if (horizDist > 200)      pitch = -3f;
        else if (horizDist > 50)  pitch = 12f;
        else                      pitch = 28f;

        // Collision avoidance — overrides base pitch. Priority: ground > forward > ceiling.
        if (groundTooClose(p, mc, 6)) {
            pitch = -25f;                       // pull up hard
        } else if (obstacleAhead(p, mc, 14)) {
            pitch = -30f;                       // mountain/wall ahead → climb
        } else if (isNether() && ceilingTooClose(p, mc, 4)) {
            pitch = 20f;                        // nether ceiling → push down
        }

        p.setXRot(pitch);

        // Deploy elytra if airborne and falling but not gliding
        if (!p.isFallFlying() && !p.onGround() && p.getDeltaMovement().y < 0) {
            try { p.tryToStartFallFlying(); } catch (Throwable ignored) {}
        }

        // Auto-rocket while gliding and still far from destination
        if (p.isFallFlying() && horizDist > 40 && rocketCooldown <= 0) {
            if (fireRocket(p, mc)) rocketCooldown = 60;
        }
        if (rocketCooldown > 0) rocketCooldown--;

        showStatus(p, horizDist, false);
    }

    /** True if a solid block is within {@code maxDist} along the horizontal heading. */
    private static boolean obstacleAhead(LocalPlayer p, Minecraft mc, double maxDist) {
        float yawRad = (float) Math.toRadians(p.getYRot());
        Vec3 from = p.getEyePosition();
        Vec3 dir  = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
        Vec3 to   = from.add(dir.scale(maxDist));
        HitResult hit = mc.level.clip(new ClipContext(
            from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.BLOCK;
    }

    private static boolean groundTooClose(LocalPlayer p, Minecraft mc, double maxDist) {
        Vec3 from = p.position();
        Vec3 to   = from.add(0, -maxDist, 0);
        HitResult hit = mc.level.clip(new ClipContext(
            from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.BLOCK;
    }

    private static boolean ceilingTooClose(LocalPlayer p, Minecraft mc, double maxDist) {
        Vec3 from = p.position().add(0, p.getBbHeight(), 0);
        Vec3 to   = from.add(0, maxDist, 0);
        HitResult hit = mc.level.clip(new ClipContext(
            from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.BLOCK;
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
        if (done) {
            p.displayClientMessage(Component.literal(
                String.format("§a[Goto] arrived (%.0fm)", dist)), true);
            return;
        }
        double etaSec = dist / 33.0;  // ~33 blocks/sec sustained elytra speed
        int px = (int) p.getX(), pz = (int) p.getZ();
        int tx = (int) targetX,  tz = (int) targetZ;

        // Show the other dimension's equivalent coord — divide by 8 when in
        // overworld, multiply by 8 when in nether — so user can decide if
        // portalling would be shorter.
        String other;
        if (isNether()) {
            other = String.format("§7overworld §f%d,%d→%d,%d", px * 8, pz * 8, tx * 8, tz * 8);
        } else {
            other = String.format("§7nether §f%d,%d→%d,%d", px / 8, pz / 8, tx / 8, tz / 8);
        }
        String msg = String.format("§b[Goto] §f%.0fm  §7ETA §f%.0fs  §8| §7now §f%d,%d  §8| %s",
            dist, etaSec, px, pz, other);
        p.displayClientMessage(Component.literal(msg), true);
    }
}
