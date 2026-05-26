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

    public static boolean players = true;
    public static boolean hostile = false;
    public static boolean animals = false;
    public static boolean items   = false;

    public static int boxR = 128, boxG = 255, boxB = 64;
    public static boolean chroma      = false;
    public static float   chromaSpeed = 0.5f;  // hue cycles per second

    public static int     style         = STYLE_HITBOX;
    public static boolean showName      = true;
    public static boolean showHp        = true;
    public static boolean showDistance  = true;
    public static boolean showTracer    = false;
    public static float   maxDistance   = 80.0f;
    public static int     lineThickness = 1;

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    public static int boxColor() {
        if (chroma) {
            float hue = (float)((System.currentTimeMillis() / 1000.0 * chromaSpeed) % 1.0);
            return 0xFF000000 | (java.awt.Color.HSBtoRGB(hue, 1f, 1f) & 0xFFFFFF);
        }
        return 0xFF000000 | (boxR << 16) | (boxG << 8) | boxB;
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
