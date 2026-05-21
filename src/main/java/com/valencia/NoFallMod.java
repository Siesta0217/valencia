package com.valencia;

import net.fabricmc.api.ClientModInitializer;

public class NoFallMod implements ClientModInitializer {

    private static boolean enabled = true;

    public static boolean isEnabled() { return enabled; }
    public static void toggleManual() { enabled = !enabled; }

    @Override
    public void onInitializeClient() {
        ModConfig cfg = ModConfig.get();

        // Restore module enabled states from last session
        enabled                   = cfg.nofallEnabled;
        if (cfg.xrayEnabled     != XRayMod.isEnabled())     XRayMod.toggle();
        if (cfg.maceAuraEnabled != MaceAuraMod.isEnabled()) MaceAuraMod.toggle();
        if (cfg.noSlowEnabled   != NoSlowMod.isEnabled())   NoSlowMod.toggle();
        if (cfg.bhopEnabled     != BHopMod.isEnabled())     BHopMod.toggle();
        if (cfg.stepEnabled     != StepMod.isEnabled())     StepMod.toggle();
        if (cfg.killAuraEnabled   != KillAuraMod.isEnabled())   KillAuraMod.toggle();
        if (cfg.autoSprintEnabled != AutoSprintMod.isEnabled()) AutoSprintMod.toggle();
        if (cfg.velocityEnabled   != VelocityMod.isEnabled())   VelocityMod.toggle();
        if (cfg.fastPlaceEnabled  != FastPlaceMod.isEnabled())  FastPlaceMod.toggle();
        if (cfg.critEnabled       != CritMod.isEnabled())       CritMod.toggle();

        // Restore tuning values
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
