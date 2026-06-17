package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * AutoTool — while mining, auto-switch the hotbar to the fastest tool for the
 * block under the crosshair. Sends ServerboundSetCarriedItemPacket so the
 * SERVER also sees the switch (otherwise the server keeps the old tool's
 * break time and nothing speeds up). Server-accepted on vanilla — this is the
 * only thing that genuinely speeds single-block mining there.
 */
public final class AutoToolMod {

    private static boolean enabled = false;
    /** Restore the previous hotbar slot when you stop mining. */
    public static boolean switchBack = true;

    private static int prevSlot = -1;

    private AutoToolMod() {}

    public static boolean isEnabled() { return enabled; }
    public static void toggle() { enabled = !enabled; if (!enabled) restore(); }

    public static boolean isActive() {
        if (!enabled) return false;
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null && mc.gameMode != null;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (!isActive() || mc.screen != null) return;

        if (!mc.options.keyAttack.isDown()
                || !(mc.hitResult instanceof BlockHitResult bhr)
                || bhr.getType() != HitResult.Type.BLOCK) {
            if (switchBack) restore();
            return;
        }

        BlockState state = mc.level.getBlockState(bhr.getBlockPos());
        LocalPlayer p = mc.player;
        Inventory inv = p.getInventory();
        int cur = inv.getSelectedSlot();
        int best = bestSlot(inv, state, cur);
        if (best != cur) {
            if (prevSlot == -1) prevSlot = cur;
            select(mc, p, best);
        }
    }

    /** Hotbar slot whose item breaks {@code state} fastest (ties keep current). */
    public static int bestSlot(Inventory inv, BlockState state, int cur) {
        float bestSpeed = inv.getItem(cur).getDestroySpeed(state);
        int best = cur;
        for (int i = 0; i < 9; i++) {
            float s = inv.getItem(i).getDestroySpeed(state);
            if (s > bestSpeed) { bestSpeed = s; best = i; }
        }
        return best;
    }

    public static void select(Minecraft mc, LocalPlayer p, int slot) {
        p.getInventory().setSelectedSlot(slot);
        if (mc.getConnection() != null) mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot));
    }

    private static void restore() {
        if (prevSlot == -1) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) select(mc, mc.player, prevSlot);
        prevSlot = -1;
    }
}
