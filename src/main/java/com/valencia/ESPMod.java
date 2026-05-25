package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

/**
 * Through-wall glow ESP. Piggybacks on the existing {@link com.valencia.mixin.GlowMixin}
 * — when this module is on, {@link #shouldGlow(Entity)} forces the chosen
 * categories of entities to return {@code true} from
 * {@code Entity#isCurrentlyGlowing}, which makes the vanilla outline shader
 * draw them through walls.
 */
public class ESPMod {

    private static boolean enabled = false;

    public static boolean players  = true;
    public static boolean hostile  = false;
    public static boolean animals  = false;
    public static boolean items    = false;

    public static boolean showBox = false;
    public static boolean showName = true;
    public static boolean showHealth = true;
    public static boolean cornerBox = false;

    public static int boxColor = 0xFF80FF40;
    public static int nameColor = 0xFFFFFFFF;
    public static int healthColorHigh = 0xFF00FF00;
    public static int healthColorLow  = 0xFFFF0000;

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    /** Whether ESP should target this entity (used by both glow + box). */
    public static boolean targets(Entity e) {
        if (!enabled || e == null) return false;
        if (e == Minecraft.getInstance().player) return false;
        if (players  && e instanceof Player)     return true;
        if (hostile  && e instanceof Enemy)      return true;
        if (animals  && e instanceof Animal)     return true;
        if (items    && e instanceof ItemEntity) return true;
        return false;
    }

    public static boolean shouldGlow(Entity e) {
        return targets(e);
    }
}
