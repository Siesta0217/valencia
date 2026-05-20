package com.nofall;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class KillAuraMod {
    private static boolean enabled = false;

    public static float savedYRot, savedXRot;
    public static Entity currentTarget = null;
    public static boolean pendingAttack = false;

    public static float RANGE        = 4.0f;
    public static float ATTACK_RANGE = 3.0f;
    public static boolean targetHostile = true;
    public static boolean targetAnimals = false;
    public static boolean targetPlayers = false;

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    public static boolean isActive() {
        if (!enabled) return false;
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null;
    }

    public static Entity findTarget() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return null;

        AABB box = mc.player.getBoundingBox().inflate(RANGE);
        Entity best = null;
        double bestDist = Double.MAX_VALUE;

        for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (e == mc.player) continue;
            if (e.isDeadOrDying()) continue;

            boolean isPlayer   = e instanceof Player;
            boolean isHostile  = e instanceof Monster;
            boolean isFriendly = e instanceof Animal;

            if (isPlayer   && !targetPlayers) continue;
            if (isHostile  && !targetHostile) continue;
            if (isFriendly && !targetAnimals) continue;
            if (!isPlayer && !isHostile && !isFriendly) continue;

            double dist = mc.player.distanceTo(e);
            if (dist <= RANGE && dist < bestDist) {
                bestDist = dist;
                best = e;
            }
        }
        return best;
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
}
