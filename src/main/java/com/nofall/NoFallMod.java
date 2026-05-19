package com.nofall;

import net.fabricmc.api.ClientModInitializer;

public class NoFallMod implements ClientModInitializer {

    private static boolean enabled = true;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void toggleManual() {
        enabled = !enabled;
    }

    @Override
    public void onInitializeClient() {
        ModConfig.get(); // load config on startup
    }
}
