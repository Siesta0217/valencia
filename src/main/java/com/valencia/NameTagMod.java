package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;

public class NameTagMod {

    private static boolean enabled = false;

    public static boolean players    = true;
    public static boolean hostile    = false;
    public static boolean animals    = false;

    public static boolean showArmor      = true;
    public static boolean showHands      = true;
    public static boolean showDurability = true;
    public static boolean showHpBar      = true;
    public static boolean showHpText     = true;

    public static float scale = 1.0f;

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    public static boolean targets(Entity e) {
        if (!enabled || !(e instanceof LivingEntity)) return false;
        if (e == Minecraft.getInstance().player) return false;
        if (!e.isAlive()) return false;
        if (players && e instanceof Player)  return true;
        if (hostile && e instanceof Enemy)   return true;
        if (animals && e instanceof Animal)  return true;
        return false;
    }
}
