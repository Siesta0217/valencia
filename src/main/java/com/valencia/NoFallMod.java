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
        if (cfg.velocityEnabled   != VelocityMod.isEnabled())   VelocityMod.toggle();
        if (cfg.fastPlaceEnabled  != FastPlaceMod.isEnabled())  FastPlaceMod.toggle();
        if (cfg.critEnabled       != CritMod.isEnabled())       CritMod.toggle();
        if (cfg.scaffoldEnabled   != ScaffoldMod.isEnabled())   ScaffoldMod.toggle();
        if (cfg.timerEnabled      != TimerMod.isEnabled())      TimerMod.toggle();
        if (cfg.spearAuraEnabled  != SpearAuraMod.isEnabled())  SpearAuraMod.toggle();
        if (cfg.noCrashEnabled    != NoCrashMod.isEnabled())    NoCrashMod.toggle();

        NoCrashMod.lookahead = cfg.noCrashLookAhead;
        NoCrashMod.maxSpeed  = cfg.noCrashMaxSpeed;

        SpearAuraMod.SCAN_RANGE         = cfg.spearScanRange;
        SpearAuraMod.MIN_REACH          = cfg.spearMinReach;
        SpearAuraMod.MAX_REACH          = cfg.spearMaxReach;
        SpearAuraMod.mode               = cfg.spearMode;
        SpearAuraMod.targetPlayers      = cfg.spearPlayers;
        SpearAuraMod.targetHostile      = cfg.spearHostile;
        SpearAuraMod.targetAnimals      = cfg.spearAnimals;
        SpearAuraMod.autoStepBack       = cfg.spearStepBack;
        SpearAuraMod.chargeReleaseTicks = cfg.spearChargeTicks;

        ScaffoldMod.tower      = cfg.scaffoldTower;
        ScaffoldMod.towerMove  = cfg.scaffoldTowerMove;
        ScaffoldMod.towerSpeed = cfg.scaffoldTowerSpeed;
        ScaffoldMod.autoSwitch = cfg.scaffoldAutoSwitch;
        ScaffoldMod.switchBack = cfg.scaffoldSwitchBack;
        ScaffoldMod.placeDelay = cfg.scaffoldPlaceDelay;
        ScaffoldMod.silentRot  = cfg.scaffoldSilentRot;
        ScaffoldMod.fakeHand   = cfg.scaffoldFakeHand;

        VelocityMod.horizontal = cfg.velocityHoriz;
        VelocityMod.vertical   = cfg.velocityVert;

        // Restore tuning values
        MaceAuraMod.RANGE          = cfg.maceDetectRange;
        MaceAuraMod.ATTACK_RANGE   = cfg.maceAttackRange;
        MaceAuraMod.targetHostile  = cfg.maceHostile;
        MaceAuraMod.targetAnimals  = cfg.maceAnimals;
        MaceAuraMod.targetPlayers  = cfg.macePlayers;
        StepMod.stepHeight         = cfg.stepHeight;
        KillAuraMod.RANGE        = cfg.killRange;
        KillAuraMod.ATTACK_RANGE = cfg.killAttackRange;
        KillAuraMod.targetHostile = cfg.killHostile;
        KillAuraMod.targetAnimals = cfg.killAnimals;
        KillAuraMod.targetPlayers = cfg.killPlayers;
        KillAuraMod.singleMode    = cfg.killSingle;
        KillAuraMod.attackDelay   = cfg.killAttackDelay;
        KillAuraMod.raycast       = cfg.killRaycast;
        KillAuraMod.skipInvisible = cfg.killSkipInvis;
        KillAuraMod.waitCooldown  = cfg.killWaitCool;
        KillAuraMod.smoothRot     = cfg.killSmoothRot;
        KillAuraMod.maxTurnDeg    = cfg.killMaxTurn;
        KillAuraMod.bodyLock      = cfg.killBodyLock;
        KillAuraMod.visibleBody   = cfg.killVisBody;
        BHopMod.speedMultiplier  = cfg.bhopSpeed;
        BHopMod.lowHop           = cfg.bhopLowHop;
        BHopMod.jumpHeight       = cfg.bhopJumpHeight;
        BHopMod.boost            = cfg.bhopBoost;
        BHopMod.kbBoost          = cfg.bhopKBBoost;
        TimerMod.speed           = cfg.timerSpeed;
        ElytraGotoMod.safeHpThreshold = cfg.elytraSafeHp;
        if (cfg.netherCoordEnabled != NetherCoordMod.isEnabled()) NetherCoordMod.toggle();
        if (cfg.autoFishEnabled    != AutoFishMod.isEnabled())    AutoFishMod.toggle();
        AutoFishMod.biteVy       = cfg.autoFishBiteVy;
        AutoFishMod.recastDelay  = cfg.autoFishRecast;
        if (cfg.espEnabled         != ESPMod.isEnabled())         ESPMod.toggle();
        ESPMod.players  = cfg.espPlayers;
        ESPMod.hostile  = cfg.espHostile;
        ESPMod.animals  = cfg.espAnimals;
        ESPMod.items    = cfg.espItems;
    }
}
