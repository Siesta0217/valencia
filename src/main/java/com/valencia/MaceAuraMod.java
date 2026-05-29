package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;

public class MaceAuraMod {

    private static boolean enabled = false;

    public static float savedYRot, savedXRot;
    public static Entity currentTarget = null;
    public static boolean pendingAttack = false;
    public static Entity glowTarget = null;

    public static float RANGE        = 6.0f;
    public static float ATTACK_RANGE = 3.5f;

    public static boolean targetHostile = true;
    public static boolean targetAnimals = false;
    public static boolean targetPlayers = true;

    public static boolean isEnabled() { return enabled; }
    public static void toggle() { enabled = !enabled; }

    public static boolean isActive() {
        if (!enabled) { glowTarget = null; return false; }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) { glowTarget = null; return false; }
        if (!mc.player.getMainHandItem().is(Items.MACE)) { glowTarget = null; return false; }
        return true;
    }

    public static Entity findTarget() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return null;

        AABB box = mc.player.getBoundingBox().inflate(RANGE);
        Entity best = null;
        double bestDistSq = Double.MAX_VALUE;
        double rangeSq = (double) RANGE * RANGE;

        for (LivingEntity e : mc.level.getEntitiesOfClass(LivingEntity.class, box)) {
            if (e == mc.player) continue;
            if (e.isDeadOrDying()) continue;

            boolean isPlayer  = e instanceof Player;
            boolean isHostile = e instanceof Enemy;
            boolean isAnimal  = e instanceof Animal;
            if (isPlayer  && !targetPlayers) continue;
            if (isHostile && !targetHostile) continue;
            if (isAnimal  && !targetAnimals) continue;
            if (!isPlayer && !isHostile && !isAnimal) continue;

            // Eye → hitbox nearest point (squared), matching KillAura — fixes
            // ghost swings on tall / airborne mobs that center-distance misjudges.
            double distSq = KillAuraMod.reachDistSq(mc.player, e);
            if (distSq <= rangeSq && distSq < bestDistSq) {
                bestDistSq = distSq;
                best = e;
            }
        }
        glowTarget = best;
        return best;
    }

    public static float[] calcRotations(Entity from, Entity to) {
        double dx = to.getX() - from.getX();
        double dy = (to.getY() + to.getBbHeight() * 0.85) - (from.getY() + from.getEyeHeight());
        double dz = to.getZ() - from.getZ();
        double h = Math.sqrt(dx * dx + dz * dz);
        float yaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-dx, dz)));
        float pitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(dy, h)), -90f, 90f);
        return new float[]{yaw, pitch};
    }
}
