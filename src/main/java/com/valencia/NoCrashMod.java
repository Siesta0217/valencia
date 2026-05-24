package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * NoCrash — mitigates elytra "flew into a wall" damage.
 *
 * Vanilla damage path (server-authoritative, can't be cancelled client-side):
 *   in {@code LivingEntity.travel()} while fall-flying, when
 *   {@code horizontalCollision}:
 *     change = previousDelta.length() - currentDelta.length()
 *     if (change > 0.2) hurt(flyIntoWall, (int)(change * 10))
 *
 * Strategy: raycast in the direction of motion each tick. If a solid block
 * is within {@code lookahead} blocks, clamp horizontal velocity to
 * {@code maxSpeed}. Because we apply the deceleration BEFORE the collision
 * tick, the server sees a smooth slowdown rather than a high-speed impact
 * — {@code change} stays under the 0.2 threshold so no damage is dealt.
 *
 * Works for any elytra flight (manual or ElytraGoto). Does nothing when
 * the player isn't fall-flying, so it's safe to leave enabled.
 */
public class NoCrashMod {

    private static boolean enabled = false;

    /** How far ahead to raycast (blocks). At normal flight speed (~1.5 b/t),
     *  4 blocks ≈ 2-3 ticks of warning — enough for one good brake pulse. */
    public static float lookahead = 4.0f;

    /** Horizontal speed cap when a wall is detected (blocks/tick).
     *  Squared length difference of 0.4 between ticks ≈ change ≤ 0.2 → no dmg. */
    public static float maxSpeed = 0.4f;

    public static boolean isEnabled() { return enabled; }
    public static void toggle() { enabled = !enabled; }

    public static void tick() {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;
        if (!p.isFallFlying()) return;

        Vec3 v = p.getDeltaMovement();
        double speed = Math.sqrt(v.x * v.x + v.z * v.z);
        if (speed < 0.05) return;  // standing still horizontally — no impact risk

        // Raycast at eye level in the direction of horizontal motion.
        Vec3 eye = p.getEyePosition();
        Vec3 dir = new Vec3(v.x, 0, v.z).normalize();
        Vec3 end = eye.add(dir.scale(lookahead));

        HitResult hit = mc.level.clip(new ClipContext(
            eye, end,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            p));

        if (hit.getType() != HitResult.Type.BLOCK) return;

        // Wall ahead — clamp horizontal speed. Vertical motion left alone so
        // gravity / firework boost still apply naturally.
        double scale = maxSpeed / speed;
        if (scale >= 1.0) return;  // already slow enough

        p.setDeltaMovement(v.x * scale, v.y, v.z * scale);
    }
}
