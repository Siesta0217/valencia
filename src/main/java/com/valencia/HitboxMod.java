package com.valencia;

/**
 * Hitbox — inflates other entities' bounding boxes so melee raycasts find
 * them more easily. Used by the HitboxMixin which hooks
 * {@code Entity.getBoundingBox()} RETURN and grows the AABB by {@link #expand}.
 *
 * <p>Why this helps: vanilla melee pick is
 * {@code ProjectileUtil.getEntityHitResult(..., bbox.inflate(pickRadius), ...)}
 * — a bigger source bbox = bigger hit volume. Edge-of-reach swings that
 * would have missed now register.
 *
 * <p>Limits to be aware of:
 * <ul>
 *   <li>1.20+ vanilla servers re-validate that the player's view actually
 *       intersects the real hitbox. Expanding too much (≥ 0.5) flags as
 *       a hitbox cheat on anti-cheat servers.</li>
 *   <li>Inflating affects ALL callers of {@code getBoundingBox()} —
 *       collision physics, AI pathfinding, frustum culling. Small values
 *       are mostly invisible; large values produce weird side effects.</li>
 *   <li>SpearAura's silent-aim path is unaffected because it doesn't rely
 *       on view-raycast hit detection — this mod helps manual melee.</li>
 * </ul>
 */
public class HitboxMod {

    private static boolean enabled = false;

    /** Amount to inflate AABB by (each axis, each side). 0.3 ≈ +60 cm wider. */
    public static float expand = 0.3f;

    /** Restrict expansion to players only (PvP focus, less side-effects). */
    public static boolean playersOnly = true;

    public static boolean isEnabled() { return enabled; }
    public static void toggle() { enabled = !enabled; }
}
