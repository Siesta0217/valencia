package com.valencia;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3x2fStack;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class NameTagRenderer {

    private static final EquipmentSlot[] SLOTS = {
        EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
        EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND
    };

    public static void render(GuiGraphics g) {
        if (!NameTagMod.isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.position();
        Quaternionf invRot = new Quaternionf(camera.rotation()).conjugate();

        float partialTick = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        org.joml.Matrix4f proj = mc.gameRenderer.getProjectionMatrix(partialTick);
        double m00 = proj.m00();
        double m11 = proj.m11();
        Window win = mc.getWindow();
        int viewW = win.getGuiScaledWidth();
        int viewH = win.getGuiScaledHeight();

        Font font = mc.font;

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!NameTagMod.targets(e)) continue;
            LivingEntity le = (LivingEntity) e;

            double tx = e.getX();
            double ty = e.getBoundingBox().maxY + 0.45;
            double tz = e.getZ();

            int[] sp = projectPoint(tx, ty, tz, camPos, invRot, m00, m11, viewW, viewH);
            if (sp == null) continue;

            double dx = e.getX() - camPos.x;
            double dy = e.getEyeY() - camPos.y;
            double dz = e.getZ() - camPos.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);

            float s = NameTagMod.scale * (float) Math.max(0.6, Math.min(1.4, 8.0 / Math.max(1.0, dist)));
            drawTag(g, font, le, sp[0], sp[1], s);
        }
    }

    private static void drawTag(GuiGraphics g, Font font, LivingEntity e, int anchorX, int anchorY, float scale) {
        Matrix3x2fStack stack = g.pose();
        stack.pushMatrix();
        stack.translate(anchorX, anchorY);
        stack.scale(scale, scale);

        String name = e.getName().getString();
        float hp  = e.getHealth();
        float max = e.getMaxHealth();
        float abs = e.getAbsorptionAmount();

        boolean showHands = NameTagMod.showHands;
        boolean showArmor = NameTagMod.showArmor;
        int iconCount = (showArmor ? 4 : 0) + (showHands ? 2 : 0);

        int padX = 3;
        int padY = 2;
        int iconSize = 16;
        int iconSpacing = 1;
        int rowGap = 2;

        int nameW = font.width(name);
        String hpText = String.format("%.0f/%.0f", hp, max) + (abs > 0 ? "+" + (int) abs : "");
        int hpTextW = NameTagMod.showHpText ? font.width(hpText) + 4 : 0;
        int armorVal = e.getArmorValue();
        String armorText = armorVal > 0 ? " §7[§b" + armorVal + "§7]" : "";
        int armorTextW = font.width(armorText);

        int headerW = nameW + hpTextW + armorTextW;
        int iconsW  = iconCount > 0 ? iconCount * iconSize + (iconCount - 1) * iconSpacing : 0;
        int contentW = Math.max(headerW, iconsW);
        int contentH = 9
            + (NameTagMod.showHpBar ? 3 + rowGap : rowGap)
            + (iconCount > 0 ? iconSize : 0);

        int boxW = contentW + padX * 2;
        int boxH = contentH + padY * 2;
        int boxX = -boxW / 2;
        int boxY = -boxH;

        int bgA = Math.max(60, Math.min(220, ModConfig.get().bgAlpha));
        int bg  = (bgA << 24);
        g.fill(boxX, boxY, boxX + boxW, boxY + boxH, bg);
        int border = 0x80000000 | ((ModConfig.get().accentR & 0xFF) << 16)
                                | ((ModConfig.get().accentG & 0xFF) << 8)
                                |  (ModConfig.get().accentB & 0xFF);
        g.fill(boxX,            boxY,            boxX + boxW, boxY + 1,        border);
        g.fill(boxX,            boxY + boxH - 1, boxX + boxW, boxY + boxH,     border);
        g.fill(boxX,            boxY,            boxX + 1,    boxY + boxH,     border);
        g.fill(boxX + boxW - 1, boxY,            boxX + boxW, boxY + boxH,     border);

        int cursorY = boxY + padY;
        int headerX = -headerW / 2;

        g.drawString(font, name, headerX, cursorY, 0xFFFFFFFF, false);
        if (NameTagMod.showHpText) {
            int hpColor = hpColor(hp / Math.max(1.0f, max));
            g.drawString(font, hpText, headerX + nameW + 4, cursorY, hpColor, false);
        }
        if (!armorText.isEmpty()) {
            g.drawString(font, armorText, headerX + nameW + hpTextW, cursorY, 0xFFFFFFFF, false);
        }
        cursorY += 9;

        if (NameTagMod.showHpBar) {
            int barW = Math.max(40, contentW);
            int barX = -barW / 2;
            float frac = Math.max(0f, Math.min(1f, hp / Math.max(1f, max)));
            g.fill(barX,     cursorY, barX + barW,                       cursorY + 3, 0xFF202020);
            g.fill(barX + 1, cursorY + 1, barX + 1 + (int)((barW - 2) * frac), cursorY + 2, hpColor(frac));
            cursorY += 3 + rowGap;
        } else {
            cursorY += rowGap;
        }

        if (iconCount > 0) {
            int iconsX = -iconsW / 2;
            int slotIdx = 0;
            if (showArmor) {
                for (int i = 0; i < 4; i++) {
                    drawItem(g, font, e, SLOTS[i], iconsX + slotIdx * (iconSize + iconSpacing), cursorY);
                    slotIdx++;
                }
            }
            if (showHands) {
                for (int i = 4; i < 6; i++) {
                    drawItem(g, font, e, SLOTS[i], iconsX + slotIdx * (iconSize + iconSpacing), cursorY);
                    slotIdx++;
                }
            }
        }

        stack.popMatrix();
    }

    private static void drawItem(GuiGraphics g, Font font, LivingEntity e, EquipmentSlot slot, int x, int y) {
        ItemStack stack = e.getItemBySlot(slot);
        g.fill(x, y, x + 16, y + 16, 0x60000000);
        if (stack.isEmpty()) return;
        g.renderItem(stack, x, y);
        if (NameTagMod.showDurability) {
            g.renderItemDecorations(font, stack, x, y);
        }
    }

    private static int hpColor(float frac) {
        if (frac > 0.66f) return 0xFF55FF55;
        if (frac > 0.33f) return 0xFFFFAA00;
        return 0xFFFF5555;
    }

    private static int[] projectPoint(
        double wx, double wy, double wz,
        Vec3 camPos, Quaternionf invRot,
        double m00, double m11, int viewW, int viewH
    ) {
        Vector3f rel = new Vector3f(
            (float)(wx - camPos.x),
            (float)(wy - camPos.y),
            (float)(wz - camPos.z)
        );
        rel.rotate(invRot);
        if (rel.z >= -0.05f) return null;
        double invNegZ = 1.0 / -rel.z;
        int sx = (int)(viewW / 2.0 + ((double) rel.x * m00 * invNegZ) * viewW / 2.0);
        int sy = (int)(viewH / 2.0 - ((double) rel.y * m11 * invNegZ) * viewH / 2.0);
        return new int[]{ sx, sy };
    }
}
