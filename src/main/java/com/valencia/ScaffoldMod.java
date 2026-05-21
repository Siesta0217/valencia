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
import java.util.LinkedHashSet;
import java.util.Set;

public class ScaffoldMod {

    private static boolean enabled = false;

    // ── Behavior ──────────────────────────────────────────────────────────────
    public static boolean tower         = false;
    public static boolean autoSwitch    = true;
    public static boolean switchBack    = true;
    public static int     placeDelay    = 0;   // ticks (0 = every tick)
    public static boolean sneakWhile    = false;
    public static boolean safeWalk      = true;

    // ── Aggressive / vanilla-server tuning ────────────────────────────────────
    public static int     blocksPerTick  = 3;     // max useItemOn calls per tick
    public static boolean lookAhead      = true;  // predict next-tick foot pos
    public static boolean airPlace       = true;  // place even when off-ground
    public static boolean silentRot      = true;  // send rotation packet before useItemOn
    public static boolean skipHeavy      = true;  // skip FallingBlock items
    public static boolean skipContainer  = true;  // skip chests/barrels/furnaces
    public static int     extendRadius   = 0;     // extra placements around foot

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

        // Tower: stamp jump velocity while on ground
        if (tower && p.onGround()) {
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(v.x, 0.42, v.z);
        }

        if (placeTimer > 0) { placeTimer--; return; }

        // Air-place gate
        if (!airPlace && !p.onGround()) {
            if (switchBack) restoreSlot(mc);
            return;
        }

        // Collect candidate positions
        Set<BlockPos> candidates = new LinkedHashSet<>();

        // Current foot (block directly under player)
        BlockPos curFoot = BlockPos.containing(p.position().subtract(0, 1, 0));
        candidates.add(curFoot);

        // Predict next-tick foot position using current velocity
        if (lookAhead) {
            Vec3 nextPos = p.position().add(p.getDeltaMovement());
            BlockPos nextFoot = BlockPos.containing(nextPos.subtract(0, 1, 0));
            candidates.add(nextFoot);
        }

        // Extend: rectangular radius around current foot
        int r = Math.max(0, extendRadius);
        if (r > 0) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx == 0 && dz == 0) continue;
                    candidates.add(curFoot.offset(dx, 0, dz));
                }
            }
        }

        int placed = 0;
        int maxPerTick = Math.max(1, blocksPerTick);

        for (BlockPos target : candidates) {
            if (placed >= maxPerTick) break;

            BlockState cur = mc.level.getBlockState(target);
            if (!cur.canBeReplaced()) continue;

            BlockHitResult hit = findPlacement(mc.level, target);
            if (hit == null) continue;

            int slot = findBlockSlot(p);
            if (slot == -1) break;

            Inventory inv = p.getInventory();
            int curSlot = getSelected(inv);
            if (autoSwitch && curSlot != slot) {
                if (prevSlot == -1) prevSlot = curSlot;
                setSelected(inv, slot);
            }

            if (silentRot) sendRotation(mc, p, hit);

            mc.gameMode.useItemOn(p, InteractionHand.MAIN_HAND, hit);
            p.swing(InteractionHand.MAIN_HAND);
            placed++;
        }

        // Reset look so subsequent vanilla packets carry the player's real view
        if (silentRot && placed > 0) restoreRotation(mc, p);

        placeTimer = placed > 0 ? Math.max(0, placeDelay) : 0;

        if (switchBack) restoreSlot(mc);
    }

    private static void restoreSlot(Minecraft mc) {
        if (prevSlot == -1) return;
        if (mc.player != null) setSelected(mc.player.getInventory(), prevSlot);
        prevSlot = -1;
    }

    /**
     * Send a server-side rotation packet aimed at the hit point. Lets vanilla
     * / Paper's reach + facing checks accept the upcoming useItemOn.
     */
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

    /**
     * Find a reference block adjacent to targetPos that we can right-click to
     * place a block at targetPos. Prefers DOWN, then horizontals, then UP.
     */
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

    private static int findBlockSlot(LocalPlayer player) {
        Inventory inv = player.getInventory();
        int best = -1;
        int bestCount = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty()) continue;
            if (!(s.getItem() instanceof BlockItem bi)) continue;
            Block b = bi.getBlock();
            if (skipHeavy && b instanceof FallingBlock) continue;
            if (skipContainer && (b instanceof ChestBlock
                               || b instanceof BarrelBlock
                               || b instanceof EnderChestBlock
                               || b instanceof AbstractFurnaceBlock
                               || b instanceof CraftingTableBlock)) continue;
            // Pick the slot with the most blocks to avoid running out mid-bridge
            if (s.getCount() > bestCount) {
                best = i;
                bestCount = s.getCount();
            }
        }
        return best;
    }
}
