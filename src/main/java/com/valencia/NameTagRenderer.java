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
 * Polished entity nametag overlay.
 *
 * Visual goals (v1.7.7):
 *   - Pseudo-rounded panel via corner-pixel shaving
 *   - Thin accent gradient stripe along the top edge
 *   - Drop-shadowed name, muted distance, HP-tinted HP text
 *   - Gradient HP bar (red → orange → green) with a 1-px top highlight
 *   - Compact armor / hands rows with a subtle divider between them
 *
 * Anchor: world point at (entity centre, bbox top + 0.45, entity centre)
 * projected via {@link Projection#projectPoint}. The whole panel is drawn
 * under a {@code translate + scale} transform so layout math stays in
 * unscaled pixels with the anchor at (0, 0) and the panel extending
 * upward from there.
 */
public final class NameTagRenderer {

    /** Anchor distance above the bounding-box top, in world blocks. */
    private static final double ANCHOR_WORLD_OFFSET_Y = 0.45;

    /** Reject tags whose anchor lands further than this many pixels off-screen. */
    private static final int OFFSCREEN_MARGIN_PX = 96;

    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final EquipmentSlot[] HAND_SLOTS = {
        EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };

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
            double dist = Math.sqrt(
                (eye.x - frame.camX) * (eye.x - frame.camX) +
                (eye.y - frame.camY) * (eye.y - frame.camY) +
                (eye.z - frame.camZ) * (eye.z - frame.camZ)
            );
            if (dist > NameTagMod.maxDistance) continue;

            float scale = NameTagMod.scale * distanceScale(dist);
            drawTag(g, font, le, sx, sy, scale, dist);
        }
    }

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
                                int anchorX, int anchorY, float scale, double dist) {
        Matrix3x2fStack pose = g.pose();
        pose.pushMatrix();
        pose.translate(anchorX, anchorY);
        pose.scale(scale, scale);

        ModConfig cfg = ModConfig.get();
        String name = e.getName().getString();
        float hp = e.getHealth();
        float max = Math.max(1f, e.getMaxHealth());
        float absorb = e.getAbsorptionAmount();
        float frac = clamp01(hp / max);
        int armorVal = e.getArmorValue();

        boolean showArmor = NameTagMod.showArmor;
        boolean showHands = NameTagMod.showHands;
        boolean showHpBar = NameTagMod.showHpBar;
        boolean showHpText = NameTagMod.showHpText;
        boolean showDurability = NameTagMod.showDurability;

        // ── Strings & widths ───────────────────────────────────────────
        String distText = String.format("%.1fm", dist);
        String hpText = showHpText
            ? String.format("%.0f/%.0f", hp, max) + (absorb > 0 ? "+" + (int) absorb : "")
            : "";
        String armorText = armorVal > 0 ? "⛨ " + armorVal : "";

        int nameW   = font.width(name);
        int distW   = font.width(distText);
        int hpTextW = font.width(hpText);
        int armorTextW = font.width(armorText);

        // ── Layout constants ───────────────────────────────────────────
        final int padX     = 6;
        final int padY     = 4;
        final int gap      = 6;     // gap between header columns
        final int rowGap   = 3;     // vertical gap between rows
        final int barH     = 4;
        final int iconSize = 16;
        final int iconGap  = 1;
        final int groupGap = 4;     // gap between armor group and hand group

        int iconCount = (showArmor ? ARMOR_SLOTS.length : 0) + (showHands ? HAND_SLOTS.length : 0);
        int armorW = showArmor ? ARMOR_SLOTS.length * iconSize + (ARMOR_SLOTS.length - 1) * iconGap : 0;
        int handsW = showHands ? HAND_SLOTS.length  * iconSize + (HAND_SLOTS.length  - 1) * iconGap : 0;
        int iconsW = armorW + handsW + (showArmor && showHands ? groupGap : 0);

        int headerW = nameW + gap + distW;
        int statW   = (hpText.isEmpty() ? 0 : hpTextW)
                    + ((!hpText.isEmpty() && !armorText.isEmpty()) ? gap : 0)
                    + (armorText.isEmpty() ? 0 : armorTextW);

        int contentW = Math.max(Math.max(headerW, statW), iconsW);
        if (contentW < 64) contentW = 64; // minimum width so short names don't shrink the panel

        // Compute total height accumulating rows we actually draw.
        int contentH = 9; // header line (name / distance)
        if (showHpBar || !hpText.isEmpty() || !armorText.isEmpty()) {
            contentH += rowGap;
            if (!hpText.isEmpty() || !armorText.isEmpty()) contentH += 9;
            if (showHpBar) contentH += (hpText.isEmpty() && armorText.isEmpty() ? 0 : 2) + barH;
        }
        if (iconCount > 0) {
            contentH += rowGap + iconSize;
        }

        int boxW = contentW + padX * 2;
        int boxH = contentH + padY * 2;
        int boxX = -boxW / 2;
        int boxY = -boxH;

        int accR = NameTagMod.useTheme ? cfg.accentR : NameTagMod.colorR;
        int accG = NameTagMod.useTheme ? cfg.accentG : NameTagMod.colorG;
        int accB = NameTagMod.useTheme ? cfg.accentB : NameTagMod.colorB;
        int accent = 0xFF000000
            | ((accR & 0xFF) << 16)
            | ((accG & 0xFF) << 8)
            |  (accB & 0xFF);
        int bgAlpha = clamp(cfg.bgAlpha, 60, 220);

        drawPanel(g, boxX, boxY, boxW, boxH, bgAlpha, accent);

        int cursorY = boxY + padY;

        // ── Header: name (left, shadowed) + distance (right, muted) ────
        g.drawString(font, name,     boxX + padX,                       cursorY, 0xFFFFFFFF, true);
        g.drawString(font, distText, boxX + boxW - padX - distW,        cursorY, 0xFFB0B0B0, false);
        cursorY += 9;

        // ── HP text + armor row ────────────────────────────────────────
        if (!hpText.isEmpty() || !armorText.isEmpty()) {
            cursorY += rowGap;
            int hpColor = hpColor(frac);
            if (!hpText.isEmpty()) {
                g.drawString(font, hpText, boxX + padX, cursorY, hpColor, false);
            }
            if (!armorText.isEmpty()) {
                g.drawString(font, armorText, boxX + boxW - padX - armorTextW, cursorY, 0xFF8FD3FF, false);
            }
            cursorY += 9;
        }

        // ── HP bar (gradient + highlight) ──────────────────────────────
        if (showHpBar) {
            // Small spacing only if a stat row preceded; otherwise tight after header.
            if (!hpText.isEmpty() || !armorText.isEmpty()) cursorY += 2;
            else cursorY += rowGap;
            int barX = boxX + padX;
            int barW = boxW - padX * 2;
            drawHpBar(g, barX, cursorY, barW, barH, frac);
            cursorY += barH;
        }

        // ── Equipment row ──────────────────────────────────────────────
        if (iconCount > 0) {
            cursorY += rowGap;
            int iconsX = -iconsW / 2;
            int x = iconsX;
            if (showArmor) {
                for (EquipmentSlot slot : ARMOR_SLOTS) {
                    drawSlot(g, font, e, slot, x, cursorY, showDurability);
                    x += iconSize + iconGap;
                }
            }
            if (showArmor && showHands) {
                // Subtle divider: a 1-px vertical line in the middle of the gap.
                int divX = x + groupGap / 2 - 1;
                g.fill(divX, cursorY + 3, divX + 1, cursorY + iconSize - 3, 0x60FFFFFF);
                x += groupGap;
            }
            if (showHands) {
                for (EquipmentSlot slot : HAND_SLOTS) {
                    drawSlot(g, font, e, slot, x, cursorY, showDurability);
                    x += iconSize + iconGap;
                }
            }
        }

        pose.popMatrix();
    }

    /**
     * Pseudo-rounded panel: bg fill with 4 corner pixels shaved off, then
     * an accent gradient stripe along the top edge. The stripe fades from
     * full accent at the centre to half alpha at the edges, which reads as
     * a soft highlight without needing alpha-blended diagonals.
     */
    private static void drawPanel(GuiGraphics g, int x, int y, int w, int h, int bgAlpha, int accent) {
        int bg = bgAlpha << 24;

        // Body bg minus the 4 corners (1-px corner shave for fake rounding).
        // Inset top/bottom edges by 1, full-width middle.
        g.fill(x + 1, y,         x + w - 1, y + 1,         bg);
        g.fill(x,     y + 1,     x + w,     y + h - 1,     bg);
        g.fill(x + 1, y + h - 1, x + w - 1, y + h,         bg);

        // Subtle bottom darkening to add depth.
        int bottomShade = (Math.min(220, bgAlpha + 40)) << 24;
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, bottomShade);

        // Accent stripe along the top — full alpha centre, fading to half at edges.
        int accentR = (accent >> 16) & 0xFF;
        int accentG = (accent >>  8) & 0xFF;
        int accentB =  accent        & 0xFF;
        int stripeW = w - 2;
        int stripeX = x + 1;
        int stripeY = y + 1;
        for (int i = 0; i < stripeW; i++) {
            // Triangular alpha falloff from centre. 0..1.
            float t = 1f - Math.abs((i - stripeW / 2f) / (stripeW / 2f));
            int a = (int)(255 * (0.45f + 0.55f * t));
            int col = (a << 24) | (accentR << 16) | (accentG << 8) | accentB;
            g.fill(stripeX + i, stripeY, stripeX + i + 1, stripeY + 1, col);
        }
    }

    /**
     * Gradient HP bar. Dark backplate, then a filled portion whose colour
     * smoothly interpolates red → orange → green based on the fraction.
     * A 1-px highlight stripe along the top adds shape.
     */
    private static void drawHpBar(GuiGraphics g, int x, int y, int w, int h, float frac) {
        g.fill(x, y, x + w, y + h, 0xC0202020);
        if (w <= 2 || h <= 1) return;

        int fillW = (int) ((w - 2) * frac);
        if (fillW <= 0) return;

        // Fill — gradient by progress: low frac stays red, mid orange, high green.
        int color = hpGradientColor(frac);
        g.fill(x + 1, y + 1, x + 1 + fillW, y + h - 1, color);

        // Top highlight stripe — same hue, brightened.
        int hi = brighten(color, 0.35f);
        g.fill(x + 1, y + 1, x + 1 + fillW, y + 2, hi);
    }

    private static void drawSlot(GuiGraphics g, Font font, LivingEntity e,
                                  EquipmentSlot slot, int x, int y, boolean showDurability) {
        // Subtle inset frame: outer 1-px lighter, inner darker.
        g.fill(x,     y,     x + 16, y + 16, 0x60000000);
        g.fill(x,     y,     x + 16, y + 1,  0x30FFFFFF);  // top highlight
        g.fill(x,     y + 15, x + 16, y + 16, 0x40000000); // bottom shadow
        ItemStack stack = e.getItemBySlot(slot);
        if (stack.isEmpty()) return;
        g.renderItem(stack, x, y);
        if (showDurability) g.renderItemDecorations(font, stack, x, y);
    }

    // ─── colour helpers ─────────────────────────────────────────────────

    private static int hpColor(float frac) {
        if (frac > 0.66f) return 0xFF66FF66;
        if (frac > 0.33f) return 0xFFFFB347;
        return 0xFFFF5555;
    }

    /** Smooth gradient: red(0) → orange(0.5) → green(1.0). */
    private static int hpGradientColor(float frac) {
        frac = clamp01(frac);
        int r, gr, b;
        if (frac < 0.5f) {
            float t = frac * 2f; // 0..1
            r  = (int) lerp(255, 255, t);  // 255 → 255
            gr = (int) lerp(85,  179, t);  // dark red → orange-green
            b  = (int) lerp(85,  67,  t);
        } else {
            float t = (frac - 0.5f) * 2f;
            r  = (int) lerp(255, 102, t);
            gr = (int) lerp(179, 255, t);
            b  = (int) lerp(67,  102, t);
        }
        return 0xFF000000 | (r << 16) | (gr << 8) | b;
    }

    private static int brighten(int argb, float amount) {
        int a = (argb >> 24) & 0xFF;
        int r = Math.min(255, (int) (((argb >> 16) & 0xFF) + 255 * amount));
        int g = Math.min(255, (int) (((argb >>  8) & 0xFF) + 255 * amount));
        int b = Math.min(255, (int) (( argb        & 0xFF) + 255 * amount));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static float clamp01(float v)                 { return v < 0f ? 0f : (v > 1f ? 1f : v); }
    private static int clamp(int v, int lo, int hi)       { return v < lo ? lo : (v > hi ? hi : v); }
}
