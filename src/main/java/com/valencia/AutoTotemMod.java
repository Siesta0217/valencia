package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Keeps a Totem of Undying in the off-hand at all times.
 *
 * Works purely through the player's own inventory menu (no GUI open), so it
 * uses {@link InventoryMenu#SHIELD_SLOT} as the off-hand slot id and drives the
 * swap with {@code MultiPlayerGameMode.handleInventoryMouseClick} — the same
 * call vanilla uses for real clicks, so the server sees a normal pickup chain:
 *   1. pick up a totem from any inventory slot   (cursor = totem)
 *   2. drop it into the off-hand slot            (off-hand = totem, cursor = old off-hand item)
 *   3. drop the old off-hand item back           (cursor empty)
 *
 * Only runs while no container screen is open ({@code mc.screen == null}), so
 * {@code containerMenu} is guaranteed to be the player inventory menu.
 */
public final class AutoTotemMod {

    private static boolean enabled = false;

    /** Cooldown after a swap so we don't spam clicks before the prediction settles. */
    private static int delay = 0;

    private AutoTotemMod() {}

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    public static void tick() {
        if (!enabled) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (mc.screen != null) return;   // only when no container is open

        if (delay > 0) { delay--; return; }

        LocalPlayer p = mc.player;

        // Already holding a totem off-hand → nothing to do.
        if (p.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;

        AbstractContainerMenu menu = p.inventoryMenu;
        int totemSlot = -1;
        for (int i = 0; i < menu.slots.size(); i++) {
            if (i == InventoryMenu.SHIELD_SLOT) continue;
            ItemStack s = menu.slots.get(i).getItem();
            if (s.is(Items.TOTEM_OF_UNDYING)) { totemSlot = i; break; }
        }
        if (totemSlot == -1) return;   // no totem anywhere

        int id = menu.containerId;
        mc.gameMode.handleInventoryMouseClick(id, totemSlot, 0, ClickType.PICKUP, p);
        mc.gameMode.handleInventoryMouseClick(id, InventoryMenu.SHIELD_SLOT, 0, ClickType.PICKUP, p);
        // Return whatever was previously in the off-hand back to the totem's old slot.
        mc.gameMode.handleInventoryMouseClick(id, totemSlot, 0, ClickType.PICKUP, p);

        delay = 5;
    }
}
