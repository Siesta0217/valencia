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
 * Auto-pilot for elytra: lock yaw to a target XYZ, manage pitch with terrain
 * avoidance, and auto-fire firework rockets from main/off hand. Every action
 * is a vanilla input (rotation, useItem, tryToStartFallFlying), so a server
 * can't tell it apart from a real player flying.
 *
 * Set target via chat: `.nf goto <x> [y] <z>`. Stop with `.nf goto stop`.
 *
 * Safety layers (in priority order, each can override the base pitch):
 *  - Death / HP &lt;= safeHpThreshold: full stop
 *  - Wrong dimension: pause (keeps target for re-entry)
 *  - Ground / lava within 7m below: pitch -25
 *  - Obstacle / fluid within (15 + speed*12) ahead, 5-ray fan: pitch -30
 *  - Nether ceiling within 4m: pitch +20
 *  - Below safe altitude (Y &lt; 120 overworld / 80 nether): pitch -10
 *  - Otherwise: distance-banded base pitch
 */
public class ElytraGotoMod {

    private static boolean enabled = false;

    public static double  targetX = 0, targetY = 64, targetZ = 0;
    public static boolean hasTarget = false;

    /** Dimension the target was set in (Object because ResourceKey isn't on
     *  the compile classpath in this Lunar build; Object#equals works). */
    public static Object  targetDim = null;

    /** Ticks until we may fire another rocket. */
    public static int rocketCooldown = 0;

    /** Health (HP, 1 heart = 2) at or below which we stop the autopilot. */
    public static float safeHpThreshold = 4.0f;

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

        // Death / HP abort — stops further packets while dying / corpse falling
        if (!p.isAlive() || p.isDeadOrDying()) {
            p.displayClientMessage(Component.literal("§c[Goto] died — auto-pilot off"), false);
            stop();
            return;
        }
        if (p.getHealth() <= safeHpThreshold) {
            p.displayClientMessage(Component.literal(
                String.format("§e[Goto] HP %.0f≤%.0f — auto-pilot off", p.getHealth(), safeHpThreshold)), false);
            stop();
            return;
        }

        // Different dimension — pause (target preserved so portalling back resumes)
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

        // Lock yaw on target
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        p.setYRot(yaw);
        p.setYHeadRot(yaw);

        // ── Distance-banded base pitch ───────────────────────────────────────
        // Tightened curve near target so the player kisses the ground at low
        // vertical speed instead of slamming in at 28° dive angle.
        float pitch;
        if      (horizDist > 200) pitch = -3f;     // cruise
        else if (horizDist > 80)  pitch = 10f;     // descend
        else if (horizDist > 30)  pitch = 20f;     // steep approach
        else if (horizDist > 15)  pitch = 8f;      // flare for landing
        else                      pitch = -2f;     // touch-down: level out

        // ── Altitude floor: climb to safe Y when too low and still far ──────
        double safeY = isNether() ? 80 : 120;
        if (p.getY() < safeY - 5 && horizDist > 80) {
            pitch = Math.min(pitch, -10f);
        }

        // ── Speed-aware multi-ray danger detection ──────────────────────────
        double speed = p.getDeltaMovement().horizontalDistance();
        double lookahead = Math.max(15, speed * 12);     // ~12 ticks of flight
        boolean groundDanger  = nearestBelow(p, mc, 7)      < Double.MAX_VALUE;
        double  forwardHit    = nearestAhead(p, mc, lookahead);
        boolean forwardDanger = forwardHit < lookahead;
        boolean ceilingDanger = isNether() && ceilingTooClose(p, mc, 4);

        // ── Avoidance overrides (highest priority wins) ─────────────────────
        if (groundDanger) {
            pitch = -25f;                            // pull up hard
        } else if (forwardDanger) {
            // Closer obstacle → steeper climb
            pitch = forwardHit < lookahead * 0.4 ? -40f : -28f;
        } else if (ceilingDanger) {
            pitch = 20f;                             // push away from ceiling
        }

        p.setXRot(pitch);

        // Deploy elytra if airborne and falling but not gliding
        if (!p.isFallFlying() && !p.onGround() && p.getDeltaMovement().y < 0) {
            try { p.tryToStartFallFlying(); } catch (Throwable ignored) {}
        }

        // Auto-rocket — skip when in danger so we don't boost into the wall,
        // and skip on landing approach so we glide in slowly.
        boolean inDanger = groundDanger || forwardDanger;
        boolean landingPhase = horizDist < 50;
        if (p.isFallFlying() && !inDanger && !landingPhase && rocketCooldown <= 0) {
            if (fireRocket(p, mc)) rocketCooldown = 60;
        }
        if (rocketCooldown > 0) rocketCooldown--;

        showStatus(p, horizDist, false);
    }

    // ── Raycast helpers ──────────────────────────────────────────────────────

    /** Cast 5 horizontal rays in a ±25° fan; returns the nearest block/fluid
     *  hit distance, or maxDist if nothing within range. Fluid.ANY so lava
     *  and water also count as obstacles. */
    private static double nearestAhead(LocalPlayer p, Minecraft mc, double maxDist) {
        float baseYaw = p.getYRot();
        Vec3 from = p.getEyePosition();
        double min = Double.MAX_VALUE;
        for (float offset : new float[]{-25f, -12f, 0f, 12f, 25f}) {
            float yawRad = (float) Math.toRadians(baseYaw + offset);
            Vec3 dir = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad));
            Vec3 to  = from.add(dir.scale(maxDist));
            HitResult hit = mc.level.clip(new ClipContext(
                from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, p));
            if (hit.getType() != HitResult.Type.MISS) {
                double d = from.distanceTo(hit.getLocation());
                if (d < min) min = d;
            }
        }
        return min;
    }

    /** Straight down with Fluid.ANY — catches lava lakes / water as hazards. */
    private static double nearestBelow(LocalPlayer p, Minecraft mc, double maxDist) {
        Vec3 from = p.position();
        Vec3 to   = from.add(0, -maxDist, 0);
        HitResult hit = mc.level.clip(new ClipContext(
            from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.ANY, p));
        return hit.getType() == HitResult.Type.MISS ? Double.MAX_VALUE : from.distanceTo(hit.getLocation());
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
        double etaSec = dist / 33.0;
        int px = (int) p.getX(), pz = (int) p.getZ();
        int tx = (int) targetX,  tz = (int) targetZ;
        String other = isNether()
            ? String.format("§7overworld §f%d,%d→%d,%d", px * 8, pz * 8, tx * 8, tz * 8)
            : String.format("§7nether §f%d,%d→%d,%d",    px / 8, pz / 8, tx / 8, tz / 8);
        p.displayClientMessage(Component.literal(
            String.format("§b[Goto] §f%.0fm  §7ETA §f%.0fs  §8| §7now §f%d,%d  §8| %s",
                dist, etaSec, px, pz, other)), true);
    }
}
