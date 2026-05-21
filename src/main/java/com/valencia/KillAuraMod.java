package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class KillAuraMod {
    private static boolean enabled = false;

    public static float savedYRot, savedXRot;
    public static Entity currentTarget  = null;
    public static boolean pendingAttack = false;

    public static float RANGE        = 4.0f;
    public static float ATTACK_RANGE = 3.0f;
    public static int   attackDelay  = 10;

    public static boolean targetHostile = true;
    public static boolean targetAnimals = false;
    public static boolean targetPlayers = false;

    public static boolean      singleMode   = false;
    public static LivingEntity lockedTarget = null;

    public static Entity glowTarget = null;

    // New: behavior options
    public static boolean raycast       = true;   // line-of-sight check
    public static boolean skipInvisible = true;   // ignore invisible targets
    public static boolean waitCooldown  = true;   // wait for attack charge = 1.0
    public static boolean smoothRot     = true;   // lerp rotation toward target
    public static float   maxTurnDeg    = 60.0f;  // max degrees per tick when smoothing
    public static boolean bodyLock      = false;  // DON'T restore rotation — view physically snaps to target

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    public static boolean isActive() {
        if (!enabled) {
            glowTarget   = null;
            lockedTarget = null;
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            glowTarget = null;
            return false;
        }
        return true;
    }

    public static Entity findTarget() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return null;

        // Single mode: keep locked target if still valid
        if (singleMode && lockedTarget != null) {
            if (!lockedTarget.isDeadOrDying()
                    && reachDistSq(mc.player, lockedTarget) <= (double) RANGE * RANGE
                    && (!skipInvisible || !lockedTarget.isInvisible())
                    && (!raycast || canSee(mc.player, lockedTarget))) {
                glowTarget = lockedTarget;
                return lockedTarget;
            }
            lockedTarget = null;
        }

        AABB box = mc.player.getBoundingBox().inflate(RANGE);
        LivingEntity best = null;
        double bestDistSq = Double.MAX_VALUE;
        double rangeSq = (double) RANGE * RANGE;

        for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (e == mc.player)   continue;
            if (e.isDeadOrDying()) continue;
            if (skipInvisible && e.isInvisible()) continue;

            boolean isPlayer   = e instanceof Player;
            boolean isHostile  = e instanceof Monster;
            boolean isFriendly = e instanceof Animal;
            if (isPlayer   && !targetPlayers) continue;
            if (isHostile  && !targetHostile) continue;
            if (isFriendly && !targetAnimals) continue;
            if (!isPlayer && !isHostile && !isFriendly) continue;

            // Distance from player eye to entity hitbox closest point (squared)
            double distSq = reachDistSq(mc.player, e);
            if (distSq > rangeSq || distSq >= bestDistSq) continue;

            // Defer raycast (most expensive check) until distance + faction pass
            if (raycast && !canSee(mc.player, e)) continue;

            bestDistSq = distSq;
            best = e;
        }

        if (singleMode && best != null) lockedTarget = best;
        glowTarget = best;
        return best;
    }

    /**
     * Squared distance from player's eye to the target's hitbox nearest point.
     * Matches how the vanilla server validates attack reach far better than
     * Entity.distanceTo (which uses center-to-center) — fixes "ghost swings"
     * against airborne / tall mobs.
     */
    public static double reachDistSq(Entity from, Entity to) {
        Vec3 eye = from.getEyePosition();
        AABB b   = to.getBoundingBox();
        double cx = Mth.clamp(eye.x, b.minX, b.maxX);
        double cy = Mth.clamp(eye.y, b.minY, b.maxY);
        double cz = Mth.clamp(eye.z, b.minZ, b.maxZ);
        double dx = eye.x - cx, dy = eye.y - cy, dz = eye.z - cz;
        return dx * dx + dy * dy + dz * dz;
    }

    /** Block raycast from player eye to target eye-Y position. */
    public static boolean canSee(Entity from, Entity to) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return true;
        Vec3 eye = from.getEyePosition();
        Vec3 tgt = new Vec3(to.getX(), to.getEyeY(), to.getZ());
        HitResult hit = mc.level.clip(new ClipContext(
            eye, tgt,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, from
        ));
        return hit.getType() == HitResult.Type.MISS;
    }

    public static float[] calcRotations(Entity from, Entity to) {
        double dx = to.getX() - from.getX();
        double dy = (to.getY() + to.getBbHeight() * 0.85) - (from.getY() + from.getEyeHeight());
        double dz = to.getZ() - from.getZ();
        double h  = Math.sqrt(dx * dx + dz * dz);
        float yaw   = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-dx, dz)));
        float pitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(dy, h)), -90f, 90f);
        return new float[]{yaw, pitch};
    }

    /** Step current rotation toward target by at most maxStep degrees per axis. */
    public static float[] smoothRotation(float curYaw, float curPitch,
                                         float tgtYaw, float tgtPitch, float maxStep) {
        float dy = Mth.wrapDegrees(tgtYaw - curYaw);
        float dp = tgtPitch - curPitch;
        if (Math.abs(dy) > maxStep) dy = Math.signum(dy) * maxStep;
        if (Math.abs(dp) > maxStep) dp = Math.signum(dp) * maxStep;
        return new float[]{Mth.wrapDegrees(curYaw + dy), Mth.clamp(curPitch + dp, -90f, 90f)};
    }
}
