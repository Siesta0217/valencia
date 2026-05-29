package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * SpearAura — auto-target + silent-aim for the 1.21.11 Spear weapon.
 *
 * Spear mechanics that drive the design:
 *  - Tiered weapon (wooden..netherite) — `Items.WOODEN_SPEAR` etc. We check
 *    via the `ItemTags.SPEARS` tag so all tiers + future variants count.
 *  - Two attack types: JAB (quick left-click) and CHARGE (hold left-click).
 *  - Damage depends on view-angle accuracy and relative speed of attacker
 *    vs target. Mounted-at-gallop + charge = max damage; dismounts riders.
 *  - Has a MIN reach as well as MAX reach — targets too close yield no damage.
 *  - Lunge enchant: jab-only, dashes forward, drains hunger (we don't trigger
 *    it ourselves — vanilla handles it when the player has Lunge equipped).
 */
public class SpearAuraMod {

    // ── Modes ────────────────────────────────────────────────────────────────
    public static final int MODE_JAB    = 0;
    public static final int MODE_CHARGE = 1;
    public static final int MODE_AUTO   = 2;

    // ── State ────────────────────────────────────────────────────────────────
    private static boolean enabled = false;

    public static float savedYRot, savedXRot;
    public static Entity currentTarget = null;
    public static boolean pendingAttack = false;
    public static Entity glowTarget = null;

    // Charge state machine — keyAttack stays down for `chargeReleaseTicks`,
    // then we release so the spear actually swings. Tracked in mixin tick.
    public static boolean charging = false;
    public static int chargeTicks = 0;

    // ── Tunable (mutated by ClickGUI / config) ───────────────────────────────
    public static float SCAN_RANGE = 7.0f;   // wider scan to find candidates
    public static float MIN_REACH  = 1.6f;   // below this, spear deals 0 dmg
    public static float MAX_REACH  = 5.5f;   // extended reach
    public static int   mode       = MODE_AUTO;

    public static boolean targetPlayers = true;
    public static boolean targetHostile = true;
    public static boolean targetAnimals = false;

    /** Auto-walk backward when the best target is inside MIN_REACH. */
    public static boolean autoStepBack = true;

    /** How long to hold attack in CHARGE mode before releasing (ticks). */
    public static int chargeReleaseTicks = 12;

    // ── Public API ───────────────────────────────────────────────────────────
    public static boolean isEnabled() { return enabled; }

    public static void toggle() {
        enabled = !enabled;
        if (!enabled) {
            stopCharge();
            stopStepBack();
        }
    }

    public static boolean isActive() {
        if (!enabled) { glowTarget = null; return false; }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) { glowTarget = null; return false; }
        ItemStack held = mc.player.getMainHandItem();
        if (held.isEmpty() || !held.is(ItemTags.SPEARS)) { glowTarget = null; return false; }
        return true;
    }

    /**
     * Pick the best target in SCAN_RANGE.
     *
     * Priority: prefer the target closest to the "sweet spot" distance
     * (midpoint of MIN_REACH and MAX_REACH) so we don't lock onto a mob
     * that's pressed against our face. Targets outside [0, SCAN_RANGE]
     * are ignored. Targets inside MIN_REACH are still considered (so we
     * can trigger autoStepBack) but ranked worst.
     */
    public static Entity findTarget() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return null;

        AABB box = mc.player.getBoundingBox().inflate(SCAN_RANGE);
        Entity best = null;
        double bestScore = Double.MAX_VALUE;
        double sweet = (MIN_REACH + MAX_REACH) * 0.5;

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

            // Eye → hitbox nearest point (not center-to-center) so MIN/MAX
            // reach and the sweet-spot score match the server's reach check.
            double dist = Math.sqrt(KillAuraMod.reachDistSq(mc.player, e));
            if (dist > SCAN_RANGE) continue;

            // Score = distance from the sweet spot. Penalty if inside MIN_REACH
            // so a far-but-attackable target wins over a too-close one.
            double score = Math.abs(dist - sweet);
            if (dist < MIN_REACH) score += 100;
            if (score < bestScore) {
                bestScore = score;
                best = e;
            }
        }
        glowTarget = best;
        return best;
    }

    /**
     * Silent-aim rotations toward a target's BODY CENTER. Body center
     * (not head, not feet) maximises spear damage because the view-angle
     * factor peaks when the crosshair points at the entity's centroid.
     */
    public static float[] calcRotations(Entity from, Entity to) {
        double dx = to.getX() - from.getX();
        double dy = (to.getY() + to.getBbHeight() * 0.5) - (from.getY() + from.getEyeHeight());
        double dz = to.getZ() - from.getZ();
        double h = Math.sqrt(dx * dx + dz * dz);
        float yaw   = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(-dx, dz)));
        float pitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(dy, h)), -90f, 90f);
        return new float[]{yaw, pitch};
    }

    /** Horizontal speed in blocks/tick — used by AUTO mode to pick jab vs charge. */
    public static double horizontalSpeed() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return 0;
        Vec3 v = mc.player.getDeltaMovement();
        return Math.sqrt(v.x * v.x + v.z * v.z);
    }

    /** AUTO mode picks CHARGE when the player (or their mount) is moving fast
     *  enough that the speed bonus actually pays off; otherwise JAB. */
    public static int effectiveMode() {
        if (mode != MODE_AUTO) return mode;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && mc.player.getVehicle() != null) return MODE_CHARGE;
        return horizontalSpeed() > 0.15 ? MODE_CHARGE : MODE_JAB;
    }

    // ── Charge key holding ───────────────────────────────────────────────────
    public static void startCharge() {
        if (charging) return;
        Minecraft.getInstance().options.keyAttack.setDown(true);
        charging = true;
        chargeTicks = 0;
    }

    public static void stopCharge() {
        if (!charging) return;
        Minecraft.getInstance().options.keyAttack.setDown(false);
        charging = false;
        chargeTicks = 0;
    }

    public static void tickCharge() {
        if (charging) chargeTicks++;
    }

    // ── Step-back assistance ─────────────────────────────────────────────────
    // We hold the back key for one tick per call. Mixin clears it on the next
    // tick if the situation no longer warrants it.
    private static boolean steppingBack = false;

    public static void startStepBack() {
        if (steppingBack) return;
        Minecraft.getInstance().options.keyDown.setDown(true);
        steppingBack = true;
    }

    public static void stopStepBack() {
        if (!steppingBack) return;
        Minecraft.getInstance().options.keyDown.setDown(false);
        steppingBack = false;
    }
}
