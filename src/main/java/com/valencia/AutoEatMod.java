package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * AutoEat — when hunger drops to the threshold, switch to the best food in
 * the hotbar, hold right-click until it's eaten, then restore the original
 * slot. Everything goes through the vanilla input system (Options.keyUse +
 * real slot switch), so the server sees a normal eating sequence.
 *
 * Food choice: highest nutrition item carrying a FOOD component, skipping a
 * small harmful-food blacklist (rotten flesh, spider eye, poisonous potato,
 * pufferfish, raw chicken). Golden apples still qualify via canAlwaysEat
 * when hunger is full but a fight calls for absorption — we don't handle
 * that; this module is hunger management only.
 */
public final class AutoEatMod {

    private static boolean enabled = false;

    /** Start eating at or below this food level (20 = full). */
    public static int threshold = 14;

    private static boolean eating   = false;
    private static int     prevSlot = -1;

    private AutoEatMod() {}

    public static boolean isEnabled() { return enabled; }

    public static void toggle() {
        enabled = !enabled;
        if (!enabled) release(Minecraft.getInstance());
    }

    public static void tick() {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.gameMode == null) { release(mc); return; }
        if (mc.screen != null) { release(mc); return; }
        if (p.isCreative() || !p.isAlive()) { release(mc); return; }

        int food = p.getFoodData().getFoodLevel();

        if (eating) {
            // Done when the bite finished and hunger is back above threshold.
            // If still hungry and food remains, keyUse stays held and vanilla
            // starts the next bite on its own.
            if (!p.isUsingItem() && (food > threshold || findFood(p.getInventory()) == -1)) {
                release(mc);
            }
            return;
        }

        if (food > threshold) return;

        int slot = findFood(p.getInventory());
        if (slot == -1) return;   // nothing edible in the hotbar

        Inventory inv = p.getInventory();
        if (inv.getSelectedSlot() != slot) {
            if (prevSlot == -1) prevSlot = inv.getSelectedSlot();
            AutoToolMod.select(mc, p, slot);   // client + server slot sync
        }
        mc.options.keyUse.setDown(true);
        eating = true;
    }

    /** Best hotbar food slot by nutrition, or -1. Skips harmful foods. */
    private static int findFood(Inventory inv) {
        int best = -1, bestNutrition = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack s = inv.getItem(i);
            if (s.isEmpty() || isHarmful(s)) continue;
            FoodProperties fp = s.get(DataComponents.FOOD);
            if (fp == null) continue;
            if (fp.nutrition() > bestNutrition) { bestNutrition = fp.nutrition(); best = i; }
        }
        return best;
    }

    private static boolean isHarmful(ItemStack s) {
        return s.is(Items.ROTTEN_FLESH) || s.is(Items.SPIDER_EYE)
            || s.is(Items.POISONOUS_POTATO) || s.is(Items.PUFFERFISH)
            || s.is(Items.CHICKEN);
    }

    /** Stop holding use and restore the original hotbar slot. */
    private static void release(Minecraft mc) {
        if (eating) {
            eating = false;
            if (mc.options != null) mc.options.keyUse.setDown(false);
        }
        if (prevSlot != -1 && mc.player != null) {
            AutoToolMod.select(mc, mc.player, prevSlot);
            prevSlot = -1;
        }
    }
}
