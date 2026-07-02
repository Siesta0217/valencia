package com.valencia;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds the ClickGUI module/panel data (the module↔setting wiring). Extracted
 * verbatim from ClickGuiScreen.buildPanels(); {@link ClickGuiScreen} calls
 * {@link #build()} once in its constructor. Each setting closure still reads/writes
 * the same public {@code ModConfig} fields and {@code *Mod} statics as before.
 */
final class ModuleRegistry {

    private final List<Panel> panels = new ArrayList<>();

    /** Build and return the panel list (one Panel per category, populated with modules). */
    static List<Panel> build() {
        return new ModuleRegistry().buildInternal();
    }

    private List<Panel> buildInternal() {
        for (Cat c : Cat.values()) panels.add(new Panel(c));
        ModConfig cfg = ModConfig.get();

        // ── Combat ──────────────────────────────────────────────────────────
        add(Cat.COMBAT, new ModEntry("KillAura", KillAuraMod::isEnabled, KillAuraMod::toggle, true, List.of(
            new SliderS("Range",     () -> cfg.killRange,       v -> { cfg.killRange = (float)v;       KillAuraMod.RANGE = (float)v;          cfg.save(); }, 1, 10),
            new SliderS("Atk Range", () -> cfg.killAttackRange, v -> { cfg.killAttackRange = (float)v; KillAuraMod.ATTACK_RANGE = (float)v;   cfg.save(); }, 1, 8),
            new SliderS("Atk Delay", () -> cfg.killAttackDelay, v -> { cfg.killAttackDelay = (int)v;   KillAuraMod.attackDelay = (int)v;       cfg.save(); }, 2, 20),
            new BoolS("Single",  () -> cfg.killSingle,    () -> { cfg.killSingle = !cfg.killSingle;       KillAuraMod.singleMode = cfg.killSingle;       cfg.save(); }),
            new BoolS("Hostile", () -> cfg.killHostile,   () -> { cfg.killHostile = !cfg.killHostile;     KillAuraMod.targetHostile = cfg.killHostile;   cfg.save(); }),
            new BoolS("Animals", () -> cfg.killAnimals,   () -> { cfg.killAnimals = !cfg.killAnimals;     KillAuraMod.targetAnimals = cfg.killAnimals;   cfg.save(); }),
            new BoolS("Players", () -> cfg.killPlayers,   () -> { cfg.killPlayers = !cfg.killPlayers;     KillAuraMod.targetPlayers = cfg.killPlayers;   cfg.save(); }),
            new BoolS("Raycast", () -> cfg.killRaycast,   () -> { cfg.killRaycast = !cfg.killRaycast;     KillAuraMod.raycast = cfg.killRaycast;         cfg.save(); }),
            new BoolS("Skip Inv",() -> cfg.killSkipInvis, () -> { cfg.killSkipInvis = !cfg.killSkipInvis; KillAuraMod.skipInvisible = cfg.killSkipInvis; cfg.save(); }),
            new BoolS("Wait CD", () -> cfg.killWaitCool,  () -> { cfg.killWaitCool = !cfg.killWaitCool;   KillAuraMod.waitCooldown = cfg.killWaitCool;   cfg.save(); }),
            new BoolS("Smooth",  () -> cfg.killSmoothRot, () -> { cfg.killSmoothRot = !cfg.killSmoothRot; KillAuraMod.smoothRot = cfg.killSmoothRot;    cfg.save(); }),
            new SliderS("Max Turn", () -> cfg.killMaxTurn, v -> { cfg.killMaxTurn = (int)v; KillAuraMod.maxTurnDeg = (int)v; cfg.save(); }, 10, 180),
            new BoolS("Body Lock",() -> cfg.killBodyLock, () -> { cfg.killBodyLock = !cfg.killBodyLock;   KillAuraMod.bodyLock = cfg.killBodyLock;       cfg.save(); }),
            new BoolS("Vis Body",() -> cfg.killVisBody,   () -> { cfg.killVisBody = !cfg.killVisBody;     KillAuraMod.visibleBody = cfg.killVisBody;     cfg.save(); }),
            new BoolS("GCD",     () -> cfg.killGcd,       () -> { cfg.killGcd = !cfg.killGcd;             KillAuraMod.gcdSnap = cfg.killGcd;             cfg.save(); }),
            new SliderS("CPS Jit", () -> cfg.killCpsJitter, v -> { cfg.killCpsJitter = (int)v; KillAuraMod.cpsJitter = (int)v; cfg.save(); }, 0, 5),
            new KeyS("Key", () -> cfg.killAuraKey, v -> { cfg.killAuraKey = v; cfg.save(); })
        )));

        add(Cat.COMBAT, new ModEntry("MaceAura", MaceAuraMod::isEnabled, MaceAuraMod::toggle, true, List.of(
            new SliderS("Det Range", () -> cfg.maceDetectRange, v -> { cfg.maceDetectRange = (float)v; MaceAuraMod.RANGE = (float)v;        cfg.save(); }, 1, 12),
            new SliderS("Atk Range", () -> cfg.maceAttackRange, v -> { cfg.maceAttackRange = (float)v; MaceAuraMod.ATTACK_RANGE = (float)v; cfg.save(); }, 1, 8),
            new BoolS("Hostile", () -> cfg.maceHostile, () -> { cfg.maceHostile = !cfg.maceHostile; MaceAuraMod.targetHostile = cfg.maceHostile; cfg.save(); }),
            new BoolS("Animals", () -> cfg.maceAnimals, () -> { cfg.maceAnimals = !cfg.maceAnimals; MaceAuraMod.targetAnimals = cfg.maceAnimals; cfg.save(); }),
            new BoolS("Players", () -> cfg.macePlayers, () -> { cfg.macePlayers = !cfg.macePlayers; MaceAuraMod.targetPlayers = cfg.macePlayers; cfg.save(); }),
            new BoolS("Raycast", () -> cfg.maceRaycast,   () -> { cfg.maceRaycast = !cfg.maceRaycast;     MaceAuraMod.raycast = cfg.maceRaycast;         cfg.save(); }),
            new BoolS("Skip Inv",() -> cfg.maceSkipInvis, () -> { cfg.maceSkipInvis = !cfg.maceSkipInvis; MaceAuraMod.skipInvisible = cfg.maceSkipInvis; cfg.save(); }),
            new BoolS("Smooth",  () -> cfg.maceSmoothRot, () -> { cfg.maceSmoothRot = !cfg.maceSmoothRot; MaceAuraMod.smoothRot = cfg.maceSmoothRot;     cfg.save(); }),
            new SliderS("Max Turn", () -> cfg.maceMaxTurn, v -> { cfg.maceMaxTurn = (int)v; MaceAuraMod.maxTurnDeg = (int)v; cfg.save(); }, 10, 180),
            new BoolS("GCD",     () -> cfg.maceGcd,        () -> { cfg.maceGcd = !cfg.maceGcd;             MaceAuraMod.gcdSnap = cfg.maceGcd;             cfg.save(); }),
            new KeyS("Key", () -> cfg.maceAuraKey, v -> { cfg.maceAuraKey = v; cfg.save(); })
        )));

        add(Cat.COMBAT, new ModEntry("SpearAura", SpearAuraMod::isEnabled, SpearAuraMod::toggle, true, List.of(
            new SliderS("Mode",      () -> cfg.spearMode,        v -> { cfg.spearMode = (int)v;        SpearAuraMod.mode = (int)v;                     cfg.save(); }, 0, 2),
            new SliderS("Scan Rng",  () -> cfg.spearScanRange,   v -> { cfg.spearScanRange = (float)v; SpearAuraMod.SCAN_RANGE = (float)v;             cfg.save(); }, 3, 10),
            new SliderS("Min Reach", () -> cfg.spearMinReach,    v -> { cfg.spearMinReach = (float)v;  SpearAuraMod.MIN_REACH = (float)v;              cfg.save(); }, 0.5, 3),
            new SliderS("Max Reach", () -> cfg.spearMaxReach,    v -> { cfg.spearMaxReach = (float)v;  SpearAuraMod.MAX_REACH = (float)v;              cfg.save(); }, 2, 12),
            new SliderS("Chg Ticks", () -> cfg.spearChargeTicks, v -> { cfg.spearChargeTicks = (int)v; SpearAuraMod.chargeReleaseTicks = (int)v;       cfg.save(); }, 4, 30),
            new BoolS("Hostile",  () -> cfg.spearHostile,  () -> { cfg.spearHostile = !cfg.spearHostile;   SpearAuraMod.targetHostile = cfg.spearHostile; cfg.save(); }),
            new BoolS("Animals",  () -> cfg.spearAnimals,  () -> { cfg.spearAnimals = !cfg.spearAnimals;   SpearAuraMod.targetAnimals = cfg.spearAnimals; cfg.save(); }),
            new BoolS("Players",  () -> cfg.spearPlayers,  () -> { cfg.spearPlayers = !cfg.spearPlayers;   SpearAuraMod.targetPlayers = cfg.spearPlayers; cfg.save(); }),
            new BoolS("Step Back",() -> cfg.spearStepBack, () -> { cfg.spearStepBack = !cfg.spearStepBack; SpearAuraMod.autoStepBack = cfg.spearStepBack; cfg.save(); }),
            new BoolS("Raycast",  () -> cfg.spearRaycast,   () -> { cfg.spearRaycast = !cfg.spearRaycast;     SpearAuraMod.raycast = cfg.spearRaycast;         cfg.save(); }),
            new BoolS("Skip Inv", () -> cfg.spearSkipInvis, () -> { cfg.spearSkipInvis = !cfg.spearSkipInvis; SpearAuraMod.skipInvisible = cfg.spearSkipInvis; cfg.save(); }),
            new BoolS("Smooth",   () -> cfg.spearSmoothRot, () -> { cfg.spearSmoothRot = !cfg.spearSmoothRot; SpearAuraMod.smoothRot = cfg.spearSmoothRot;     cfg.save(); }),
            new SliderS("Max Turn", () -> cfg.spearMaxTurn, v -> { cfg.spearMaxTurn = (int)v; SpearAuraMod.maxTurnDeg = (int)v; cfg.save(); }, 10, 180),
            new BoolS("GCD",      () -> cfg.spearGcd,       () -> { cfg.spearGcd = !cfg.spearGcd;             SpearAuraMod.gcdSnap = cfg.spearGcd;             cfg.save(); }),
            new KeyS("Key", () -> cfg.spearAuraKey, v -> { cfg.spearAuraKey = v; cfg.save(); })
        )));

        add(Cat.COMBAT, new ModEntry("CritHit", CritMod::isEnabled, CritMod::toggle, true,
            List.of(new KeyS("Key", () -> cfg.critKey, v -> { cfg.critKey = v; cfg.save(); }))));

        add(Cat.COMBAT, new ModEntry("Hitbox",
            HitboxMod::isEnabled, () -> { HitboxMod.toggle(); cfg.hitboxEnabled = HitboxMod.isEnabled(); cfg.save(); }, true, List.of(
            new SliderS("Expand", () -> (double)cfg.hitboxExpand, v -> { cfg.hitboxExpand = (float)v; HitboxMod.expand = (float)v; cfg.save(); }, 0.05, 1.0),
            new BoolS("Players", () -> cfg.hitboxPlayers, () -> { cfg.hitboxPlayers = !cfg.hitboxPlayers; HitboxMod.players = cfg.hitboxPlayers; cfg.save(); }),
            new BoolS("Hostile", () -> cfg.hitboxHostile, () -> { cfg.hitboxHostile = !cfg.hitboxHostile; HitboxMod.hostile = cfg.hitboxHostile; cfg.save(); }),
            new BoolS("Animals", () -> cfg.hitboxAnimals, () -> { cfg.hitboxAnimals = !cfg.hitboxAnimals; HitboxMod.animals = cfg.hitboxAnimals; cfg.save(); })
        )));

        add(Cat.COMBAT, new ModEntry("AutoTotem",
            AutoTotemMod::isEnabled, () -> { AutoTotemMod.toggle(); cfg.autoTotemEnabled = AutoTotemMod.isEnabled(); cfg.save(); }, true,
            List.of(new KeyS("Key", () -> cfg.autoTotemKey, v -> { cfg.autoTotemKey = v; cfg.save(); }))));

        // ── Movement ────────────────────────────────────────────────────────
        add(Cat.MOVEMENT, new ModEntry("BHop", BHopMod::isEnabled, BHopMod::toggle, true, List.of(
            new SliderS("Speed",    () -> (double)cfg.bhopSpeed,      v -> { cfg.bhopSpeed = (float)v;      BHopMod.speedMultiplier = (float)v; cfg.save(); }, 0.5, 2.5),
            new BoolS("Low Hop",    () -> cfg.bhopLowHop,             () -> { cfg.bhopLowHop = !cfg.bhopLowHop;       BHopMod.lowHop = cfg.bhopLowHop;     cfg.save(); }),
            new SliderS("Jump Hgt", () -> (double)cfg.bhopJumpHeight, v -> { cfg.bhopJumpHeight = (float)v; BHopMod.jumpHeight = (float)v;     cfg.save(); }, 0.1, 1.0),
            new SliderS("Boost",    () -> (double)cfg.bhopBoost,      v -> { cfg.bhopBoost = (float)v;      BHopMod.boost = (float)v;          cfg.save(); }, 1.0, 1.5),
            new BoolS("KB Boost",   () -> cfg.bhopKBBoost,            () -> { cfg.bhopKBBoost = !cfg.bhopKBBoost;     BHopMod.kbBoost = cfg.bhopKBBoost;   cfg.save(); }),
            new KeyS("Key", () -> cfg.bhopKey, v -> { cfg.bhopKey = v; cfg.save(); })
        )));

        add(Cat.MOVEMENT, new ModEntry("Velocity", VelocityMod::isEnabled, VelocityMod::toggle, true, List.of(
            new SliderS("Horiz", () -> cfg.velocityHoriz, v -> { cfg.velocityHoriz = (int)v; VelocityMod.horizontal = (int)v; cfg.save(); }, 0, 200),
            new SliderS("Vert",  () -> cfg.velocityVert,  v -> { cfg.velocityVert = (int)v;  VelocityMod.vertical = (int)v;   cfg.save(); }, 0, 200),
            new KeyS("Key", () -> cfg.velocityKey, v -> { cfg.velocityKey = v; cfg.save(); })
        )));

        add(Cat.MOVEMENT, new ModEntry("NoFall", NoFallMod::isEnabled, NoFallMod::toggleManual, true, List.of(
            new SliderS("Mode", () -> cfg.nofallMode, v -> { cfg.nofallMode = (int)v; NoFallMod.mode = (int)v; cfg.save(); }, 0, 1),
            new BoolS("AntiKick", () -> cfg.nofallNoFlightKick, () -> { cfg.nofallNoFlightKick = !cfg.nofallNoFlightKick; NoFallMod.noFlightKick = cfg.nofallNoFlightKick; cfg.save(); }),
            new KeyS("Key", () -> cfg.nofallKey, v -> { cfg.nofallKey = v; cfg.save(); })
        )));
        add(Cat.MOVEMENT, new ModEntry("NoSlow", NoSlowMod::isEnabled, NoSlowMod::toggle, true,
            List.of(new KeyS("Key", () -> cfg.noSlowKey, v -> { cfg.noSlowKey = v; cfg.save(); }))));

        add(Cat.MOVEMENT, new ModEntry("Step", StepMod::isEnabled, StepMod::toggle, true, List.of(
            new SliderS("Height", () -> cfg.stepHeight, v -> { cfg.stepHeight = (float)v; StepMod.stepHeight = (float)v; cfg.save(); }, 1.0, 3.0),
            new KeyS("Key", () -> cfg.stepKey, v -> { cfg.stepKey = v; cfg.save(); })
        )));

        add(Cat.MOVEMENT, new ModEntry("Timer", TimerMod::isEnabled, TimerMod::toggle, true, List.of(
            new SliderS("Speed", () -> (double)cfg.timerSpeed, v -> { cfg.timerSpeed = (float)v; TimerMod.speed = (float)v; cfg.save(); }, 1.0, 3.0),
            new KeyS("Key", () -> cfg.timerKey, v -> { cfg.timerKey = v; cfg.save(); })
        )));

        add(Cat.MOVEMENT, new ModEntry("FastPlace", FastPlaceMod::isEnabled, FastPlaceMod::toggle, true,
            List.of(new KeyS("Key", () -> cfg.fastPlaceKey, v -> { cfg.fastPlaceKey = v; cfg.save(); }))));

        add(Cat.MOVEMENT, new ModEntry("Fly", FlyMod::isEnabled, FlyMod::toggle, true, List.of(
            new SliderS("H Speed", () -> cfg.flyHSpeed, v -> { cfg.flyHSpeed = (float)v; FlyMod.hSpeed = (float)v; cfg.save(); }, 0.2, 5.0),
            new SliderS("V Speed", () -> cfg.flyVSpeed, v -> { cfg.flyVSpeed = (float)v; FlyMod.vSpeed = (float)v; cfg.save(); }, 0.2, 5.0),
            new KeyS("Key", () -> cfg.flyKey, v -> { cfg.flyKey = v; cfg.save(); })
        )));

        add(Cat.MOVEMENT, new ModEntry("AutoWalk", AutoWalkMod::isEnabled, AutoWalkMod::toggle, true,
            List.of(new KeyS("Key", () -> cfg.autoWalkKey, v -> { cfg.autoWalkKey = v; cfg.save(); }))));

        // ── Player ──────────────────────────────────────────────────────────
        add(Cat.PLAYER, new ModEntry("Scaffold", ScaffoldMod::isEnabled, ScaffoldMod::toggle, true, List.of(
            new BoolS("Tower",     () -> cfg.scaffoldTower,      () -> { cfg.scaffoldTower = !cfg.scaffoldTower;           ScaffoldMod.tower = cfg.scaffoldTower;           cfg.save(); }),
            new BoolS("Twr Move",  () -> cfg.scaffoldTowerMove,  () -> { cfg.scaffoldTowerMove = !cfg.scaffoldTowerMove;   ScaffoldMod.towerMove = cfg.scaffoldTowerMove;   cfg.save(); }),
            new SliderS("Twr Spd", () -> cfg.scaffoldTowerSpeed, v -> { cfg.scaffoldTowerSpeed = (float)v; ScaffoldMod.towerSpeed = (float)v; cfg.save(); }, 0.0, 1.0),
            new BoolS("Fake Hand", () -> cfg.scaffoldFakeHand,   () -> { cfg.scaffoldFakeHand = !cfg.scaffoldFakeHand;     ScaffoldMod.fakeHand = cfg.scaffoldFakeHand;     cfg.save(); }),
            new BoolS("Silent Rt", () -> cfg.scaffoldSilentRot,  () -> { cfg.scaffoldSilentRot = !cfg.scaffoldSilentRot;   ScaffoldMod.silentRot = cfg.scaffoldSilentRot;   cfg.save(); }),
            new BoolS("Auto Sw",   () -> cfg.scaffoldAutoSwitch, () -> { cfg.scaffoldAutoSwitch = !cfg.scaffoldAutoSwitch; ScaffoldMod.autoSwitch = cfg.scaffoldAutoSwitch; cfg.save(); }),
            new BoolS("Sw Back",   () -> cfg.scaffoldSwitchBack, () -> { cfg.scaffoldSwitchBack = !cfg.scaffoldSwitchBack; ScaffoldMod.switchBack = cfg.scaffoldSwitchBack; cfg.save(); }),
            new SliderS("Delay",   () -> cfg.scaffoldPlaceDelay, v -> { cfg.scaffoldPlaceDelay = (int)v; ScaffoldMod.placeDelay = (int)v; cfg.save(); }, 0, 10),
            new KeyS("Key", () -> cfg.scaffoldKey, v -> { cfg.scaffoldKey = v; cfg.save(); })
        )));

        add(Cat.PLAYER, new ModEntry("AutoFish",
            AutoFishMod::isEnabled, () -> { AutoFishMod.toggle(); cfg.autoFishEnabled = AutoFishMod.isEnabled(); cfg.save(); }, true, List.of(
            new SliderS("Bite Vy", () -> (double)cfg.autoFishBiteVy, v -> { cfg.autoFishBiteVy = (float)v; AutoFishMod.biteVy = (float)v;       cfg.save(); }, -0.2, -0.01),
            new SliderS("Recast",  () -> (double)cfg.autoFishRecast, v -> { cfg.autoFishRecast = (int)v;    AutoFishMod.recastDelay = (int)v;    cfg.save(); }, 4, 40)
        )));

        add(Cat.PLAYER, new ModEntry("ElytraGoto", ElytraGotoMod::isEnabled, ElytraGotoMod::toggle, true, List.of(
            new SliderS("Safe HP", () -> (double)cfg.elytraSafeHp, v -> { cfg.elytraSafeHp = (float)v; ElytraGotoMod.safeHpThreshold = (float)v; cfg.save(); }, 2, 20)
        )));

        add(Cat.PLAYER, new ModEntry("AutoEat",
            AutoEatMod::isEnabled, () -> { AutoEatMod.toggle(); cfg.autoEatEnabled = AutoEatMod.isEnabled(); cfg.save(); }, true, List.of(
            new SliderS("Hunger", () -> (double)cfg.autoEatThreshold, v -> { cfg.autoEatThreshold = (int)v; AutoEatMod.threshold = (int)v; cfg.save(); }, 6, 19)
        )));

        add(Cat.PLAYER, new ModEntry("NoCrash",
            NoCrashMod::isEnabled, () -> { NoCrashMod.toggle(); cfg.noCrashEnabled = NoCrashMod.isEnabled(); cfg.save(); }, true, List.of(
            new SliderS("Look Ahd", () -> (double)cfg.noCrashLookAhead, v -> { cfg.noCrashLookAhead = (float)v; NoCrashMod.lookahead = (float)v; cfg.save(); }, 2, 10),
            new SliderS("Max Spd",  () -> (double)cfg.noCrashMaxSpeed,  v -> { cfg.noCrashMaxSpeed = (float)v;  NoCrashMod.maxSpeed = (float)v;  cfg.save(); }, 0.1, 1.0)
        )));

        add(Cat.PLAYER, new ModEntry("FastBreak", FastBreakMod::isEnabled, FastBreakMod::toggle, true,
            List.of(new KeyS("Key", () -> cfg.fastBreakKey, v -> { cfg.fastBreakKey = v; cfg.save(); }))));

        add(Cat.PLAYER, new ModEntry("AutoTool", AutoToolMod::isEnabled, AutoToolMod::toggle, true, List.of(
            new BoolS("Sw Back", () -> cfg.autoToolSwitchBack, () -> { cfg.autoToolSwitchBack = !cfg.autoToolSwitchBack; AutoToolMod.switchBack = cfg.autoToolSwitchBack; cfg.save(); }),
            new KeyS("Key", () -> cfg.autoToolKey, v -> { cfg.autoToolKey = v; cfg.save(); })
        )));

        add(Cat.PLAYER, new ModEntry("Nuker", NukerMod::isEnabled, NukerMod::toggle, true, List.of(
            new SliderS("Range", () -> (double)cfg.nukerRange, v -> { cfg.nukerRange = (float)v; NukerMod.range = (float)v; cfg.save(); }, 2.0, 5.0),
            new KeyS("Key", () -> cfg.nukerKey, v -> { cfg.nukerKey = v; cfg.save(); })
        )));

        // ── Render ──────────────────────────────────────────────────────────
        add(Cat.RENDER, new ModEntry("XRay", XRayMod::isEnabled, XRayMod::toggle, true,
            List.of(new KeyS("Key", () -> cfg.xrayKey, v -> { cfg.xrayKey = v; cfg.save(); }))));

        add(Cat.RENDER, new ModEntry("ESP",
            ESPMod::isEnabled, () -> { ESPMod.toggle(); cfg.espEnabled = ESPMod.isEnabled(); cfg.save(); }, true, List.of(
            // Targets
            new BoolS("Players",  () -> cfg.espPlayers, () -> { cfg.espPlayers = !cfg.espPlayers; ESPMod.players = cfg.espPlayers; cfg.save(); }),
            new BoolS("Hostile",  () -> cfg.espHostile, () -> { cfg.espHostile = !cfg.espHostile; ESPMod.hostile = cfg.espHostile; cfg.save(); }),
            new BoolS("Animals",  () -> cfg.espAnimals, () -> { cfg.espAnimals = !cfg.espAnimals; ESPMod.animals = cfg.espAnimals; cfg.save(); }),
            new BoolS("Items",    () -> cfg.espItems,   () -> { cfg.espItems   = !cfg.espItems;   ESPMod.items   = cfg.espItems;   cfg.save(); }),
            // Box
            new SliderS("Thick",   () -> cfg.espLineThick,   v -> { cfg.espLineThick   = (int)v;   ESPMod.lineThickness = (int)v;   cfg.save(); }, 1, 3),
            new SliderS("MaxDist", () -> cfg.espMaxDistance, v -> { cfg.espMaxDistance = (float)v; ESPMod.maxDistance   = (float)v; cfg.save(); }, 16, 200),
            // Labels
            new BoolS("Name",     () -> cfg.espShowName,     () -> { cfg.espShowName     = !cfg.espShowName;     ESPMod.showName     = cfg.espShowName;     cfg.save(); }),
            new BoolS("HP",       () -> cfg.espShowHp,       () -> { cfg.espShowHp       = !cfg.espShowHp;       ESPMod.showHp       = cfg.espShowHp;       cfg.save(); }),
            new BoolS("Distance", () -> cfg.espShowDistance, () -> { cfg.espShowDistance = !cfg.espShowDistance; ESPMod.showDistance = cfg.espShowDistance; cfg.save(); }),
            new BoolS("Tracer",   () -> cfg.espShowTracer,   () -> { cfg.espShowTracer   = !cfg.espShowTracer;   ESPMod.showTracer   = cfg.espShowTracer;   cfg.save(); }),
            new BoolS("Glow",     () -> cfg.espGlow,         () -> { cfg.espGlow         = !cfg.espGlow;         ESPMod.glow         = cfg.espGlow;         cfg.save(); }),
            // Border color
            new SliderS("Red",     () -> cfg.espRed,   v -> { cfg.espRed   = (int)v; ESPMod.red   = cfg.espRed;   cfg.save(); }, 0, 255),
            new SliderS("Green",   () -> cfg.espGreen, v -> { cfg.espGreen = (int)v; ESPMod.green = cfg.espGreen; cfg.save(); }, 0, 255),
            new SliderS("Blue",    () -> cfg.espBlue,  v -> { cfg.espBlue  = (int)v; ESPMod.blue  = cfg.espBlue;  cfg.save(); }, 0, 255)
        )));

        add(Cat.RENDER, new ModEntry("NameTag",
            NameTagMod::isEnabled, () -> { NameTagMod.toggle(); cfg.nameTagEnabled = NameTagMod.isEnabled(); cfg.save(); }, true, List.of(
            new BoolS("Players",    () -> cfg.nameTagPlayers,        () -> { cfg.nameTagPlayers        = !cfg.nameTagPlayers;        NameTagMod.players        = cfg.nameTagPlayers;        cfg.save(); }),
            new BoolS("Hostile",    () -> cfg.nameTagHostile,        () -> { cfg.nameTagHostile        = !cfg.nameTagHostile;        NameTagMod.hostile        = cfg.nameTagHostile;        cfg.save(); }),
            new BoolS("Animals",    () -> cfg.nameTagAnimals,        () -> { cfg.nameTagAnimals        = !cfg.nameTagAnimals;        NameTagMod.animals        = cfg.nameTagAnimals;        cfg.save(); }),
            new BoolS("Armor",      () -> cfg.nameTagShowArmor,      () -> { cfg.nameTagShowArmor      = !cfg.nameTagShowArmor;      NameTagMod.showArmor      = cfg.nameTagShowArmor;      cfg.save(); }),
            new BoolS("Hands",      () -> cfg.nameTagShowHands,      () -> { cfg.nameTagShowHands      = !cfg.nameTagShowHands;      NameTagMod.showHands      = cfg.nameTagShowHands;      cfg.save(); }),
            new BoolS("Durability", () -> cfg.nameTagShowDurability, () -> { cfg.nameTagShowDurability = !cfg.nameTagShowDurability; NameTagMod.showDurability = cfg.nameTagShowDurability; cfg.save(); }),
            new BoolS("HP Bar",     () -> cfg.nameTagShowHpBar,      () -> { cfg.nameTagShowHpBar      = !cfg.nameTagShowHpBar;      NameTagMod.showHpBar      = cfg.nameTagShowHpBar;      cfg.save(); }),
            new BoolS("HP Text",    () -> cfg.nameTagShowHpText,     () -> { cfg.nameTagShowHpText     = !cfg.nameTagShowHpText;     NameTagMod.showHpText     = cfg.nameTagShowHpText;     cfg.save(); }),
            new SliderS("Scale",    () -> cfg.nameTagScale * 100,    v -> { cfg.nameTagScale = (float)v / 100f; NameTagMod.scale = cfg.nameTagScale; cfg.save(); }, 60, 160),
            new SliderS("MaxDist",  () -> cfg.nameTagMaxDistance,    v -> { cfg.nameTagMaxDistance = (float)v; NameTagMod.maxDistance = (float)v; cfg.save(); }, 16, 128),
            new BoolS("Theme Col",  () -> cfg.nameTagUseTheme,       () -> { cfg.nameTagUseTheme = !cfg.nameTagUseTheme; NameTagMod.useTheme = cfg.nameTagUseTheme; cfg.save(); }),
            new SliderS("Red",      () -> cfg.nameTagR,              v -> { cfg.nameTagR = (int)v; NameTagMod.colorR = (int)v; cfg.save(); }, 0, 255),
            new SliderS("Green",    () -> cfg.nameTagG,              v -> { cfg.nameTagG = (int)v; NameTagMod.colorG = (int)v; cfg.save(); }, 0, 255),
            new SliderS("Blue",     () -> cfg.nameTagB,              v -> { cfg.nameTagB = (int)v; NameTagMod.colorB = (int)v; cfg.save(); }, 0, 255),
            new KeyS("Key",         () -> cfg.nameTagKey,            v -> { cfg.nameTagKey = v; cfg.save(); })
        )));

        add(Cat.RENDER, new ModEntry("DimCoord",
            NetherCoordMod::isEnabled, () -> { NetherCoordMod.toggle(); cfg.netherCoordEnabled = NetherCoordMod.isEnabled(); cfg.save(); },
            true, List.of()));

        add(Cat.RENDER, new ModEntry("Freecam", FreecamMod::isEnabled, FreecamMod::toggle, true, List.of(
            new SliderS("Speed", () -> cfg.freecamSpeed, v -> { cfg.freecamSpeed = (float)v; FreecamMod.speed = (float)v; cfg.save(); }, 0.2, 5.0),
            new KeyS("Key", () -> cfg.freecamKey, v -> { cfg.freecamKey = v; cfg.save(); })
        )));

        add(Cat.RENDER, new ModEntry("TargetHUD",
            TargetHudMod::isEnabled, () -> { TargetHudMod.toggle(); cfg.targetHudEnabled = TargetHudMod.isEnabled(); cfg.save(); },
            true, List.of(
            new SliderS("Style", () -> cfg.targetHudStyle, v -> { cfg.targetHudStyle = (int)v; cfg.save(); }, 0, 4)
        )));

        add(Cat.RENDER, new ModEntry("Waypoints",
            WaypointsMod::isEnabled, () -> { WaypointsMod.toggle(); cfg.waypointsEnabled = WaypointsMod.isEnabled(); cfg.save(); },
            true, List.of()));

        add(Cat.RENDER, new ModEntry("ArrayList",
            ArrayListMod::isEnabled, () -> { ArrayListMod.toggle(); cfg.arrayListEnabled = ArrayListMod.isEnabled(); cfg.save(); }, true, List.of(
            new BoolS("Rainbow", () -> cfg.arrayListRainbow,    () -> { cfg.arrayListRainbow = !cfg.arrayListRainbow;       ArrayListMod.rainbow = cfg.arrayListRainbow;       cfg.save(); }),
            new BoolS("BG",      () -> cfg.arrayListBackground, () -> { cfg.arrayListBackground = !cfg.arrayListBackground; ArrayListMod.background = cfg.arrayListBackground; cfg.save(); })
        )));

        // ── Client ──────────────────────────────────────────────────────────
        add(Cat.CLIENT, new ModEntry("Theme", () -> false, () -> {}, false, List.of(
            new SliderS("Layout",    () -> cfg.guiLayout, v -> { cfg.guiLayout = (int)v; cfg.save(); }, 0, 4),
            new SliderS("GUI Style", () -> cfg.guiStyle, v -> { cfg.guiStyle = (int)v; cfg.save(); }, 0, 4),
            new SliderS("Red",      () -> cfg.accentR, v -> { cfg.accentR = (int)v; cfg.save(); }, 0, 255),
            new SliderS("Green",    () -> cfg.accentG, v -> { cfg.accentG = (int)v; cfg.save(); }, 0, 255),
            new SliderS("Blue",     () -> cfg.accentB, v -> { cfg.accentB = (int)v; cfg.save(); }, 0, 255),
            new SliderS("BG Alpha", () -> cfg.bgAlpha, v -> { cfg.bgAlpha = (int)v; cfg.save(); }, 60, 240)
        )));
        add(Cat.CLIENT, new ModEntry("GUI Key", () -> false, () -> {}, false,
            List.of(new KeyS("Key", () -> cfg.guiKey, v -> { cfg.guiKey = v; cfg.save(); }))));
        add(Cat.CLIENT, new ModEntry("Panic", () -> false, () -> {}, false,
            List.of(new KeyS("Key", () -> cfg.panicKey, v -> { cfg.panicKey = v; cfg.save(); }))));

        return panels;
    }

    private void add(Cat cat, ModEntry m) {
        for (Panel p : panels) if (p.cat == cat) { p.mods.add(m); return; }
    }
}
