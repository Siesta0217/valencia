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

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    public static boolean shouldGlow(Entity e) {
        if (!enabled || e == null) return false;
        if (e == Minecraft.getInstance().player) return false;   // never glow self
        if (players  && e instanceof Player)     return true;
        if (hostile  && e instanceof Enemy)      return true;
        if (animals  && e instanceof Animal)     return true;
        if (items    && e instanceof ItemEntity) return true;
        return false;
    }
}
