package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.CraftingTableBlock;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;

public class ScaffoldMod {

    private static boolean enabled = false;

    // ── User-facing settings ──────────────────────────────────────────────────
    public static boolean tower       = false;  // jump-up + auto place under foot
    public static boolean towerMove   = true;   // Tower: keep horizontal velocity (false = lock in place, straight up)
    public static boolean autoSwitch  = true;
    public static boolean switchBack  = true;
    public static int     placeDelay  = 0;      // ticks (0 = every tick)
    public static boolean lookAhead   = true;   // place predicted next-tick foot if current foot already solid
    public static boolean silentRot   = true;   // send rotation packet aimed at hit point before useItemOn

    // ── Internal constants (always on; not exposed in GUI) ────────────────────
    // Skip falling / container blocks so we never bridge with sand or place a
    // chest under our feet. Hard-coded — there's no reason a user would want
    // these off.
    // ──────────────────────────────────────────────────────────────────────────

    // ── Runtime state ─────────────────────────────────────────────────────────
    private static int placeTimer = 0;
    private static int prevSlot   = -1;

    // Inventory.selected is private in Lunar 1.21.11 — reflect once, cache.
    private static final Field SELECTED_FIELD;
    static {
        Field f = null;
        try { f = Inventory.class.getDeclaredField("selected"); f.setAccessible(true); }
        catch (Exception ignored) {}
        SELECTED_FIELD = f;
    }

    private static int  getSelected(Inventory inv)            { try { return SELECTED_FIELD == null ? 0 : SELECTED_FIELD.getInt(inv); } catch (Exception e) { return 0; } }
    private static void setSelected(Inventory inv, int slot)  { if (SELECTED_FIELD == null) return; try { SELECTED_FIELD.setInt(inv, slot); } catch (Exception ignored) {} }

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    public static boolean isActive() {
        if (!enabled) return false;
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null && mc.gameMode != null;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();

        if (!isActive()) { restoreSlot(mc); return; }

        LocalPlayer p = mc.player;
        if (p == null || mc.level == null) return;

        // Tower: stamp jump velocity while on ground — pair with Scaffold's
        // auto-place to ride a column of blocks up. towerMove=true keeps the
        // horizontal velocity so WASD steers the tower; false zeroes it so
        // the player goes straight up regardless of input.
        if (tower && p.onGround()) {
            Vec3 v = p.getDeltaMovement();
            if (towerMove) p.setDeltaMovement(v.x, 0.42, v.z);
            else            p.setDeltaMovement(0,   0.42, 0);
        }

        if (placeTimer > 0) { placeTimer--; return; }

        // Choose target: current foot if open, else (when lookAhead) the cell
        // we'll arrive at next tick based on current velocity. This is what
        // lets sprinting / sprint-jumping not drop the player.
        BlockPos curFoot = BlockPos.containing(p.position().subtract(0, 1, 0));
        BlockPos target = null;
        if (mc.level.getBlockState(curFoot).canBeReplaced()) {
            target = curFoot;
        } else if (lookAhead) {
            Vec3 nextPos = p.position().add(p.getDeltaMovement());
            BlockPos nextFoot = BlockPos.containing(nextPos.subtract(0, 1, 0));
            if (!nextFoot.equals(curFoot) && mc.level.getBlockState(nextFoot).canBeReplaced()) {
                target = nextFoot;
            }
        }

        if (target == null) { if (switchBack) restoreSlot(mc); return; }

        BlockHitResult hit = findPlacement(mc.level, target);
        if (hit == null) return;

        int slot = findBlockSlot(p);
        if (slot == -1) return;

        Inventory inv = p.getInventory();
        int curSlot = getSelected(inv);
        if (autoSwitch && curSlot != slot) {
            if (prevSlot == -1) prevSlot = curSlot;
            setSelected(inv, slot);
        }

        if (silentRot) sendRotation(mc, p, hit);

        mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, hit);
        p.swing(InteractionHand.MAIN_HAND);

        if (silentRot) restoreRotation(mc, p);

        placeTimer = Math.max(0, placeDelay);

        if (switchBack) restoreSlot(mc);
    }

    private static void restoreSlot(Minecraft mc) {
        if (prevSlot == -1) return;
        if (mc.player != null) setSelected(mc.player.getInventory(), prevSlot);
        prevSlot = -1;
    }

    /** Send a server-side rotation aimed at the hit point. */
    private static void sendRotation(Minecraft mc, LocalPlayer p, BlockHitResult hit) {
        if (mc.getConnection() == null) return;
        Vec3 eye = p.getEyePosition();
        Vec3 tgt = hit.getLocation();
        double dx = tgt.x - eye.x, dy = tgt.y - eye.y, dz = tgt.z - eye.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        float yaw   = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, h));
        try {
            mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(
                yaw, pitch, p.onGround(), p.horizontalCollision
            ));
        } catch (Exception ignored) {}
    }

    private static void restoreRotation(Minecraft mc, LocalPlayer p) {
        if (mc.getConnection() == null) return;
        try {
            mc.getConnection().send(new ServerboundMovePlayerPacket.Rot(
                p.getYRot(), p.getXRot(), p.onGround(), p.horizontalCollision
            ));
        } catch (Exception ignored) {}
    }

    /** Find a solid neighbor of targetPos to use as the reference block. */
    private static BlockHitResult findPlacement(Level level, BlockPos targetPos) {
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

            Direction face = d.getOpposite();
            Vec3 hitVec = Vec3.atCenterOf(neighbor).add(
                face.getStepX() * 0.5,
                face.getStepY() * 0.5,
                face.getStepZ() * 0.5
            );
            return new BlockHitResult(hitVec, face, neighbor, false);
        }
        return null;
    }

    /** Pick the hotbar slot with the most blocks, skipping junk types. */
    private static int findBlockSlot(LocalPlayer player) {
        Inventory inv = player.getInventory();
        int best = -1;
        int bestCount = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (!(s.getItem() instanceof BlockItem bi)) continue;
            Block b = bi.getBlock();
            if (b instanceof FallingBlock) continue;
            if (b instanceof ChestBlock || b instanceof BarrelBlock
                || b instanceof EnderChestBlock || b instanceof AbstractFurnaceBlock
                || b instanceof CraftingTableBlock) continue;
            if (s.getCount() > bestCount) {
                best = i;
                bestCount = s.getCount();
            }
        }
        return best;
    }
}
