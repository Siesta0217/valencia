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
    private static int prevSlot = -1;   // hotbar slot to restore when we stop

    private NukerMod() {}

    public static boolean isEnabled() { return enabled; }
    public static void toggle() { enabled = !enabled; if (!enabled) { current = null; restoreSlot(); } }

    public static boolean isActive() {
        if (!enabled) return false;
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null && mc.gameMode != null;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (!isActive() || mc.screen != null) return;
        // Yield while AutoEat is mid-bite — swapping to a tool would take the
        // food out of hand and stall the eat. Resume digging when it's done.
        if (AutoEatMod.isEating()) return;
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
                    // exposed() last: only evaluated when this block would become
                    // the new nearest, so it runs a bounded number of times.
                    if (d2 <= r2 && d2 < bestDist && exposed(level, m)) { bestDist = d2; best = m.immutable(); }
                }
            }
        }
        if (best == null) { current = null; restoreSlot(); return; }

        equipBest(mc, p, level.getBlockState(best));

        Direction face = Direction.getNearest(
            (int) Math.round(eye.x - (best.getX() + 0.5)),
            (int) Math.round(eye.y - (best.getY() + 0.5)),
            (int) Math.round(eye.z - (best.getZ() + 0.5)), Direction.UP);

        if (!best.equals(current)) {
            current = best;
            mc.gameMode.startDestroyBlock(best, face);
            // Swing only when starting a new block, not every tick — the dig
            // continues without re-swinging, so this cuts ~20 swing packets/sec.
            p.swing(InteractionHand.MAIN_HAND);
        } else {
            mc.gameMode.continueDestroyBlock(best, face);
        }
    }

    /** True if at least one face of {@code pos} touches air / fluid / a
     *  non-solid block — i.e. the block is actually reachable, not sealed
     *  inside terrain. Skipping enclosed blocks avoids wasted (server-rejected)
     *  breaks and the most anti-cheat-obvious behavior. */
    private static boolean exposed(Level level, BlockPos pos) {
        for (Direction d : Direction.values()) {
            BlockPos np = pos.relative(d);
            BlockState n = level.getBlockState(np);
            if (n.isAir() || !n.getFluidState().isEmpty()) return true;
            if (n.getCollisionShape(level, np).isEmpty()) return true;
        }
        return false;
    }

    private static void restoreSlot() {
        if (prevSlot == -1) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) AutoToolMod.select(mc, mc.player, prevSlot);
        prevSlot = -1;
    }

    /** Auto-equip the fastest tool for {@code st} (server-synced). */
    private static void equipBest(Minecraft mc, LocalPlayer p, BlockState st) {
        Inventory inv = p.getInventory();
        int cur = inv.getSelectedSlot();
        int best = AutoToolMod.bestSlot(inv, st, cur);
        if (best != cur) {
            if (prevSlot == -1) prevSlot = cur;
            AutoToolMod.select(mc, p, best);
        }
    }
}
