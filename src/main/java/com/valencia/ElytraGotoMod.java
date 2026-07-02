package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
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
 * Set target via chat: `.nf goto [ow|nether] <x> [y] <z>`.
 * Optional frame prefix (`ow` / `nether`) auto-converts the XZ pair
 * by ÷8 / ×8 when standing in the other dimension; Y is never scaled
 * because vanilla portals keep Y unchanged. Stop with `.nf goto stop`.
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

    /** Consecutive airborne ticks (used to delay deploy until server has
     *  synced the player's off-ground position; matches vanilla's behavior
     *  of only deploying once {@code getDeltaMovement().y < 0}). */
    private static int airborneTicks = 0;

    /** Diagnostic: how many useItem calls we've fired this flight. The user
     *  can compare against the visible firework stack count to tell whether
     *  the server is actually accepting the boost or silently ignoring it. */
    private static int firesAttempted = 0;

    /** Firework count snapshot at the start of glide — used to compute how
     *  many rockets were actually consumed (and detect server rejection). */
    private static int flightStartRockets = -1;

    /** Count of re-deploy attempts when server appears to silently reject. */
    private static int redeployTriggers = 0;

    /** Once we see a rocket actually consumed, we know the server is happy and
     *  switch from "spam to bridge sync gap" to "normal 50-tick cooldown". */
    private static boolean rocketConfirmed = false;

    /** "Stopping while airborne" state: instead of abandoning the player mid-
     *  glide (the server has no cancel-fall-flying packet, so they'd keep
     *  gliding / stall = "stuck"), keep a hands-off managed descent — gentle
     *  dive, no yaw lock, no rockets — until they touch ground and vanilla ends
     *  fall-flying, then fully release. */
    private static boolean landing = false;
    private static int landingTicks = 0;

    /** Auto-takeoff: ticks left to hold the jump key (0 = released). */
    private static int autoJumpHold = 0;
    /** Auto-jump attempts since last grounded; capped so a low ceiling doesn't
     *  turn into an endless bounce loop. */
    private static int autoJumpTries = 0;

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
        firesAttempted = 0;
        flightStartRockets = -1;
        redeployTriggers = 0;
        rocketConfirmed = false;
        deployFailTicks = 0;
        landing = false;
        autoJumpTries = 0;
        if (!enabled) enabled = true;
    }

    public static void stop() {
        enabled = false;
        hasTarget = false;
        targetDim = null;
        rocketCooldown = 0;
        firesAttempted = 0;
        flightStartRockets = -1;
        redeployTriggers = 0;
        rocketConfirmed = false;
        airborneTicks = 0;
        landing = false;
        landingTicks = 0;
        autoJumpTries = 0;
        if (autoJumpHold > 0) {
            autoJumpHold = 0;
            Minecraft mc = Minecraft.getInstance();
            if (mc.options != null) mc.options.keyJump.setDown(false);
        }
    }

    /**
     * Chat {@code .nf goto stop}. If the player is mid-glide we can't tell the
     * server to cancel fall-flying, so abandoning control leaves them gliding
     * / stalling ("stuck"). Instead enter a managed descent that flies them
     * down to the ground (where vanilla ends fall-flying) and then releases.
     * On the ground already → stop immediately.
     */
    public static void requestStop() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p != null && p.isFallFlying() && !p.onGround()) {
            landing = true;
            landingTicks = 0;
            hasTarget = false;   // no more target steering
            enabled = true;      // keep tick() alive for the descent
            p.displayClientMessage(Component.literal(
                "§e[Goto] §7landing — gliding down, control releases on touchdown"), false);
        } else {
            stop();
            if (p != null) p.displayClientMessage(Component.literal("§7[Goto] §cstopped"), false);
        }
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
        if (landing) { tickLanding(); return; }
        if (!isActive()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;

        // Release the auto-takeoff jump key after its short hold.
        if (autoJumpHold > 0 && --autoJumpHold == 0) mc.options.keyJump.setDown(false);

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
        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));

        // Arrived horizontally — handle the landing phase. Don't stop the
        // autopilot just because we're over the target; keep guiding the
        // player down until they actually touch ground (or vanilla cancels
        // fall flying for us). This fixes "doesn't land — autopilot quits
        // mid-air and I'm still gliding past the target".
        if (horizDist < 8) {
            if (p.onGround() || !p.isFallFlying()) {
                showStatus(p, horizDist, "arrived");
                stop();
                return;
            }
            // Still airborne above target: gentle descent if above target Y,
            // level out otherwise. Smooth rotation so the camera doesn't snap.
            smoothSetYaw(p, targetYaw);
            float landingPitch = (p.getY() > targetY + 2) ? 15f : 0f;
            smoothSetPitch(p, landingPitch);
            showStatus(p, horizDist, "flying");
            return;
        }

        // Treat "really flying" as fall-flying AND not on ground. If the
        // client-side fall-flying flag is stuck true (server already cancelled
        // because the player touched ground, but the sync packet hasn't
        // arrived), trusting isFallFlying() alone would send useItem packets
        // to a server that has isFallFlying = false — silent rocket no-ops
        // that don't even consume the stack.
        boolean reallyFlying = p.isFallFlying() && !p.onGround();

        if (!reallyFlying) {
            if (p.onGround()) {
                deployFailTicks = 0;
                airborneTicks = 0;
                // Auto-takeoff: jump for the player, but only when the flight
                // can actually happen (elytra on back + rockets in inventory)
                // and capped so a low ceiling doesn't bounce forever.
                boolean geared = p.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)
                              && countRockets(p) > 0;
                if (geared && autoJumpTries < 8) {
                    if (autoJumpHold == 0) {
                        autoJumpTries++;
                        autoJumpHold = 2;
                        mc.options.keyJump.setDown(true);
                    }
                    showStatus(p, horizDist, "takeoff");
                } else {
                    showStatus(p, horizDist, geared ? "jump" : "gear");
                }
                return;
            }
            airborneTicks++;

            // Wait until the player is actually FALLING before sending the
            // deploy packet (matches vanilla's `y < 0` check). Critical: if
            // we send START_FALL_FLYING while the client thinks airborne but
            // the server's last synced position still has the player on the
            // ground (the jump-arc / first 100-200ms after take-off), the
            // server REJECTS the deploy. Client flag flips true locally,
            // server flag stays false, every subsequent useItem packet is
            // silently rejected because server.isFallFlying disagrees.
            //
            // Fallback: if we've been airborne ~300ms with y still hovering
            // near zero (e.g. tiny ledge), force the deploy so we don't lose
            // the jump entirely.
            boolean falling = p.getDeltaMovement().y < -0.04;
            boolean forceDeploy = airborneTicks >= 6;
            if (!falling && !forceDeploy) {
                showStatus(p, horizDist, "deploying");
                return;
            }

            boolean deployed = false;
            try { deployed = p.tryToStartFallFlying(); } catch (Throwable ignored) {}
            if (deployed) {
                deployFailTicks = 0;
                // Brief delay before first rocket so the deploy packet has
                // landed on the server. Adaptive retry will catch up if
                // there's any remaining sync gap.
                rocketCooldown = 4;
            } else {
                deployFailTicks++;
            }
            showStatus(p, horizDist, deployFailTicks > 40 ? "deploy-fail" : "deploying");
            return;
        }
        deployFailTicks = 0;
        airborneTicks = 0;
        autoJumpTries = 0;

        // ── From here on the player is gliding: full autopilot ──────────────

        // Snapshot the firework count on the first gliding tick of this flight
        // so we can tell later whether the server is actually consuming
        // rockets or silently rejecting our useItem packets.
        if (flightStartRockets < 0) {
            flightStartRockets = countRockets(p);
        }

        // If we've fired several useItems but the stack hasn't moved, the
        // server is silently rejecting (server.isFallFlying disagrees with
        // client). Re-send START_FALL_FLYING directly to try to nudge the
        // server back in sync. Capped at 3 attempts to avoid spam.
        int consumed = flightStartRockets - countRockets(p);
        if (firesAttempted >= 2 && consumed == 0 && redeployTriggers < 3) {
            try {
                if (p.connection != null) {
                    p.connection.send(new ServerboundPlayerCommandPacket(
                        p, ServerboundPlayerCommandPacket.Action.START_FALL_FLYING));
                }
            } catch (Throwable ignored) {}
            redeployTriggers++;
        }

        // Lock yaw on target — smoothed so the camera doesn't snap-rotate
        smoothSetYaw(p, targetYaw);

        // Target-Y-aware base pitch.
        //   Long cruise (>200m): slight nose-up, gravity will trim altitude —
        //     UNLESS we're already inside the glide cone to a lower target,
        //     in which case start the descent now. Cruising level and only
        //     descending over the target meant arriving high and orbiting
        //     down; an elytra glides ~10:1 level, so a 12:1 gate starts the
        //     descent with margin to spare.
        //   Mid range: aim toward (target + 5° above) so we don't undershoot
        //   Close in (<15m): level out for soft touchdown
        float pitch;
        double dy = targetY - p.getY();   // + = target above, - = target below
        boolean descendNow = dy < -10 && horizDist < -dy * 12;
        if (horizDist > 200 && !descendNow) {
            pitch = -3f;
        } else if (horizDist > 15) {
            double aimRad = Math.atan2(-dy, horizDist);
            pitch = (float) Math.toDegrees(aimRad) - 5f;   // 5° margin above line
            pitch = Math.max(-30f, Math.min(25f, pitch));  // clamp
        } else {
            pitch = -2f;                                   // flare
        }

        // Altitude floor — always active, even on short trips. Climb scales
        // with how far below safeY the player is. Released on final approach
        // to a low target (within a ~18° descent path), otherwise the floor
        // pins us at safeY until 20m out and forces an impossible dive.
        double safeY = isNether() ? 80 : 110;
        double yBelow = safeY - p.getY();
        boolean finalDescent = horizDist < Math.max(60, (p.getY() - targetY) * 3);
        if (yBelow > 5 && horizDist > 20 && !finalDescent) {
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

        // Stall guard: a hard climb with almost no airspeed (< ~7 m/s) is an
        // imminent elytra stall — the player would drop straight into the
        // terrain the climb was avoiding. Level out and let a rocket fire
        // right away to power through instead.
        if (pitch <= -20f && speed < 0.35) {
            pitch = -8f;
            if (rocketCooldown > 2) rocketCooldown = 2;
        }

        smoothSetPitch(p, pitch);

        // Detect first successful rocket consumption — flip to normal cooldown
        if (firesAttempted > 0 && consumed > 0) rocketConfirmed = true;

        // Auto-rocket cooldown is adaptive:
        //   - Before any rocket has been confirmed consumed by the server:
        //     retry every 2 ticks (~100ms, 10 fires/sec). Manual right-click
        //     fires at most ~5 fires/sec (4-tick rightClickDelay) and still
        //     needs ~1 sec of attempts to bridge the START_FALL_FLYING sync
        //     gap. At 10 fires/sec we catch the sync window almost instantly
        //     and the rejected attempts cost nothing (server doesn't shrink
        //     the stack when isFallFlying disagrees).
        //   - Once at least one rocket has been consumed by the server, drop
        //     to the normal 50-tick cadence (2.5 sec — matches a default
        //     duration-1 firework boost) so we don't burn rockets needlessly.
        int interval = rocketConfirmed ? 50 : 2;
        boolean closeApproach = horizDist < 20;
        if (!closeApproach && rocketCooldown <= 0) {
            if (fireRocket(p, mc)) {
                rocketCooldown = interval;
                firesAttempted++;
            }
        }
        if (rocketCooldown > 0) rocketCooldown--;

        showStatus(p, horizDist, "flying");
    }

    /** Managed descent after {@code .nf goto stop} while airborne. Staged dive
     *  (steep when high, flare near the ground), no yaw lock (player picks the
     *  landing spot), no rockets. Releases the moment fall-flying ends
     *  (touchdown) or after a 30s safety cap so it can never itself get stuck.
     *
     *  Two hazards are actively avoided:
     *   - Wall impact: elytra kinetic damage from slamming a hillside is
     *     lethal; if the forward fan sees an obstacle, climb to skim over it
     *     before resuming the descent.
     *   - Lava (nether): a fluids-inclusive vs blocks-only ray pair tells us
     *     the surface below is lava — hold altitude and keep gliding until
     *     solid ground is underneath instead of flaring into the lake. */
    private static void tickLanding() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) { stop(); return; }

        landingTicks++;
        boolean grounded = p.onGround() || !p.isFallFlying();
        if (grounded || !p.isAlive() || landingTicks > 600) {
            stop();
            p.displayClientMessage(Component.literal(grounded
                ? "§a[Goto] landed — control released"
                : "§e[Goto] released §7(脫掉鞘翅才能走路)"), true);
            return;
        }

        // Wall-impact guard first — same fan the cruise autopilot uses.
        double speed = p.getDeltaMovement().horizontalDistance();
        double lookahead = Math.max(10, speed * 12);
        if (nearestAhead(p, mc, lookahead) < lookahead) {
            smoothSetPitch(p, -12f);   // skim up and over, then resume descent
            p.displayClientMessage(Component.literal("§e[Goto] §7landing… §6避障拉起"), true);
            return;
        }

        boolean nether = isNether();
        double solidBelow = nearestBelow(p, mc, 32, false);
        double anyBelow   = nether ? nearestBelow(p, mc, 32, true) : solidBelow;
        // Fluid surface strictly above the blocks-only hit ⇒ lava underneath.
        boolean lavaBelow = nether && anyBelow < solidBelow - 0.5;

        float pitch;
        String note;
        if (lavaBelow) {
            pitch = -3f;               // hold altitude, glide to solid ground
            note  = " §c熔岩下方,續滑";
        } else if (anyBelow < 10) {
            pitch = 0f;                // flare for a soft touchdown
            note  = "";
        } else if (anyBelow < 24) {
            pitch = 12f;               // shallow final descent
            note  = "";
        } else {
            pitch = 30f;               // get down fast — less drift from the stop point
            note  = "";
        }
        smoothSetPitch(p, pitch);

        String alt = anyBelow < Double.MAX_VALUE ? String.format(" §f%dm", (int) anyBelow) : "";
        p.displayClientMessage(Component.literal("§e[Goto] §7landing…" + alt + note), true);
    }

    // ── Rotation smoothing ──────────────────────────────────────────────────

    /** Move yaw toward {@code target} by at most ~12° per tick. Snapping the
     *  full delta every tick (the v1.6.9 behavior) made the camera whip
     *  around when forward danger or yaw deltas exceeded ~20°. */
    private static void smoothSetYaw(LocalPlayer p, float target) {
        float current = p.getYRot();
        float delta = Mth.wrapDegrees(target - current);
        float maxStep = 12f;
        if (Math.abs(delta) > maxStep) delta = Math.signum(delta) * maxStep;
        float next = Mth.wrapDegrees(current + delta);
        p.setYRot(next);
        p.setYHeadRot(next);
    }

    /** Move pitch toward {@code target} by at most ~10° per tick. */
    private static void smoothSetPitch(LocalPlayer p, float target) {
        float current = p.getXRot();
        float delta = target - current;
        float maxStep = 10f;
        if (Math.abs(delta) > maxStep) delta = Math.signum(delta) * maxStep;
        p.setXRot(Mth.clamp(current + delta, -90f, 90f));
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
            // setSelectedSlot is public in this build (verified via javap) — no
            // reflection needed, and it stays correct across remaps.
            Inventory inv = p.getInventory();
            int prevSlot = inv.getSelectedSlot();
            inv.setSelectedSlot(rocketSlot);
            try {
                if (p.connection != null) {
                    p.connection.send(new ServerboundSetCarriedItemPacket(rocketSlot));
                }
            } catch (Throwable ignored) {}
            boolean fired = useItemSafe(mc, p, InteractionHand.MAIN_HAND);
            // Restore the player's original slot right away (swap → use →
            // swap back, same packet order Scaffold's fakeHand uses) so a
            // mid-flight boost doesn't leave a rocket in hand on landing.
            inv.setSelectedSlot(prevSlot);
            try {
                if (p.connection != null) {
                    p.connection.send(new ServerboundSetCarriedItemPacket(prevSlot));
                }
            } catch (Throwable ignored) {}
            return fired;
        }
        return false;  // no rocket anywhere in hotbar
    }

    /** Fire the item in {@code hand} via the vanilla useItem path, but force
     *  the "no target" branch by null-ing {@code mc.hitResult} for the duration
     *  of the call. {@code MultiPlayerGameMode.useItem} short-circuits into
     *  {@code blockState.useItemOn} when {@code hitResult} is a block, which
     *  turns a firework "boost me" into a "place rocket on the block". With
     *  hitResult cleared, control falls into {@code useItemFromInventory}
     *  which is the correct path that triggers the fall-flying speed boost. */
    private static boolean useItemSafe(Minecraft mc, LocalPlayer p, InteractionHand hand) {
        if (mc.gameMode == null) return false;
        HitResult savedHit = mc.hitResult;
        try {
            mc.hitResult = null;
            mc.gameMode.useItem(p, hand);
            return true;
        } catch (Throwable t) {
            return false;
        } finally {
            mc.hitResult = savedHit;
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
        if ("takeoff".equals(state)) {
            p.displayClientMessage(Component.literal(
                String.format("§e[Goto] §7auto-takeoff… (%.0fm to target)", dist)), true);
            return;
        }
        if ("gear".equals(state)) {
            p.displayClientMessage(Component.literal(
                "§c[Goto] 缺裝備 — 需要穿上鞘翅 + 帶煙火"), true);
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
        // ETA from actual airspeed (floor 8 m/s so a momentary stall doesn't
        // show a comical four-digit ETA), not the old fixed 33 m/s guess.
        double airspeed = p.getDeltaMovement().horizontalDistance() * 20.0;
        double etaSec = dist / Math.max(8.0, airspeed);
        int rockets = countRockets(p);
        int consumed = (flightStartRockets >= 0) ? (flightStartRockets - rockets) : 0;

        // 消耗 = rockets actually used (server confirmed)
        // 嘗試 = our useItem call count
        // If 嘗試 grows but 消耗 stays at 0, the server is silently rejecting
        // the fire — either it doesn't allow firework boost on this realm or
        // server.isFallFlying disagrees with the client. Highlight in red.
        String ammo;
        if (rockets <= 0) {
            ammo = "§c無煙火!";
        } else if (firesAttempted >= 2 && consumed == 0) {
            ammo = String.format("§c伺服器拒收 §7(嘗試§f%d§7,消耗§f0§7)", firesAttempted);
        } else {
            ammo = String.format("§7煙火 §f%d §8(消耗§f%d§8/嘗試§f%d§8)",
                rockets, consumed, firesAttempted);
        }
        // ΔY tells the user if the target is above (+) or below (−) them — handy
        // for judging the descent on approach.
        int dyTarget = (int) Math.round(targetY - p.getY());
        p.displayClientMessage(Component.literal(
            String.format("§b[Goto] §f%.0fm  §7ETA §f%.0fs  §8| §7Y §f%d§7(%+d)  §8| %s",
                dist, etaSec, (int) p.getY(), dyTarget, ammo)), true);
    }
}
