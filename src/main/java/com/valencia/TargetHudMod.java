package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * Top-center HUD panel showing the current attack target.
 *
 * Target source: the {@code glowTarget} of whichever combat module is
 * currently active (KillAura > MaceAura > SpearAura). Each of those
 * modules already publishes its lock target for the GlowMixin to use, so
 * we just read the same field.
 *
 * Layout: name on left, distance on right, HP bar underneath, HP text
 * centred over the bar. Box uses the same accent colour / alpha that
 * NameTag and ClickGUI use, so it themes consistently.
 */
public final class TargetHudMod {

    private static boolean enabled = false;

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    private TargetHudMod() {}

    public static void render(GuiGraphics g) {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.hideGui) return;

        LivingEntity target = pickTarget();
        if (target == null || !target.isAlive()) return;

        Font font = mc.font;
        ModConfig cfg = ModConfig.get();

        String name = target.getName().getString();
        float hp = target.getHealth();
        float max = Math.max(1f, target.getMaxHealth());
        float absorb = target.getAbsorptionAmount();
        float frac = Math.max(0f, Math.min(1f, hp / max));
        double dist = mc.player.distanceTo(target);

        String distText = String.format("%.1fm", dist);
        String hpText = String.format("%.0f/%.0f", hp, max) + (absorb > 0 ? "+" + (int) absorb : "");

        // Panel size
        int padX = 6;
        int padY = 4;
        int nameW = font.width(name);
        int distW = font.width(distText);
        int headerW = nameW + 8 + distW;
        int contentW = Math.max(120, headerW);
        int contentH = 9 + 2 + 6 + 1 + 9;
        int boxW = contentW + padX * 2;
        int boxH = contentH + padY * 2;

        int viewW = mc.getWindow().getGuiScaledWidth();
        int boxX = (viewW - boxW) / 2;
        int boxY = 24;

        // Background + accent border
        int bgAlpha = clamp(cfg.bgAlpha, 60, 220);
        int bg = bgAlpha << 24;
        int accent = 0xFF000000
            | ((cfg.accentR & 0xFF) << 16)
            | ((cfg.accentG & 0xFF) << 8)
            |  (cfg.accentB & 0xFF);
        g.fill(boxX, boxY, boxX + boxW, boxY + boxH, bg);
        drawBorder(g, boxX, boxY, boxX + boxW, boxY + boxH, accent);

        // Header: name (left) + distance (right)
        int cursorY = boxY + padY;
        g.drawString(font, name,     boxX + padX,                       cursorY, 0xFFFFFFFF, false);
        g.drawString(font, distText, boxX + boxW - padX - distW,        cursorY, 0xFFAAAAAA, false);
        cursorY += 9 + 2;

        // HP bar
        int barX = boxX + padX;
        int barY = cursorY;
        int barW = boxW - padX * 2;
        int barH = 6;
        g.fill(barX, barY, barX + barW, barY + barH, 0xFF202020);
        int filled = (int)((barW - 2) * frac);
        if (filled > 0) {
            g.fill(barX + 1, barY + 1, barX + 1 + filled, barY + barH - 1, hpColor(frac));
        }
        cursorY += barH + 1;

        // HP text centred under bar
        int hpW = font.width(hpText);
        g.drawString(font, hpText, boxX + (boxW - hpW) / 2, cursorY, 0xFFFFFFFF, false);
    }

    private static LivingEntity pickTarget() {
        if (KillAuraMod.isEnabled() && KillAuraMod.glowTarget instanceof LivingEntity le && le.isAlive()) return le;
        if (MaceAuraMod.isEnabled() && MaceAuraMod.glowTarget instanceof LivingEntity le && le.isAlive()) return le;
        Entity sp = SpearAuraMod.glowTarget;
        if (SpearAuraMod.isEnabled() && sp instanceof LivingEntity le && le.isAlive()) return le;
        // Fallback: the living entity under the crosshair (so the HUD is useful
        // with no aura active), excluding ourselves.
        Minecraft mc = Minecraft.getInstance();
        Entity ch = mc.crosshairPickEntity;
        if (ch instanceof LivingEntity le && le.isAlive() && le != mc.player) return le;
        return null;
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
