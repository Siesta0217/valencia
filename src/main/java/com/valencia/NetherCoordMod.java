package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Top-left HUD that always shows the player's current XYZ plus the
 * dimension-converted coord (overworld<->nether ratio 1:8). Toggleable from
 * the ClickGUI under Visuals. Hidden while F3 debug is up or the GUI HUD is
 * disabled.
 */
public class NetherCoordMod {

    private static boolean enabled = true;

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    public static void render(GuiGraphics g) {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.options.hideGui) return;
        if (mc.getDebugOverlay() != null && mc.getDebugOverlay().showDebugScreen()) return;

        Font font = mc.font;
        int px = (int) Math.floor(mc.player.getX());
        int py = (int) Math.floor(mc.player.getY());
        int pz = (int) Math.floor(mc.player.getZ());

        boolean nether = mc.level.dimension().toString().contains("the_nether");

        String line1, line2;
        if (nether) {
            line1 = String.format("§6Nether    §f%d, %d, %d", px, py, pz);
            line2 = String.format("§7Overworld §f%d, %d, %d", px * 8, py, pz * 8);
        } else {
            line1 = String.format("§aOverworld §f%d, %d, %d", px, py, pz);
            line2 = String.format("§7Nether    §f%d, %d, %d", px / 8, py, pz / 8);
        }

        g.drawString(font, line1, 4, 4, 0xFFFFFFFF, true);
        g.drawString(font, line2, 4, 14, 0xFFAAAAAA, true);
    }
}
