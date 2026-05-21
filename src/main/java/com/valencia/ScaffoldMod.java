package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
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
    public static float   towerSpeed  = 0.5f;   // y velocity per tick when Tower active (0.42 = vanilla jump, 1.0 = ~20 b/s)
    public static boolean autoSwitch  = true;
    public static boolean switchBack  = true;
    public static int     placeDelay  = 0;      // ticks (0 = every tick)
    public static boolean silentRot   = true;   // send rotation packet aimed at hit point before useItemOn
    public static boolean fakeHand    = false;  // server-only slot swap — client keeps holding original item

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

        // Tower: stamp y velocity while the jump key is held. Add 0.08 to
        // compensate for vanilla gravity (effective rise matches towerSpeed).
        //
        // Tower Move ON + no WASD held = also zero horizontal. Without this,
        // residual sprint momentum (drag 0.91/tick = ~10 ticks to decay)
        // drifts the player sideways while rising. At low Tower Spd the drift
        // outpaces vertical rise, so each scaffold placement is at a new
        // (x, z) where no neighbor of the previous column exists →
        // findPlacement returns null → block never lands → player flies up.
        if (tower && mc.options.keyJump.isDown() && towerSpeed > 0) {
            Vec3 v = p.getDeltaMovement();
            double y = towerSpeed + 0.08;
            boolean wasdHeld = mc.options.keyUp.isDown()
                            || mc.options.keyDown.isDown()
                            || mc.options.keyLeft.isDown()
                            || mc.options.keyRight.isDown();
            if (towerMove && wasdHeld) {
                p.setDeltaMovement(v.x, y, v.z);
            } else {
                p.setDeltaMovement(0, y, 0);
            }
        }

        if (placeTimer > 0) { placeTimer--; return; }

        // Place only at curFoot when it's empty. No look-ahead — predicting
        // the next-tick foot tries to place a block that the player hitbox
        // still overlaps (player feet at y=N+0.3 inside block N [N, N+1]),
        // and vanilla servers reject overlap-with-entity placements. The
        // scaffold thinks it placed but nothing actually lands, and Tower vy
        // keeps lifting the player off the column.
        //
        // Waiting until curFoot is genuinely empty (i.e. player feet have
        // cleared the previous block's top) means every useItemOn we send
        // is clean: no overlap, no rejection, column always grows.
        BlockPos curFoot = BlockPos.containing(p.position().subtract(0, 1, 0));
        BlockPos target = mc.level.getBlockState(curFoot).canBeReplaced() ? curFoot : null;

        if (target == null) { if (switchBack) restoreSlot(mc); return; }

        if (placeAt(mc, p, target)) placeTimer = Math.max(0, placeDelay);

        if (switchBack && !fakeHand) restoreSlot(mc);
    }

    /** Place one block at target. Returns true on success. */
    private static boolean placeAt(Minecraft mc, LocalPlayer p, BlockPos target) {
        BlockHitResult hit = findPlacement(mc.level, target);
        if (hit == null) return false;

        int slot = findBlockSlot(p);
        if (slot == -1) return false;

        Inventory inv = p.getInventory();
        int curSlot = getSelected(inv);
        boolean swapped = false;
        if (autoSwitch && curSlot != slot) {
            if (fakeHand) {
                sendCarried(mc, slot);
            } else {
                if (prevSlot == -1) prevSlot = curSlot;
                setSelected(inv, slot);
            }
            swapped = true;
        }

        if (silentRot) sendRotation(mc, p, hit);

        mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, hit);
        p.swing(InteractionHand.MAIN_HAND);

        if (silentRot) restoreRotation(mc, p);

        if (swapped && fakeHand) {
            sendCarried(mc, curSlot);
        }
        return true;
    }

    private static void sendCarried(Minecraft mc, int slot) {
        if (mc.getConnection() == null) return;
        try { mc.getConnection().send(new ServerboundSetCarriedItemPacket(slot)); }
        catch (Exception ignored) {}
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
