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
    public static int red   = 255;
    public static int green = 255;
    public static int blue  = 255;

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    public static int colorFor(Entity e) {
        return 0xFF000000
            | (clamp(red) << 16)
            | (clamp(green) << 8)
            | clamp(blue);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
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
