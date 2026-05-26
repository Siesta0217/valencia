package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

public class ESPMod {

    public static final int STYLE_CORNERS = 0;
    public static final int STYLE_OUTLINE = 1;
    public static final int STYLE_FILLED  = 2;
    public static final int STYLE_HITBOX  = 3;

    public static final int MODE_SINGLE   = 0;
    public static final int MODE_CATEGORY = 1;
    public static final int MODE_CHROMA   = 2;

    private static boolean enabled = false;

    // Targets
    public static boolean players = true;
    public static boolean hostile = false;
    public static boolean animals = false;
    public static boolean items   = false;

    // Box
    public static int   style         = STYLE_HITBOX;
    public static int   lineThickness = 1;
    public static float maxDistance   = 80.0f;

    // Labels
    public static boolean showName     = true;
    public static boolean showHp       = true;
    public static boolean showDistance = true;
    public static boolean showTracer   = false;

    // Color
    public static int   colorMode   = MODE_SINGLE;
    public static float hue         = 100f;   // 0..360, green-ish default
    public static float chromaSpeed = 0.5f;   // hue cycles per second

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    /**
     * ARGB color for an entity's ESP overlay.
     * - SINGLE: one hue for everything.
     * - CATEGORY: hue per target type (red/orange/green/yellow).
     * - CHROMA: hue cycles with wall-clock time.
     */
    public static int colorFor(Entity e) {
        float h;
        switch (colorMode) {
            case MODE_CHROMA -> h = (float)((System.currentTimeMillis() / 1000.0 * chromaSpeed) % 1.0);
            case MODE_CATEGORY -> {
                if      (e instanceof Player)     h = 0f / 360f;    // red
                else if (e instanceof Enemy)      h = 30f / 360f;   // orange
                else if (e instanceof Animal)     h = 120f / 360f;  // green
                else if (e instanceof ItemEntity) h = 60f / 360f;   // yellow
                else                              h = hue / 360f;
            }
            default -> h = hue / 360f;
        }
        return 0xFF000000 | (java.awt.Color.HSBtoRGB(h, 1f, 1f) & 0xFFFFFF);
    }

    public static boolean targets(Entity e) {
        if (!enabled || e == null) return false;
        if (e == Minecraft.getInstance().player) return false;
        if (players && e instanceof Player)     return true;
        if (hostile && e instanceof Enemy)      return true;
        if (animals && e instanceof Animal)     return true;
        if (items   && e instanceof ItemEntity) return true;
        return false;
    }

    public static boolean shouldGlow(Entity e) {
        return targets(e);
    }
}
