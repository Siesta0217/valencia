package com.valencia;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the ClickGUI module/panel data.
 *
 * Module rows are DERIVED from {@link Modules#ALL} — category, enabled state,
 * toggle (with persistence) and the trailing "Key" bind setting all come from
 * the ModuleDef, so the GUI can never drift from the real roster. Only each
 * module's settings list (sliders/booleans wired to ModConfig fields and
 * module statics) stays hand-written here, keyed by module label.
 *
 * Non-module rows (Theme / GUI Key / Panic) remain hand-written at the end.
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

        Map<String, List<Setting>> settings = settingsByLabel(cfg);

        // ── Module rows: one per registry entry, in registry order ─────────
        for (Modules.ModuleDef d : Modules.ALL) {
            List<Setting> s = new ArrayList<>(settings.getOrDefault(d.label(), List.of()));
            if (d.hasKey()) {
                s.add(new KeyS("Key",
                    () -> d.keyGet().getAsInt(),
                    v -> { d.keySet().accept(v); ModConfig.get().save(); }));
            }
            add(d.category(), new ModEntry(
                d.label(),
                d.enabled(),
                () -> {
                    d.toggle().run();
                    if (d.persisted()) {
                        d.persistSet().accept(d.enabled().getAsBoolean());
                        ModConfig.get().save();
                    }
                },
                true,
                List.copyOf(s)
            ));
        }

        // ── Client (non-module rows) ────────────────────────────────────────
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

    /**
     * Per-module settings (sliders / bools), keyed by ModuleDef label. The
     * trailing "Key" bind row is appended automatically for keybound modules —
     * don't add it here. Aura settings closures are verbatim from the previous
     * hand-written table; do not restructure them.
     */
    private static Map<String, List<Setting>> settingsByLabel(ModConfig cfg) {
        Map<String, List<Setting>> m = new HashMap<>();

        m.put("KillAura", List.of(
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
            new SliderS("CPS Jit", () -> cfg.killCpsJitter, v -> { cfg.killCpsJitter = (int)v; KillAuraMod.cpsJitter = (int)v; cfg.save(); }, 0, 5)
        ));

        m.put("MaceAura", List.of(
            new SliderS("Det Range", () -> cfg.maceDetectRange, v -> { cfg.maceDetectRange = (float)v; MaceAuraMod.RANGE = (float)v;        cfg.save(); }, 1, 12),
            new SliderS("Atk Range", () -> cfg.maceAttackRange, v -> { cfg.maceAttackRange = (float)v; MaceAuraMod.ATTACK_RANGE = (float)v; cfg.save(); }, 1, 8),
            new BoolS("Hostile", () -> cfg.maceHostile, () -> { cfg.maceHostile = !cfg.maceHostile; MaceAuraMod.targetHostile = cfg.maceHostile; cfg.save(); }),
            new BoolS("Animals", () -> cfg.maceAnimals, () -> { cfg.maceAnimals = !cfg.maceAnimals; MaceAuraMod.targetAnimals = cfg.maceAnimals; cfg.save(); }),
            new BoolS("Players", () -> cfg.macePlayers, () -> { cfg.macePlayers = !cfg.macePlayers; MaceAuraMod.targetPlayers = cfg.macePlayers; cfg.save(); }),
            new BoolS("Raycast", () -> cfg.maceRaycast,   () -> { cfg.maceRaycast = !cfg.maceRaycast;     MaceAuraMod.raycast = cfg.maceRaycast;         cfg.save(); }),
            new BoolS("Skip Inv",() -> cfg.maceSkipInvis, () -> { cfg.maceSkipInvis = !cfg.maceSkipInvis; MaceAuraMod.skipInvisible = cfg.maceSkipInvis; cfg.save(); }),
            new BoolS("Smooth",  () -> cfg.maceSmoothRot, () -> { cfg.maceSmoothRot = !cfg.maceSmoothRot; MaceAuraMod.smoothRot = cfg.maceSmoothRot;     cfg.save(); }),
            new SliderS("Max Turn", () -> cfg.maceMaxTurn, v -> { cfg.maceMaxTurn = (int)v; MaceAuraMod.maxTurnDeg = (int)v; cfg.save(); }, 10, 180),
            new BoolS("GCD",     () -> cfg.maceGcd,        () -> { cfg.maceGcd = !cfg.maceGcd;             MaceAuraMod.gcdSnap = cfg.maceGcd;             cfg.save(); })
        ));

        m.put("SpearAura", List.of(
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
            new BoolS("GCD",      () -> cfg.spearGcd,       () -> { cfg.spearGcd = !cfg.spearGcd;             SpearAuraMod.gcdSnap = cfg.spearGcd;             cfg.save(); })
        ));

        m.put("Hitbox", List.of(
            new SliderS("Expand", () -> (double)cfg.hitboxExpand, v -> { cfg.hitboxExpand = (float)v; HitboxMod.expand = (float)v; cfg.save(); }, 0.05, 1.0),
            new BoolS("Players", () -> cfg.hitboxPlayers, () -> { cfg.hitboxPlayers = !cfg.hitboxPlayers; HitboxMod.players = cfg.hitboxPlayers; cfg.save(); }),
            new BoolS("Hostile", () -> cfg.hitboxHostile, () -> { cfg.hitboxHostile = !cfg.hitboxHostile; HitboxMod.hostile = cfg.hitboxHostile; cfg.save(); }),
            new BoolS("Animals", () -> cfg.hitboxAnimals, () -> { cfg.hitboxAnimals = !cfg.hitboxAnimals; HitboxMod.animals = cfg.hitboxAnimals; cfg.save(); })
        ));

        m.put("BHop", List.of(
            new SliderS("Speed",    () -> (double)cfg.bhopSpeed,      v -> { cfg.bhopSpeed = (float)v;      BHopMod.speedMultiplier = (float)v; cfg.save(); }, 0.5, 2.5),
            new BoolS("Low Hop",    () -> cfg.bhopLowHop,             () -> { cfg.bhopLowHop = !cfg.bhopLowHop;       BHopMod.lowHop = cfg.bhopLowHop;     cfg.save(); }),
            new SliderS("Jump Hgt", () -> (double)cfg.bhopJumpHeight, v -> { cfg.bhopJumpHeight = (float)v; BHopMod.jumpHeight = (float)v;     cfg.save(); }, 0.1, 1.0),
            new SliderS("Boost",    () -> (double)cfg.bhopBoost,      v -> { cfg.bhopBoost = (float)v;      BHopMod.boost = (float)v;          cfg.save(); }, 1.0, 1.5),
            new BoolS("KB Boost",   () -> cfg.bhopKBBoost,            () -> { cfg.bhopKBBoost = !cfg.bhopKBBoost;     BHopMod.kbBoost = cfg.bhopKBBoost;   cfg.save(); })
        ));

        m.put("Velocity", List.of(
            new SliderS("Horiz", () -> cfg.velocityHoriz, v -> { cfg.velocityHoriz = (int)v; VelocityMod.horizontal = (int)v; cfg.save(); }, 0, 200),
            new SliderS("Vert",  () -> cfg.velocityVert,  v -> { cfg.velocityVert = (int)v;  VelocityMod.vertical = (int)v;   cfg.save(); }, 0, 200)
        ));

        m.put("NoFall", List.of(
            new SliderS("Mode", () -> cfg.nofallMode, v -> { cfg.nofallMode = (int)v; NoFallMod.mode = (int)v; cfg.save(); }, 0, 1),
            new BoolS("AntiKick", () -> cfg.nofallNoFlightKick, () -> { cfg.nofallNoFlightKick = !cfg.nofallNoFlightKick; NoFallMod.noFlightKick = cfg.nofallNoFlightKick; cfg.save(); })
        ));

        m.put("Step", List.of(
            new SliderS("Height", () -> cfg.stepHeight, v -> { cfg.stepHeight = (float)v; StepMod.stepHeight = (float)v; cfg.save(); }, 1.0, 3.0)
        ));

        m.put("Timer", List.of(
            new SliderS("Speed", () -> (double)cfg.timerSpeed, v -> { cfg.timerSpeed = (float)v; TimerMod.speed = (float)v; cfg.save(); }, 1.0, 3.0)
        ));

        m.put("Fly", List.of(
            new SliderS("H Speed", () -> cfg.flyHSpeed, v -> { cfg.flyHSpeed = (float)v; FlyMod.hSpeed = (float)v; cfg.save(); }, 0.2, 5.0),
            new SliderS("V Speed", () -> cfg.flyVSpeed, v -> { cfg.flyVSpeed = (float)v; FlyMod.vSpeed = (float)v; cfg.save(); }, 0.2, 5.0)
        ));

        m.put("Scaffold", List.of(
            new BoolS("Tower",     () -> cfg.scaffoldTower,      () -> { cfg.scaffoldTower = !cfg.scaffoldTower;           ScaffoldMod.tower = cfg.scaffoldTower;           cfg.save(); }),
            new BoolS("Twr Move",  () -> cfg.scaffoldTowerMove,  () -> { cfg.scaffoldTowerMove = !cfg.scaffoldTowerMove;   ScaffoldMod.towerMove = cfg.scaffoldTowerMove;   cfg.save(); }),
            new SliderS("Twr Spd", () -> cfg.scaffoldTowerSpeed, v -> { cfg.scaffoldTowerSpeed = (float)v; ScaffoldMod.towerSpeed = (float)v; cfg.save(); }, 0.0, 1.0),
            new BoolS("Fake Hand", () -> cfg.scaffoldFakeHand,   () -> { cfg.scaffoldFakeHand = !cfg.scaffoldFakeHand;     ScaffoldMod.fakeHand = cfg.scaffoldFakeHand;     cfg.save(); }),
            new BoolS("Silent Rt", () -> cfg.scaffoldSilentRot,  () -> { cfg.scaffoldSilentRot = !cfg.scaffoldSilentRot;   ScaffoldMod.silentRot = cfg.scaffoldSilentRot;   cfg.save(); }),
            new BoolS("Auto Sw",   () -> cfg.scaffoldAutoSwitch, () -> { cfg.scaffoldAutoSwitch = !cfg.scaffoldAutoSwitch; ScaffoldMod.autoSwitch = cfg.scaffoldAutoSwitch; cfg.save(); }),
            new BoolS("Sw Back",   () -> cfg.scaffoldSwitchBack, () -> { cfg.scaffoldSwitchBack = !cfg.scaffoldSwitchBack; ScaffoldMod.switchBack = cfg.scaffoldSwitchBack; cfg.save(); }),
            new SliderS("Delay",   () -> cfg.scaffoldPlaceDelay, v -> { cfg.scaffoldPlaceDelay = (int)v; ScaffoldMod.placeDelay = (int)v; cfg.save(); }, 0, 10)
        ));

        m.put("AutoFish", List.of(
            new SliderS("Bite Vy", () -> (double)cfg.autoFishBiteVy, v -> { cfg.autoFishBiteVy = (float)v; AutoFishMod.biteVy = (float)v;       cfg.save(); }, -0.2, -0.01),
            new SliderS("Recast",  () -> (double)cfg.autoFishRecast, v -> { cfg.autoFishRecast = (int)v;    AutoFishMod.recastDelay = (int)v;    cfg.save(); }, 4, 40)
        ));

        m.put("ElytraGoto", List.of(
            new SliderS("Safe HP", () -> (double)cfg.elytraSafeHp, v -> { cfg.elytraSafeHp = (float)v; ElytraGotoMod.safeHpThreshold = (float)v; cfg.save(); }, 2, 20)
        ));

        m.put("AutoEat", List.of(
            new SliderS("Hunger", () -> (double)cfg.autoEatThreshold, v -> { cfg.autoEatThreshold = (int)v; AutoEatMod.threshold = (int)v; cfg.save(); }, 6, 19)
        ));

        m.put("NoCrash", List.of(
            new SliderS("Look Ahd", () -> (double)cfg.noCrashLookAhead, v -> { cfg.noCrashLookAhead = (float)v; NoCrashMod.lookahead = (float)v; cfg.save(); }, 2, 10),
            new SliderS("Max Spd",  () -> (double)cfg.noCrashMaxSpeed,  v -> { cfg.noCrashMaxSpeed = (float)v;  NoCrashMod.maxSpeed = (float)v;  cfg.save(); }, 0.1, 1.0)
        ));

        m.put("AutoTool", List.of(
            new BoolS("Sw Back", () -> cfg.autoToolSwitchBack, () -> { cfg.autoToolSwitchBack = !cfg.autoToolSwitchBack; AutoToolMod.switchBack = cfg.autoToolSwitchBack; cfg.save(); })
        ));

        m.put("Nuker", List.of(
            new SliderS("Range", () -> (double)cfg.nukerRange, v -> { cfg.nukerRange = (float)v; NukerMod.range = (float)v; cfg.save(); }, 2.0, 5.0)
        ));

        m.put("ESP", List.of(
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
        ));

        m.put("NameTag", List.of(
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
            new SliderS("Blue",     () -> cfg.nameTagB,              v -> { cfg.nameTagB = (int)v; NameTagMod.colorB = (int)v; cfg.save(); }, 0, 255)
        ));

        m.put("Freecam", List.of(
            new SliderS("Speed", () -> cfg.freecamSpeed, v -> { cfg.freecamSpeed = (float)v; FreecamMod.speed = (float)v; cfg.save(); }, 0.2, 5.0)
        ));

        m.put("TargetHUD", List.of(
            new SliderS("Style", () -> cfg.targetHudStyle, v -> { cfg.targetHudStyle = (int)v; cfg.save(); }, 0, 4)
        ));

        m.put("ArrayList", List.of(
            new BoolS("Rainbow", () -> cfg.arrayListRainbow,    () -> { cfg.arrayListRainbow = !cfg.arrayListRainbow;       ArrayListMod.rainbow = cfg.arrayListRainbow;       cfg.save(); }),
            new BoolS("BG",      () -> cfg.arrayListBackground, () -> { cfg.arrayListBackground = !cfg.arrayListBackground; ArrayListMod.background = cfg.arrayListBackground; cfg.save(); })
        ));

        return m;
    }

    private void add(Cat cat, ModEntry m) {
        for (Panel p : panels) if (p.cat == cat) { p.mods.add(m); return; }
    }
}
