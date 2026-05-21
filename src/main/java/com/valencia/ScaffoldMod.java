package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;

import java.lang.reflect.Field;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class ScaffoldMod {

    private static boolean enabled = false;

    // Tunables (configured from GUI)
    public static boolean tower       = false;  // auto jump while on ground -> tower up
    public static boolean autoSwitch  = true;   // switch to block slot before placing
    public static boolean switchBack  = true;   // restore previous slot after place
    public static int     placeDelay  = 1;      // ticks between place attempts
    public static boolean sneakWhile  = false;  // hold sneak while placing
    public static boolean safeWalk    = true;   // sneak at ledges when tower=OFF

    // Runtime state
    private static int placeTimer = 0;
    private static int prevSlot   = -1;

    // Inventory.selected is private in this Lunar Client build; reach it via
    // reflection. Field reference is cached statically.
    private static final Field SELECTED_FIELD;
    static {
        Field f = null;
        try { f = Inventory.class.getDeclaredField("selected"); f.setAccessible(true); }
        catch (Exception ignored) {}
        SELECTED_FIELD = f;
    }

    private static int getSelected(Inventory inv) {
        if (SELECTED_FIELD == null) return 0;
        try { return SELECTED_FIELD.getInt(inv); } catch (Exception e) { return 0; }
    }
    private static void setSelected(Inventory inv, int slot) {
        if (SELECTED_FIELD == null) return;
        try { SELECTED_FIELD.setInt(inv, slot); } catch (Exception ignored) {}
    }

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    public static boolean isActive() {
        if (!enabled) return false;
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null && mc.gameMode != null;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();

        if (!isActive()) {
            restoreSlot(mc);
            return;
        }

        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;

        // Tower mode: stamp jump velocity while grounded
        if (tower && p.onGround()) {
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(v.x, 0.42, v.z);
        }

        if (placeTimer > 0) { placeTimer--; return; }

        // Foot block — the cell directly below the player's feet
        BlockPos targetPos = BlockPos.containing(p.position().subtract(0, 1, 0));
        BlockState cur = mc.level.getBlockState(targetPos);
        if (!cur.canBeReplaced()) {
            // Already solid — nothing to do. Restore slot if we were holding swapped.
            if (switchBack) restoreSlot(mc);
            return;
        }

        BlockHitResult hit = findPlacement(mc.level, targetPos);
        if (hit == null) return;

        int slot = findBlockSlot(p);
        if (slot == -1) return;

        Inventory inv = p.getInventory();
        int curSlot = getSelected(inv);
        if (autoSwitch && curSlot != slot) {
            if (prevSlot == -1) prevSlot = curSlot;
            setSelected(inv, slot);
        }

        mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, hit);
        p.swing(InteractionHand.MAIN_HAND);

        placeTimer = Math.max(0, placeDelay);

        if (switchBack) restoreSlot(mc);
    }

    private static void restoreSlot(Minecraft mc) {
        if (prevSlot == -1) return;
        if (mc.player != null) setSelected(mc.player.getInventory(), prevSlot);
        prevSlot = -1;
    }

    /**
     * Find a reference block neighboring targetPos that we can right-click to
     * place a block at targetPos. Returns the BlockHitResult to feed useItemOn.
     */
    private static BlockHitResult findPlacement(Level level, BlockPos targetPos) {
        // Try DOWN first (most reliable for scaffolding from a block below),
        // then horizontal, then UP last.
        Direction[] dirs = {
            Direction.DOWN,
            Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST,
            Direction.UP
        };
        for (Direction d : dirs) {
            BlockPos neighbor = targetPos.relative(d);
            BlockState ns = level.getBlockState(neighbor);
            if (ns.isAir()) continue;
            if (ns.getCollisionShape(level, neighbor).isEmpty()) continue;

            Direction face = d.getOpposite(); // face of neighbor pointing toward targetPos
            Vec3 hitVec = Vec3.atCenterOf(neighbor).add(
                face.getStepX() * 0.5,
                face.getStepY() * 0.5,
                face.getStepZ() * 0.5
            );
            return new BlockHitResult(hitVec, face, neighbor, false);
        }
        return null;
    }

    private static int findBlockSlot(LocalPlayer player) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < 9; i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                return i;
            }
        }
        return -1;
    }
}
