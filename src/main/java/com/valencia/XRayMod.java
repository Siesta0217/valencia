package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Block;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class XRayMod {

    private static boolean enabled = false;
    private static double savedGamma = 0.5;

    private static final Set<String> XRAY_BLOCKS = Set.of(
            "coal_ore", "deepslate_coal_ore",
            "iron_ore", "deepslate_iron_ore",
            "copper_ore", "deepslate_copper_ore",
            "gold_ore", "deepslate_gold_ore",
            "redstone_ore", "deepslate_redstone_ore",
            "lapis_ore", "deepslate_lapis_ore",
            "diamond_ore", "deepslate_diamond_ore",
            "emerald_ore", "deepslate_emerald_ore",
            "nether_gold_ore", "nether_quartz_ore",
            "ancient_debris",
            "chest", "trapped_chest", "ender_chest", "barrel",
            "spawner", "trial_spawner",
            "tnt",
            "obsidian", "crying_obsidian",
            "bedrock",
            "nether_portal", "end_portal", "end_portal_frame", "end_gateway"
    );

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggle() {
        enabled = !enabled;
        Minecraft mc = Minecraft.getInstance();

        if (enabled) {
            savedGamma = mc.options.gamma().get();
            mc.options.gamma().set(16.0);
        } else {
            mc.options.gamma().set(savedGamma);
        }

        if (mc.levelRenderer != null) {
            mc.levelRenderer.allChanged();
        }
    }

    // Cache per Block — Block instances are static singletons in Minecraft,
    // so this hash map only grows to the total registered block count, then
    // becomes pure O(1) lookups instead of doing string.substring every frame.
    private static final ConcurrentHashMap<Block, Boolean> XRAY_CACHE = new ConcurrentHashMap<>();

    public static boolean isXRayBlock(Block block) {
        Boolean cached = XRAY_CACHE.get(block);
        if (cached != null) return cached;
        String id = block.getDescriptionId();
        String path = id.substring(id.lastIndexOf('.') + 1);
        boolean result = XRAY_BLOCKS.contains(path) || path.endsWith("shulker_box");
        XRAY_CACHE.put(block, result);
        return result;
    }
}
