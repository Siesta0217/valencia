package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;

/**
 * 2D HUD overlay for entity nametags.
 *
 * Pipeline per frame:
 *   1. Projection.begin() — snapshot camera + projection matrix.
 *   2. For each targeted living entity:
 *        a. Compute anchor at top-of-head + small world offset.
 *        b. Projection.projectPoint() — reject if behind near plane.
 *        c. Reject if anchor is far outside the viewport (with margin).
 *        d. Compute distance-aware scale factor.
 *        e. Render the tag panel centred on the anchor.
 *
 * The panel is drawn under a translate+scale transform so all per-tag
 * layout math is in unscaled pixels — anchored at (0, 0), bottom-aligned.
 */
public final class NameTagRenderer {

    /** Anchor distance above the bounding-box top, in world blocks. */
    private static final double ANCHOR_WORLD_OFFSET_Y = 0.45;

    /** Reject tags whose anchor lands further than this many pixels off-screen. */
    private static final int OFFSCREEN_MARGIN_PX = 96;

    /** 4 armor slots + 2 hand slots. */
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final EquipmentSlot[] HAND_SLOTS = {
        EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };

    /** Per-frame scratch — HUD render is single-threaded. */
    private static final int[] ANCHOR_TMP = new int[2];

    private NameTagRenderer() {}

    public static void render(GuiGraphics g) {
        if (!NameTagMod.isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        Projection.Frame frame = Projection.begin(partialTick);
        Font font = mc.font;

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!NameTagMod.targets(e)) continue;
            LivingEntity le = (LivingEntity) e;

            Vec3 renderPos = e.getPosition(partialTick);
            AABB renderBox = e.getBoundingBox().move(
                renderPos.x - e.getX(),
                renderPos.y - e.getY(),
                renderPos.z - e.getZ()
            );
            double ax = renderPos.x;
            double ay = renderBox.maxY + ANCHOR_WORLD_OFFSET_Y;
            double az = renderPos.z;

            if (!Projection.projectPoint(frame, ax, ay, az, ANCHOR_TMP)) continue;
            int sx = ANCHOR_TMP[0];
            int sy = ANCHOR_TMP[1];
            if (isOffscreen(sx, sy, frame.viewW, frame.viewH)) continue;

            Vec3 eye = e.getEyePosition(partialTick);
            double dxw = eye.x - frame.camX;
            double dyw = eye.y - frame.camY;
            double dzw = eye.z - frame.camZ;
            double dist = Math.sqrt(dxw * dxw + dyw * dyw + dzw * dzw);

            float scale = NameTagMod.scale * distanceScale(dist);
            drawTag(g, font, le, sx, sy, scale);
        }
    }

    /** Smooth distance attenuation, clamped so close ≠ huge and far ≠ unreadable. */
    private static float distanceScale(double dist) {
        double f = 10.0 / Math.max(5.0, dist);
        if (f < 0.5) f = 0.5;
        if (f > 1.2) f = 1.2;
        return (float) f;
    }

    private static boolean isOffscreen(int x, int y, int viewW, int viewH) {
        return x < -OFFSCREEN_MARGIN_PX
            || y < -OFFSCREEN_MARGIN_PX
            || x > viewW + OFFSCREEN_MARGIN_PX
            || y > viewH + OFFSCREEN_MARGIN_PX;
    }

    // ─── tag rendering ──────────────────────────────────────────────────

    private static void drawTag(GuiGraphics g, Font font, LivingEntity e,
                                int anchorX, int anchorY, float scale) {
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(anchorX, anchorY);
        pose.scale(scale, scale);

        ModConfig cfg = ModConfig.get();
        String name = e.getName().getString();
        float hp = e.getHealth();
        float max = e.getMaxHealth();
        float absorb = e.getAbsorptionAmount();
        int armorVal = e.getArmorValue();

        boolean showArmor = NameTagMod.showArmor;
        boolean showHands = NameTagMod.showHands;
        boolean showHpBar = NameTagMod.showHpBar;
        boolean showHpText = NameTagMod.showHpText;
        boolean showDurability = NameTagMod.showDurability;

        // Header text widths
        int nameW = font.width(name);
        String hpText = showHpText ? formatHp(hp, max, absorb) : "";
        int hpTextW = showHpText ? font.width(hpText) + 4 : 0;
        String armorText = armorVal > 0 ? " §7[§b" + armorVal + "§7]" : "";
        int armorTextW = font.width(armorText);
        int headerW = nameW + hpTextW + armorTextW;

        // Icon row width
        final int iconSize = 16;
        final int iconGap = 1;
        int iconCount = (showArmor ? ARMOR_SLOTS.length : 0) + (showHands ? HAND_SLOTS.length : 0);
        int iconsW = iconCount > 0 ? iconCount * iconSize + (iconCount - 1) * iconGap : 0;

        // Panel size
        final int padX = 3;
        final int padY = 2;
        final int rowGap = 2;
        int contentW = Math.max(headerW, iconsW);
        int contentH = 9
            + (showHpBar ? 3 + rowGap : rowGap)
            + (iconCount > 0 ? iconSize : 0);
        int boxW = contentW + padX * 2;
        int boxH = contentH + padY * 2;
        int boxX = -boxW / 2;
        int boxY = -boxH;

        // Background + accent border
        int bgAlpha = clampInt(cfg.bgAlpha, 60, 220);
        int bg = bgAlpha << 24;
        int accent = 0x80000000
            | ((cfg.accentR & 0xFF) << 16)
            | ((cfg.accentG & 0xFF) << 8)
            |  (cfg.accentB & 0xFF);
        g.fill(boxX, boxY, boxX + boxW, boxY + boxH, bg);
        drawBorder(g, boxX, boxY, boxX + boxW, boxY + boxH, accent);

        // Header line
        int cursorY = boxY + padY;
        int headerX = -headerW / 2;
        g.drawString(font, name, headerX, cursorY, 0xFFFFFFFF, false);
        if (showHpText) {
            int hpCol = hpColor(hp / Math.max(1f, max));
            g.drawString(font, hpText, headerX + nameW + 4, cursorY, hpCol, false);
        }
        if (!armorText.isEmpty()) {
            g.drawString(font, armorText, headerX + nameW + hpTextW, cursorY, 0xFFFFFFFF, false);
        }
        cursorY += 9;

        // HP bar
        if (showHpBar) {
            int barW = Math.max(40, contentW);
            int barX = -barW / 2;
            float frac = Math.max(0f, Math.min(1f, hp / Math.max(1f, max)));
            g.fill(barX,     cursorY,     barX + barW,                            cursorY + 3, 0xFF202020);
            g.fill(barX + 1, cursorY + 1, barX + 1 + (int)((barW - 2) * frac),    cursorY + 2, hpColor(frac));
            cursorY += 3 + rowGap;
        } else {
            cursorY += rowGap;
        }

        // Icon row
        if (iconCount > 0) {
            int iconX = -iconsW / 2;
            if (showArmor) {
                for (EquipmentSlot slot : ARMOR_SLOTS) {
                    drawSlot(g, font, e, slot, iconX, cursorY, showDurability);
                    iconX += iconSize + iconGap;
                }
            }
            if (showHands) {
                for (EquipmentSlot slot : HAND_SLOTS) {
                    drawSlot(g, font, e, slot, iconX, cursorY, showDurability);
                    iconX += iconSize + iconGap;
                }
            }
        }

        pose.popMatrix();
    }

    private static void drawBorder(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1,     y1,     x2,     y1 + 1, color);
        g.fill(x1,     y2 - 1, x2,     y2,     color);
        g.fill(x1,     y1,     x1 + 1, y2,     color);
        g.fill(x2 - 1, y1,     x2,     y2,     color);
    }

    private static void drawSlot(GuiGraphics g, Font font, LivingEntity e,
                                  EquipmentSlot slot, int x, int y, boolean showDurability) {
        g.fill(x, y, x + 16, y + 16, 0x60000000);
        ItemStack stack = e.getItemBySlot(slot);
        if (stack.isEmpty()) return;
        g.renderItem(stack, x, y);
        if (showDurability) g.renderItemDecorations(font, stack, x, y);
    }

    private static String formatHp(float hp, float max, float absorb) {
        String base = String.format("%.0f/%.0f", hp, max);
        return absorb > 0f ? base + "+" + (int) absorb : base;
    }

    private static int hpColor(float frac) {
        if (frac > 0.66f) return 0xFF55FF55;
        if (frac > 0.33f) return 0xFFFFAA00;
        return 0xFFFF5555;
    }

    private static int clampInt(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
