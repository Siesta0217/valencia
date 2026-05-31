package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Top-center HUD panel showing the current attack target.
 *
 * Target source (shared by every style): the {@code glowTarget} of whichever
 * combat module is active (KillAura > MaceAura > SpearAura), falling back to
 * the entity under the crosshair and then the last mob that hit us.
 *
 * Four switchable looks via {@code cfg.targetHudStyle} — style 0 (Classic) is
 * the original box, kept byte-for-byte so existing users see no change:
 *   0 Classic  — name + distance header, accent box, solid HP bar, HP text
 *   1 Compact  — one slim line (name / HP / distance) with a 2px HP strip
 *   2 Gradient — bigger panel, shaved corners, red→green gradient HP bar
 *   3 Ring     — circular HP ring with the HP% inside, name/HP/distance beside
 */
public final class TargetHudMod {

    private static boolean enabled = false;

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    private TargetHudMod() {}

    /** Shared, pre-computed target metrics handed to whichever style renders. */
    private record Info(String name, float hp, float max, float absorb, float frac, double dist) {}

    public static void render(GuiGraphics g) {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.options.hideGui) return;

        LivingEntity target = pickTarget();
        if (target == null || !target.isAlive()) return;

        Font font = mc.font;
        ModConfig cfg = ModConfig.get();
        int viewW = mc.getWindow().getGuiScaledWidth();

        float hp = target.getHealth();
        float max = Math.max(1f, target.getMaxHealth());
        Info in = new Info(
            target.getName().getString(),
            hp, max, target.getAbsorptionAmount(),
            Math.max(0f, Math.min(1f, hp / max)),
            mc.player.distanceTo(target));

        switch (cfg.targetHudStyle) {
            case 1 -> renderCompact(g, font, cfg, in, viewW);
            case 2 -> renderGradient(g, font, cfg, in, viewW);
            case 3 -> renderRing(g, font, cfg, in, viewW);
            default -> renderClassic(g, font, cfg, in, viewW);
        }
    }

    // ── Style 0: Classic (original) ──────────────────────────────────────────
    private static void renderClassic(GuiGraphics g, Font font, ModConfig cfg, Info in, int viewW) {
        String distText = String.format("%.1fm", in.dist());
        String hpText = fmtHp(in);

        int padX = 6, padY = 4;
        int nameW = font.width(in.name());
        int distW = font.width(distText);
        int headerW = nameW + 8 + distW;
        int contentW = Math.max(120, headerW);
        int contentH = 9 + 2 + 6 + 1 + 9;
        int boxW = contentW + padX * 2;
        int boxH = contentH + padY * 2;

        int boxX = (viewW - boxW) / 2;
        int boxY = 24;

        int bg = clamp(cfg.bgAlpha, 60, 220) << 24;
        int accent = accent(cfg);
        g.fill(boxX, boxY, boxX + boxW, boxY + boxH, bg);
        drawBorder(g, boxX, boxY, boxX + boxW, boxY + boxH, accent);

        int cursorY = boxY + padY;
        g.drawString(font, in.name(), boxX + padX,                cursorY, 0xFFFFFFFF, false);
        g.drawString(font, distText,  boxX + boxW - padX - distW,  cursorY, 0xFFAAAAAA, false);
        cursorY += 9 + 2;

        int barX = boxX + padX, barY = cursorY, barW = boxW - padX * 2, barH = 6;
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF202020);
        int filled = (int)((barW - 2) * in.frac());
        if (filled > 0) g.fill(barX + 1, barY + 1, barX + 1 + filled, barY + barH - 1, hpColor(in.frac()));
        cursorY += barH + 1;

        int hpW = font.width(hpText);
        g.drawString(font, hpText, boxX + (boxW - hpW) / 2, cursorY, 0xFFFFFFFF, false);
    }

    // ── Style 1: Compact ─────────────────────────────────────────────────────
    private static void renderCompact(GuiGraphics g, Font font, ModConfig cfg, Info in, int viewW) {
        String hpText = fmtHp(in);
        String distText = String.format("%.1fm", in.dist());

        int padX = 5, padY = 3, gap = 6, barH = 2;
        int contentW = font.width(in.name()) + gap + font.width(hpText) + gap + font.width(distText);
        int boxW = contentW + padX * 2;
        int boxH = 9 + padY * 2 + barH;

        int boxX = (viewW - boxW) / 2;
        int boxY = 24;

        int accent = accent(cfg);
        g.fill(boxX, boxY, boxX + boxW, boxY + boxH, clamp(cfg.bgAlpha, 60, 220) << 24);
        drawBorder(g, boxX, boxY, boxX + boxW, boxY + boxH, accent);

        int tx = boxX + padX, ty = boxY + padY;
        g.drawString(font, in.name(), tx, ty, 0xFFFFFFFF, false);     tx += font.width(in.name()) + gap;
        g.drawString(font, hpText,    tx, ty, hpColor(in.frac()), false); tx += font.width(hpText) + gap;
        g.drawString(font, distText,  tx, ty, 0xFFAAAAAA, false);

        int barX = boxX + 1, barY = boxY + boxH - barH - 1, barW = boxW - 2;
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF202020);
        int filled = (int)(barW * in.frac());
        if (filled > 0) g.fill(barX, barY, barX + filled, barY + barH, hpColor(in.frac()));
    }

    // ── Style 2: Gradient ────────────────────────────────────────────────────
    private static void renderGradient(GuiGraphics g, Font font, ModConfig cfg, Info in, int viewW) {
        String distText = String.format("%.1fm", in.dist());
        String hpText = fmtHp(in);

        int padX = 7, padY = 5, barH = 8;
        int nameW = font.width(in.name());
        int distW = font.width(distText);
        int contentW = Math.max(140, nameW + 10 + distW);
        int contentH = 9 + 3 + barH + 2 + 9;
        int boxW = contentW + padX * 2;
        int boxH = contentH + padY * 2;

        int boxX = (viewW - boxW) / 2;
        int boxY = 24;

        int accent = accent(cfg);
        int bg = clamp(cfg.bgAlpha, 60, 220) << 24;
        // shaved corners (fake rounding) — inset the bg fill 1px per corner
        g.fill(boxX + 1, boxY,     boxX + boxW - 1, boxY + boxH,     bg);
        g.fill(boxX,     boxY + 1, boxX + boxW,     boxY + boxH - 1, bg);
        // accent stripe along the top edge
        g.fill(boxX + 1, boxY, boxX + boxW - 1, boxY + 2, accent);

        int cy = boxY + padY + 1;
        g.drawString(font, in.name(), boxX + padX,                cy, 0xFFFFFFFF, true);
        g.drawString(font, distText,  boxX + boxW - padX - distW,  cy, 0xFFB0B0B0, false);
        cy += 9 + 3;

        int barX = boxX + padX, barW = boxW - padX * 2, barY = cy;
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF1A1A1A);
        drawHpGradient(g, barX + 1, barY + 1, barW - 2, barH - 2, in.frac());
        cy += barH + 2;

        int hpW = font.width(hpText);
        g.drawString(font, hpText, boxX + (boxW - hpW) / 2, cy, 0xFFFFFFFF, true);
    }

    // ── Style 3: Ring ────────────────────────────────────────────────────────
    private static void renderRing(GuiGraphics g, Font font, ModConfig cfg, Info in, int viewW) {
        String hpText = fmtHp(in);
        String distText = String.format("%.1fm", in.dist());

        int pad = 6, ringR = 16, ringBox = ringR * 2;
        int textW = Math.max(font.width(in.name()), Math.max(font.width(hpText), font.width(distText)));
        int contentH = Math.max(ringBox, 9 * 3 + 4);
        int boxW = pad + ringBox + 8 + textW + pad;
        int boxH = contentH + pad * 2;

        int boxX = (viewW - boxW) / 2;
        int boxY = 24;

        int accent = accent(cfg);
        g.fill(boxX, boxY, boxX + boxW, boxY + boxH, clamp(cfg.bgAlpha, 60, 220) << 24);
        drawBorder(g, boxX, boxY, boxX + boxW, boxY + boxH, accent);

        // circular HP ring
        int rcx = boxX + pad + ringR, rcy = boxY + boxH / 2;
        drawRing(g, rcx, rcy, ringR, ringR - 4, in.frac(), hpColor(in.frac()), 0xFF303030);
        String pct = (int)(in.frac() * 100) + "%";
        g.drawString(font, pct, rcx - font.width(pct) / 2, rcy - font.lineHeight / 2, 0xFFFFFFFF, false);

        // text column to the right of the ring
        int tx = boxX + pad + ringBox + 8;
        int ty = boxY + (boxH - (9 * 3 + 4)) / 2;
        g.drawString(font, in.name(), tx, ty, 0xFFFFFFFF, true);          ty += 11;
        g.drawString(font, hpText,    tx, ty, hpColor(in.frac()), false); ty += 11;
        g.drawString(font, distText,  tx, ty, 0xFFAAAAAA, false);
    }

    /** Filled annulus; the fg arc sweeps clockwise from the top up to {@code frac}. */
    private static void drawRing(GuiGraphics g, int cx, int cy, int rOuter, int rInner, float frac, int fg, int bg) {
        float f = Math.max(0f, Math.min(1f, frac));
        int o2 = rOuter * rOuter, i2 = rInner * rInner;
        for (int dy = -rOuter; dy <= rOuter; dy++) {
            for (int dx = -rOuter; dx <= rOuter; dx++) {
                int d2 = dx * dx + dy * dy;
                if (d2 > o2 || d2 < i2) continue;
                double ang = Math.atan2(dx, -dy);          // 0 at top, +clockwise
                if (ang < 0) ang += Math.PI * 2;
                g.fill(cx + dx, cy + dy, cx + dx + 1, cy + dy + 1, (ang / (Math.PI * 2)) <= f ? fg : bg);
            }
        }
    }

    // ── Target picking (shared) ──────────────────────────────────────────────
    private static LivingEntity pickTarget() {
        if (KillAuraMod.isEnabled() && KillAuraMod.glowTarget instanceof LivingEntity le && le.isAlive()) return le;
        if (MaceAuraMod.isEnabled() && MaceAuraMod.glowTarget instanceof LivingEntity le && le.isAlive()) return le;
        Entity sp = SpearAuraMod.glowTarget;
        if (SpearAuraMod.isEnabled() && sp instanceof LivingEntity le && le.isAlive()) return le;
        // Fallback 1: the living entity under the crosshair (so the HUD is
        // useful with no aura active), excluding ourselves.
        Minecraft mc = Minecraft.getInstance();
        Entity ch = mc.crosshairPickEntity;
        if (ch instanceof LivingEntity le && le.isAlive() && le != mc.player) return le;
        // Fallback 2: the last mob that hit us — keeps the panel up through a
        // fight even when we look away from the attacker.
        if (mc.player != null) {
            LivingEntity last = mc.player.getLastHurtByMob();
            if (last != null && last.isAlive() && last != mc.player) return last;
        }
        return null;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private static String fmtHp(Info in) {
        return String.format("%.0f/%.0f", in.hp(), in.max()) + (in.absorb() > 0 ? "+" + (int) in.absorb() : "");
    }

    private static int accent(ModConfig cfg) {
        return 0xFF000000
            | ((cfg.accentR & 0xFF) << 16)
            | ((cfg.accentG & 0xFF) << 8)
            |  (cfg.accentB & 0xFF);
    }

    /** Fill columns left→right up to {@code frac}, colour ramping red→green across the bar. */
    private static void drawHpGradient(GuiGraphics g, int x, int y, int w, int h, float frac) {
        if (w <= 0) return;
        int filled = (int)(w * Math.max(0f, Math.min(1f, frac)));
        for (int i = 0; i < filled; i++) {
            float t = w <= 1 ? frac : (float) i / (w - 1);
            g.fill(x + i, y, x + i + 1, y + h, gradColor(t));
        }
    }

    private static int gradColor(float t) {
        return t < 0.5f ? lerp(0xFF5555, 0xFFAA00, t * 2f)
                        : lerp(0xFFAA00, 0x55FF55, (t - 0.5f) * 2f);
    }

    private static int lerp(int rgb1, int rgb2, float t) {
        int r = (int)(((rgb1 >> 16) & 0xFF) + (((rgb2 >> 16) & 0xFF) - ((rgb1 >> 16) & 0xFF)) * t);
        int gg = (int)(((rgb1 >> 8) & 0xFF) + (((rgb2 >> 8) & 0xFF) - ((rgb1 >> 8) & 0xFF)) * t);
        int b = (int)((rgb1 & 0xFF) + ((rgb2 & 0xFF) - (rgb1 & 0xFF)) * t);
        return 0xFF000000 | (r << 16) | (gg << 8) | b;
    }

    private static void drawBorder(GuiGraphics g, int x1, int y1, int x2, int y2, int color) {
        g.fill(x1,     y1,     x2,     y1 + 1, color);
        g.fill(x1,     y2 - 1, x2,     y2,     color);
        g.fill(x1,     y1,     x1 + 1, y2,     color);
        g.fill(x2 - 1, y1,     x2,     y2,     color);
    }

    private static int hpColor(float frac) {
        if (frac > 0.66f) return 0xFF55FF55;
        if (frac > 0.33f) return 0xFFFFAA00;
        return 0xFFFF5555;
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
