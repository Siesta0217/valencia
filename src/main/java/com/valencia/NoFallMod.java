package com.valencia;

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
        ModConfig cfg = ModConfig.get();
        // Sync saved settings into mod static fields
        MaceAuraMod.RANGE        = cfg.maceDetectRange;
        MaceAuraMod.ATTACK_RANGE = cfg.maceAttackRange;
        KillAuraMod.RANGE        = cfg.killRange;
        KillAuraMod.ATTACK_RANGE = cfg.killAttackRange;
        KillAuraMod.targetHostile = cfg.killHostile;
        KillAuraMod.targetAnimals = cfg.killAnimals;
        KillAuraMod.targetPlayers = cfg.killPlayers;
        KillAuraMod.singleMode    = cfg.killSingle;
        KillAuraMod.attackDelay   = cfg.killAttackDelay;
        BHopMod.speedMultiplier  = cfg.bhopSpeed;
    }
}
