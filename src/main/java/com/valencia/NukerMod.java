package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Nuker — auto-break every reachable block around you, nearest first, one at a
 * time through the vanilla destroy flow (so the server validates each break at
 * its legit speed). Auto-equips the fastest tool per block (same trick as
 * AutoTool). On vanilla it mines at normal per-block speed but hands-free;
 * obvious to anti-cheat, so use with that in mind.
 */
public final class NukerMod {

    private static boolean enabled = false;
    public static float range = 4.5f;   // block reach (clamped to 5)

    private static BlockPos current;    // block currently being dug

    private NukerMod() {}

    public static boolean isEnabled() { return enabled; }
    public static void toggle() { enabled = !enabled; if (!enabled) current = null; }

    public static boolean isActive() {
        if (!enabled) return false;
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null && mc.gameMode != null;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (!isActive() || mc.screen != null) return;
        LocalPlayer p = mc.player;
        Level level = mc.level;

        Vec3 eye = p.getEyePosition();
        double r = Math.min(range, 5.0);
        double r2 = r * r;
        int ri = (int) Math.ceil(r);
        BlockPos base = p.blockPosition();

        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -ri; dx <= ri; dx++) {
            for (int dy = -ri; dy <= ri; dy++) {
                for (int dz = -ri; dz <= ri; dz++) {
                    m.set(base.getX() + dx, base.getY() + dy, base.getZ() + dz);
                    BlockState st = level.getBlockState(m);
                    if (st.isAir() || !st.getFluidState().isEmpty()) continue;
                    if (st.getDestroySpeed(level, m) < 0) continue;   // bedrock / unbreakable
                    double d2 = eye.distanceToSqr(m.getX() + 0.5, m.getY() + 0.5, m.getZ() + 0.5);
                    if (d2 <= r2 && d2 < bestDist) { bestDist = d2; best = m.immutable(); }
                }
            }
        }
        if (best == null) { current = null; return; }

        equipBest(mc, p, level.getBlockState(best));

        Direction face = Direction.getNearest(
            (int) Math.round(eye.x - (best.getX() + 0.5)),
            (int) Math.round(eye.y - (best.getY() + 0.5)),
            (int) Math.round(eye.z - (best.getZ() + 0.5)), Direction.UP);

        if (!best.equals(current)) {
            current = best;
            mc.gameMode.startDestroyBlock(best, face);
        } else {
            mc.gameMode.continueDestroyBlock(best, face);
        }
        p.swing(InteractionHand.MAIN_HAND);
    }

    /** Auto-equip the fastest tool for {@code st} (server-synced). */
    private static void equipBest(Minecraft mc, LocalPlayer p, BlockState st) {
        Inventory inv = p.getInventory();
        int cur = inv.getSelectedSlot();
        int best = AutoToolMod.bestSlot(inv, st, cur);
        if (best != cur) AutoToolMod.select(mc, p, best);
    }
}
