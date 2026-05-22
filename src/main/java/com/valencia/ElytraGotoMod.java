package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
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

    /** Consecutive ticks the deploy has failed — used to surface a clear error
     *  to the player instead of looping "deploying…" forever. */
    private static int deployFailTicks = 0;

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
        rocketCooldown = 0;        // fresh flight — fire the first rocket ASAP
        if (!enabled) enabled = true;
    }

    public static void stop() {
        enabled = false;
        hasTarget = false;
        targetDim = null;
        rocketCooldown = 0;
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
            showStatus(p, horizDist, "arrived");
            stop();
            return;
        }

        // ── Pre-glide: NEVER touch rotation while the player is on the ground
        //    or mid-jump. Only attempt to deploy elytra once airborne. This
        //    fixes the "view locks before I can even jump" bug.
        if (!p.isFallFlying()) {
            if (p.onGround()) {
                deployFailTicks = 0;
                showStatus(p, horizDist, "jump");
                return;
            }
            // Airborne but not gliding → try to deploy every tick.
            boolean deployed = false;
            try { deployed = p.tryToStartFallFlying(); } catch (Throwable ignored) {}
            if (deployed) {
                deployFailTicks = 0;
            } else {
                deployFailTicks++;
            }
            // After ~2 sec of failed deploys, surface a clearer error.
            // Common causes: elytra durability <= 1, in water, levitation effect.
            showStatus(p, horizDist, deployFailTicks > 40 ? "deploy-fail" : "deploying");
            return;
        }
        deployFailTicks = 0;

        // ── From here on the player is gliding: full autopilot ──────────────

        // Lock yaw on target
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        p.setYRot(yaw);
        p.setYHeadRot(yaw);

        // Target-Y-aware base pitch.
        //   Long cruise (>200m): slight nose-up, gravity will trim altitude
        //   Mid range: aim toward (target + 5° above) so we don't undershoot
        //   Close in (<15m): level out for soft touchdown
        float pitch;
        double dy = targetY - p.getY();   // + = target above, - = target below
        if (horizDist > 200) {
            pitch = -3f;
        } else if (horizDist > 15) {
            double aimRad = Math.atan2(-dy, horizDist);
            pitch = (float) Math.toDegrees(aimRad) - 5f;   // 5° margin above line
            pitch = Math.max(-30f, Math.min(25f, pitch));  // clamp
        } else {
            pitch = -2f;                                   // flare
        }

        // Altitude floor — always active, even on short trips. Climb scales
        // with how far below safeY the player is.
        double safeY = isNether() ? 80 : 110;
        double yBelow = safeY - p.getY();
        if (yBelow > 5 && horizDist > 20) {
            float climbPitch = (float) Math.max(-30, -8 - yBelow * 0.4);
            pitch = Math.min(pitch, climbPitch);
        }

        // Multi-ray danger detection.
        //   nearestBelow uses Fluid.NONE in overworld (so open water doesn't
        //   permanently flag groundDanger), Fluid.ANY in nether (lava kills).
        //   nearestAhead now tilts the scan 10° downward so terrain we are
        //   diving into is also detected (pure-horizontal scan missed cliffs
        //   when we were at +20° pitch heading toward a peak below the
        //   horizontal eye line).
        double speed = p.getDeltaMovement().horizontalDistance();
        double lookahead = Math.max(15, speed * 12);
        boolean groundDanger  = nearestBelow(p, mc, 7, isNether()) < Double.MAX_VALUE;
        double  forwardHit    = nearestAhead(p, mc, lookahead);
        boolean forwardDanger = forwardHit < lookahead;
        boolean ceilingDanger = isNether() && ceilingTooClose(p, mc, 4);

        // Avoidance overrides. Sandwich (ground + ceiling) levels out instead
        // of slamming into one or the other. Otherwise take the steepest
        // climb of (groundDanger -25, forwardDanger -28 / -40) since both can
        // be true at once near a cliff face.
        if (groundDanger && ceilingDanger) {
            pitch = 0f;
        } else {
            float climb = Float.POSITIVE_INFINITY;
            if (groundDanger) climb = Math.min(climb, -25f);
            if (forwardDanger) climb = Math.min(climb, forwardHit < lookahead * 0.4 ? -40f : -28f);
            if (climb < Float.POSITIVE_INFINITY) {
                pitch = Math.min(pitch, climb);
            } else if (ceilingDanger) {
                pitch = Math.max(pitch, 20f);
            }
        }

        p.setXRot(pitch);

        // Auto-rocket. Fire on every available cycle while gliding — even in
        // forward danger, because the pitch override already pulled us to -28
        // or -40, and at that climb angle the rocket is what gets us *over*
        // the obstacle. Skipping rockets in danger leaves the player gliding
        // into the ground at 1-3 b/s with no thrust to escape.
        //
        // Only the last 20m of approach skips the boost so we don't ram the
        // target at full speed.
        boolean closeApproach = horizDist < 20;
        if (!closeApproach && rocketCooldown <= 0) {
            if (fireRocket(p, mc)) rocketCooldown = 50;
        }
        if (rocketCooldown > 0) rocketCooldown--;

        showStatus(p, horizDist, "flying");
    }

    // ── Raycast helpers ──────────────────────────────────────────────────────

    /** Cast 5 rays in a ±25° horizontal fan, tilted ~10° downward to also
     *  pick up terrain the player is diving into. Returns the nearest
     *  block/fluid hit distance across all 5 rays, or {@code Double.MAX_VALUE}
     *  if nothing within range. {@code Fluid.ANY} so lava and water also
     *  count as obstacles. */
    private static double nearestAhead(LocalPlayer p, Minecraft mc, double maxDist) {
        float baseYaw = p.getYRot();
        // Tilt 10° below horizontal so a dive-pitched player still sees the
        // ground that's about to be in their flight path.
        float tiltRad = (float) Math.toRadians(10);
        double tiltY = -Math.sin(tiltRad);
        double tiltH =  Math.cos(tiltRad);
        Vec3 from = p.getEyePosition();
        double min = Double.MAX_VALUE;
        for (float offset : new float[]{-25f, -12f, 0f, 12f, 25f}) {
            float yawRad = (float) Math.toRadians(baseYaw + offset);
            Vec3 dir = new Vec3(-Math.sin(yawRad) * tiltH, tiltY, Math.cos(yawRad) * tiltH);
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

    /** Distance to nearest hit directly below. {@code includeFluids} controls
     *  whether water/lava counts — in nether yes (lava kills), in overworld
     *  no (water doesn't, and a 7m water column would constantly false-flag). */
    private static double nearestBelow(LocalPlayer p, Minecraft mc, double maxDist, boolean includeFluids) {
        Vec3 from = p.position();
        Vec3 to   = from.add(0, -maxDist, 0);
        ClipContext.Fluid fluidMode = includeFluids ? ClipContext.Fluid.ANY : ClipContext.Fluid.NONE;
        HitResult hit = mc.level.clip(new ClipContext(
            from, to, ClipContext.Block.COLLIDER, fluidMode, p));
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

        // Fast path: rocket already in either hand
        if (p.getMainHandItem().is(Items.FIREWORK_ROCKET))
            return useItemSafe(mc, p, InteractionHand.MAIN_HAND);
        if (p.getOffhandItem().is(Items.FIREWORK_ROCKET))
            return useItemSafe(mc, p, InteractionHand.OFF_HAND);

        // Hotbar scan — auto-switch the selected slot to a rocket so the
        // server sees a normal "swap hotbar then right-click" sequence.
        int rocketSlot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = p.getInventory().getItem(i);
            if (stack.is(Items.FIREWORK_ROCKET)) { rocketSlot = i; break; }
        }
        if (rocketSlot >= 0) {
            if (!setHotbarSlot(p, rocketSlot)) return false;
            try {
                if (p.connection != null) {
                    p.connection.send(new ServerboundSetCarriedItemPacket(rocketSlot));
                }
            } catch (Throwable ignored) {}
            return useItemSafe(mc, p, InteractionHand.MAIN_HAND);
        }
        return false;  // no rocket anywhere in hotbar
    }

    /** Set the selected hotbar slot — tries the modern method first, falls
     *  back to writing the private {@code selected} field via reflection
     *  (different MC builds expose this differently). */
    private static boolean setHotbarSlot(LocalPlayer p, int slot) {
        Object inv = p.getInventory();
        // Try common setter names first
        for (String name : new String[]{"setSelectedHotbarSlot", "setSelectedSlot"}) {
            try {
                java.lang.reflect.Method m = inv.getClass().getMethod(name, int.class);
                m.invoke(inv, slot);
                return true;
            } catch (NoSuchMethodException e) {
                // try next
            } catch (Throwable t) {
                return false;
            }
        }
        // Fall back to setting the field directly via reflection
        try {
            java.lang.reflect.Field f = inv.getClass().getDeclaredField("selected");
            f.setAccessible(true);
            f.setInt(inv, slot);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Send a direct ServerboundUseItemPacket. Bypasses {@code mc.gameMode.useItem}
     *  which checks {@code mc.hitResult} and, when the autopilot is pitched
     *  +20 deg into terrain, routes a firework "use" through the block-place
     *  path (rocket gets placed on the block instead of boosting the glide).
     *  Direct packet send always triggers the server-side fall-flying boost. */
    private static boolean useItemSafe(Minecraft mc, LocalPlayer p, InteractionHand hand) {
        if (p.connection == null) return false;
        try {
            int seq = 0;
            try {
                java.lang.reflect.Method m = p.connection.getClass().getMethod("suggestedSequence");
                Object result = m.invoke(p.connection);
                if (result instanceof Integer) seq = (Integer) result;
            } catch (Throwable ignored) {}
            p.connection.send(new ServerboundUseItemPacket(hand, seq, p.getYRot(), p.getXRot()));
            // Local prediction so the item count visually updates this tick
            try {
                ItemStack stack = p.getItemInHand(hand);
                if (!p.getAbilities().instabuild) stack.shrink(1);
            } catch (Throwable ignored) {}
            return true;
        } catch (Throwable t) {
            // Fallback to the high-level path if direct packet build fails
            try {
                mc.gameMode.useItem(p, hand);
                return true;
            } catch (Throwable ignored) {
                return false;
            }
        }
    }

    /** Count firework rockets in the player's hotbar + offhand for the status display. */
    private static int countRockets(LocalPlayer p) {
        int n = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(Items.FIREWORK_ROCKET)) n += s.getCount();
        }
        ItemStack off = p.getOffhandItem();
        if (off.is(Items.FIREWORK_ROCKET)) n += off.getCount();
        return n;
    }

    private static void showStatus(LocalPlayer p, double dist, String state) {
        if ("arrived".equals(state)) {
            p.displayClientMessage(Component.literal(
                String.format("§a[Goto] arrived (%.0fm)", dist)), true);
            return;
        }
        if ("jump".equals(state)) {
            p.displayClientMessage(Component.literal(
                String.format("§e[Goto] §7waiting — jump off something (%.0fm to target)", dist)), true);
            return;
        }
        if ("deploying".equals(state)) {
            p.displayClientMessage(Component.literal(
                "§e[Goto] §7deploying elytra…"), true);
            return;
        }
        if ("deploy-fail".equals(state)) {
            p.displayClientMessage(Component.literal(
                "§c[Goto] elytra 展不開 — 檢查耐久、確認沒在水裡 / 沒 levitation 效果"), true);
            return;
        }
        double etaSec = dist / 33.0;
        int px = (int) p.getX(), pz = (int) p.getZ();
        int rockets = countRockets(p);
        String ammo = rockets > 0
            ? String.format("§7煙火 §f%d", rockets)
            : "§c無煙火!";
        p.displayClientMessage(Component.literal(
            String.format("§b[Goto] §f%.0fm  §7ETA §f%.0fs  §8| §7Y §f%d  §8| %s",
                dist, etaSec, (int) p.getY(), ammo)), true);
    }
}
