package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.gizmos.TextGizmo;
import net.minecraft.world.phys.Vec3;

import java.awt.Color;
import java.util.Map;

/**
 * Waypoints — named saved coordinates, rendered in-world as a vertical beam
 * plus a floating "name (123m)" label, and usable as `.nf goto <name>` targets.
 *
 * Chat commands (ChatMixin):
 *   .nf wp add <name>   save current position + dimension
 *   .nf wp del <name>   delete
 *   .nf wp list         list all with distance
 *   .nf goto <name>     fly there (ElytraGoto; cross-dimension coords auto ÷8/×8)
 *
 * Rendering rides the same vanilla Gizmos pipeline as ESP Hitbox
 * (DebugRenderer.emitGizmos + setAlwaysOnTop), so beams show through walls
 * and stay frame-perfect. Nether↔overworld waypoints are drawn at converted
 * coordinates; End waypoints only render in the End.
 */
public final class WaypointsMod {

    private static boolean enabled = true;

    /** Beam color per waypoint: stable hue from the name hash. */
    private static int colorFor(String name) {
        float hue = (Math.floorMod(name.hashCode(), 360)) / 360f;
        return Color.HSBtoRGB(hue, 0.65f, 1.0f) | 0xFF000000;
    }

    private WaypointsMod() {}

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    // ── data (lives in ModConfig.waypoints, persisted with the config) ──────

    public static Map<String, ModConfig.Wp> all() { return ModConfig.get().waypoints; }

    public static void add(String name, double x, double y, double z, String dim) {
        ModConfig.Wp wp = new ModConfig.Wp();
        wp.x = x; wp.y = y; wp.z = z; wp.dim = dim;
        ModConfig cfg = ModConfig.get();
        cfg.waypoints.put(name, wp);
        cfg.save();
    }

    public static boolean remove(String name) {
        ModConfig cfg = ModConfig.get();
        boolean removed = cfg.waypoints.remove(name) != null;
        if (removed) cfg.save();
        return removed;
    }

    private static boolean isNetherDim(String dim) { return dim != null && dim.contains("the_nether"); }
    private static boolean isEndDim(String dim)    { return dim != null && dim.contains("the_end"); }

    /**
     * Waypoint coords converted into the player's current dimension frame
     * (nether↔overworld ÷8/×8 on X/Z, Y untouched), or null if the waypoint
     * can't be represented here (End↔elsewhere).
     */
    public static double[] resolve(ModConfig.Wp wp) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        String cur = mc.level.dimension().toString();
        boolean curNether = isNetherDim(cur), curEnd = isEndDim(cur);
        boolean wpNether = isNetherDim(wp.dim), wpEnd = isEndDim(wp.dim);

        if (curEnd != wpEnd) return null;                    // End doesn't map
        double x = wp.x, z = wp.z;
        if (wpNether && !curNether)      { x *= 8; z *= 8; } // nether wp seen from OW
        else if (!wpNether && curNether) { x /= 8; z /= 8; } // OW wp seen from nether
        return new double[]{x, wp.y, z};
    }

    // ── rendering (called from ESPGizmoMixin's emitGizmos hook) ─────────────

    public static void emitGizmos(Minecraft mc) {
        if (!enabled || mc.level == null || mc.player == null) return;
        Map<String, ModConfig.Wp> wps = all();
        if (wps.isEmpty()) return;

        double px = mc.player.getX(), pz = mc.player.getZ();
        double eyeY = mc.player.getEyeY();

        for (Map.Entry<String, ModConfig.Wp> e : wps.entrySet()) {
            double[] pos = resolve(e.getValue());
            if (pos == null) continue;
            double x = pos[0], z = pos[2];
            int color = colorFor(e.getKey());

            // Full-height beam, through walls.
            Gizmos.line(new Vec3(x, -64, z), new Vec3(x, 320, z), color, 2.0f)
                  .setAlwaysOnTop();

            // Label at eye level so it's always on the horizon line.
            double dist = Math.sqrt((x - px) * (x - px) + (z - pz) * (z - pz));
            Gizmos.billboardText(
                String.format("%s (%dm)", e.getKey(), (int) dist),
                new Vec3(x, eyeY, z),
                TextGizmo.Style.forColorAndCentered(color)
            ).setAlwaysOnTop();
        }
    }
}
