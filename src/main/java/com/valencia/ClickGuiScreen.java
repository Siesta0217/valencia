package com.valencia;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import javax.imageio.ImageIO;

/**
 * Raven-style ClickGUI ported to Valencia / Fabric 1.21.11.
 *
 * Layout mimics Raven B++: draggable category panels, left-click toggle,
 * right-click expand settings, gradient enabled/disabled module rows,
 * half-scale labels, rounded slider bars, toggle switches.
 */
public class ClickGuiScreen extends Screen {

    // ── Inner types ─────────────────────────────────────────────────────────
    enum Cat {
        COMBAT("Combat"), MOVEMENT("Movement"), PLAYER("Player"),
        RENDER("Render"), CLIENT("Client");
        final String label;
        Cat(String l) { label = l; }
    }

    interface Setting {}
    record SliderS(String label, DoubleSupplier get, DoubleConsumer set, double min, double max) implements Setting {}
    record BoolS(String label, BooleanSupplier get, Runnable toggle) implements Setting {}
    record KeyS(String label, IntSupplier get, IntConsumer set) implements Setting {}

    static class ModEntry {
        final String name;
        final BooleanSupplier enabled;
        final Runnable toggle;
        final boolean toggleable;
        final List<Setting> settings;
        ModEntry(String n, BooleanSupplier e, Runnable t, boolean tog, List<Setting> s) {
            name = n; enabled = e; toggle = t; toggleable = tog; settings = s;
        }
    }

    static class Panel {
        final Cat cat;
        int x, y;
        boolean open = true;
        boolean dragging;
        int dragOX, dragOY;
        ModEntry expanded;
        int scrollOff;          // scroll offset for expanded settings
        final List<ModEntry> mods = new ArrayList<>();
        Panel(Cat c) { cat = c; }
    }

    // ── Sizes (Raven-style) ─────────────────────────────────────────────────
    private static final int HDR       = 15;   // category header height
    private static final int MOD_H     = 20;   // module row height (Raven=20)
    private static final int S_SLIDER  = 16;   // slider setting height
    private static final int S_BOOL    = 14;   // bool toggle height
    private static final int S_BIND    = 14;   // key bind height
    private static final int MAX_SET_H = 260;  // max visible settings height before scroll
    private int PANEL_W = 92;                  // panel width (Raven=92)

    // ── State ───────────────────────────────────────────────────────────────
    private final List<Panel> panels = new ArrayList<>();
    private Panel  sliderPanel;
    private int    sliderIdx  = -1;
    private KeyS   rebindTarget;   // key setting currently capturing input (either layout); null = none
    private long   openTime;
    private GuiSkin skin;   // resolved each frame from cfg.guiStyle (live switch)

    // ── Sidebar layout state (guiLayout == 1) ────────────────────────────────
    private int sbWinX = -1, sbWinY = -1;   // window top-left; -1 = center on first render
    private boolean sbDragging;
    private int sbDragOX, sbDragOY;
    private int sbCatIdx;                    // selected category (index into panels)
    private ModEntry sbExpanded;             // module shown in the settings pane (null = none)
    private int sbScrollMods, sbScrollSett;  // scroll offsets for the two scrollable columns
    private SliderS sbSliderActive;          // slider being dragged
    private int sbSliderX, sbSliderW;

    // ── Tenacity layout state (guiLayout == 2) ───────────────────────────────
    private int tnWinX = -1, tnWinY = -1;    // window top-left; -1 = center on first render
    private boolean tnDragging;
    private int tnDragOX, tnDragOY;
    private int tnCatIdx;                     // selected category (index into panels)
    private ModEntry tnExpanded;             // inline-expanded module card (null = none)
    private int tnScroll;                     // card-list scroll offset
    private SliderS tnSliderActive;          // slider being dragged
    private int tnSliderX, tnSliderW;
    private final java.util.Map<String, Float> tnKnob = new java.util.HashMap<>();  // pill knob slide anim

    // ── Waifu ───────────────────────────────────────────────────────────────
    private Identifier waifuLoc;
    private int waifuTexW, waifuTexH;
    private String waifuHint, waifuErr;

    // ═════════════════════════════════════════════════════════════════════════
    public ClickGuiScreen() {
        super(Component.empty());
        buildPanels();
        loadWaifu();
        openTime = System.currentTimeMillis();
    }

    @Override public boolean isPauseScreen() { return false; }

    // ── Build module/panel data ─────────────────────────────────────────────
    private void buildPanels() {
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

        add(Cat.PLAYER, new ModEntry("NoCrash",
            NoCrashMod::isEnabled, () -> { NoCrashMod.toggle(); cfg.noCrashEnabled = NoCrashMod.isEnabled(); cfg.save(); }, true, List.of(
            new SliderS("Look Ahd", () -> (double)cfg.noCrashLookAhead, v -> { cfg.noCrashLookAhead = (float)v; NoCrashMod.lookahead = (float)v; cfg.save(); }, 2, 10),
            new SliderS("Max Spd",  () -> (double)cfg.noCrashMaxSpeed,  v -> { cfg.noCrashMaxSpeed = (float)v;  NoCrashMod.maxSpeed = (float)v;  cfg.save(); }, 0.1, 1.0)
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
            new SliderS("Style", () -> cfg.targetHudStyle, v -> { cfg.targetHudStyle = (int)v; cfg.save(); }, 0, 3)
        )));

        add(Cat.RENDER, new ModEntry("ArrayList",
            ArrayListMod::isEnabled, () -> { ArrayListMod.toggle(); cfg.arrayListEnabled = ArrayListMod.isEnabled(); cfg.save(); }, true, List.of(
            new BoolS("Rainbow", () -> cfg.arrayListRainbow,    () -> { cfg.arrayListRainbow = !cfg.arrayListRainbow;       ArrayListMod.rainbow = cfg.arrayListRainbow;       cfg.save(); }),
            new BoolS("BG",      () -> cfg.arrayListBackground, () -> { cfg.arrayListBackground = !cfg.arrayListBackground; ArrayListMod.background = cfg.arrayListBackground; cfg.save(); })
        )));

        // ── Client ──────────────────────────────────────────────────────────
        add(Cat.CLIENT, new ModEntry("Theme", () -> false, () -> {}, false, List.of(
            new SliderS("Layout",    () -> cfg.guiLayout, v -> { cfg.guiLayout = (int)v; cfg.save(); }, 0, 2),
            new SliderS("GUI Style", () -> cfg.guiStyle, v -> { cfg.guiStyle = (int)v; cfg.save(); }, 0, 3),
            new SliderS("Red",      () -> cfg.accentR, v -> { cfg.accentR = (int)v; cfg.save(); }, 0, 255),
            new SliderS("Green",    () -> cfg.accentG, v -> { cfg.accentG = (int)v; cfg.save(); }, 0, 255),
            new SliderS("Blue",     () -> cfg.accentB, v -> { cfg.accentB = (int)v; cfg.save(); }, 0, 255),
            new SliderS("BG Alpha", () -> cfg.bgAlpha, v -> { cfg.bgAlpha = (int)v; cfg.save(); }, 60, 240)
        )));
        add(Cat.CLIENT, new ModEntry("GUI Key", () -> false, () -> {}, false,
            List.of(new KeyS("Key", () -> cfg.guiKey, v -> { cfg.guiKey = v; cfg.save(); }))));
        add(Cat.CLIENT, new ModEntry("Panic", () -> false, () -> {}, false,
            List.of(new KeyS("Key", () -> cfg.panicKey, v -> { cfg.panicKey = v; cfg.save(); }))));
    }

    private void add(Cat cat, ModEntry m) {
        for (Panel p : panels) if (p.cat == cat) { p.mods.add(m); return; }
    }

    // ── Layout ──────────────────────────────────────────────────────────────
    private boolean layoutDone = false;

    @Override
    protected void init() {
        super.init();
        if (!layoutDone) {
            PANEL_W = 92;
            int gap = 5;
            int xOff = gap, yOff = 5;
            for (Panel p : panels) {
                p.x = xOff;
                p.y = yOff;
                xOff += PANEL_W + gap;
                if (xOff + PANEL_W > width) {
                    xOff = gap;
                    yOff += 120;
                }
            }
            layoutDone = true;
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  RENDERING — Raven style
    // ═════════════════════════════════════════════════════════════════════════

    /** Astolfo / rainbow color cycle (Raven-style). */
    private static int astolfo(int yOffset, float speed) {
        float hue = (float)((System.currentTimeMillis() % (long)speed) + yOffset) / speed;
        return java.awt.Color.HSBtoRGB(hue % 1f, 0.55f, 1.0f);
    }

    /** Smooth gradient from top to bottom color. */
    private static int gradVert(int y, int topColor, int botColor) {
        // just return topColor; Raven uses GL shading — we approximate with solid
        return topColor;
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        super.render(g, mx, my, dt);
        ModConfig cfg = ModConfig.get();

        int accent = 0xFF000000 | (cfg.accentR << 16) | (cfg.accentG << 8) | cfg.accentB;
        skin = GuiSkin.of(cfg.guiStyle, accent);

        // semi-transparent dark background (Raven-style)
        float openAnim = Math.min(1f, (System.currentTimeMillis() - openTime) / 400f);
        int bgA = (int)(cfg.bgAlpha * openAnim);
        g.fill(0, 0, width, height, (bgA << 24));

        // waifu
        renderWaifu(g);

        // version string at bottom-left (Raven-style)
        Font font = Minecraft.getInstance().font;
        String verStr = "Valencia";
        int verColor = skin.rainbowVersion ? (astolfo(0, 4890f) | 0xFF000000) : accent;
        g.drawString(font, verStr, 5, height - font.lineHeight - 3, verColor, true);

        // active layout
        if (cfg.guiLayout == 2) renderTenacity(g, mx, my, accent, font);
        else if (cfg.guiLayout == 1) renderSidebar(g, mx, my, accent, font);
        else for (Panel p : panels) renderPanel(g, p, mx, my, accent, font);
    }

    private void renderPanel(GuiGraphics g, Panel p, int mx, int my, int accent, Font font) {
        int ph = panelH(p);
        int x1 = p.x, y1 = p.y, x2 = p.x + PANEL_W, y2 = p.y + ph;

        // ── panel background ────────────────────────────────────────────────
        g.fill(x1, y1, x2, y2, skin.panelBg);

        // ── header ──────────────────────────────────────────────────────────
        boolean hoverHdr = mx >= x1 && mx < x2 && my >= y1 && my < y1 + HDR;
        g.fill(x1, y1, x2, y1 + HDR, hoverHdr ? skin.headerHover : skin.headerBg);

        // category name (left side)
        g.drawString(font, p.cat.label, x1 + 3, y1 + (HDR - font.lineHeight) / 2 + 1, skin.catLabel, false);

        // +/- indicator (right side, Raven-style: green when open, red when closed)
        String sym = p.open ? "-" : "+";
        int symColor = p.open ? 0xFF55FF55 : 0xFFFF5555;
        g.drawString(font, sym, x2 - font.width(sym) - 3, y1 + (HDR - font.lineHeight) / 2 + 1, symColor, false);

        // thin accent line under header
        g.fill(x1, y1 + HDR - 1, x2, y1 + HDR, skin.headerUnderline);

        if (!p.open) return;

        if (p.expanded != null)
            drawExpanded(g, p, mx, my, font, accent);
        else
            drawModList(g, p, mx, my, font, accent);

        // border (Raven-style: outer border)
        boolean hoverPanel = mx >= x1 && mx < x2 && my >= y1 && my < y2;
        int borderColor = hoverPanel ? (accent & 0x00FFFFFF) | 0x80000000 : skin.borderIdle;
        drawBorder(g, x1, y1, x2, y2, borderColor);
    }

    // ── Module list (Raven: gradient bg for enabled, centered text) ──────────
    private void drawModList(GuiGraphics g, Panel p, int mx, int my, Font font, int accent) {
        int yo = p.y + HDR;
        for (ModEntry m : p.mods) {
            boolean hover = mx >= p.x && mx < p.x + PANEL_W && my >= yo && my < yo + MOD_H;
            boolean on = m.enabled.getAsBoolean();

            // enabled-row background (accent gradient or flat tint, per skin)
            if (on) {
                drawEnabledBg(g, p.x, yo, p.x + PANEL_W, yo + MOD_H, accent);
            } else if (hover) {
                g.fill(p.x, yo, p.x + PANEL_W, yo + MOD_H, skin.rowHover);
            }

            // centered module name (Raven-style)
            int tc = on ? skin.textOn : (m.toggleable ? skin.textDim : skin.textOff);
            int tw = font.width(m.name);
            g.drawString(font, m.name, p.x + (PANEL_W - tw) / 2, yo + (MOD_H - font.lineHeight) / 2, tc, skin.nameShadow);

            yo += MOD_H;
        }
    }

    // ── Expanded settings view (Raven: replaces module list) ────────────────
    private void drawExpanded(GuiGraphics g, Panel p, int mx, int my, Font font, int accent) {
        ModEntry m = p.expanded;
        int yo = p.y + HDR;

        // module name row with back arrow
        boolean on = m.enabled.getAsBoolean();
        if (on) {
            drawEnabledBg(g, p.x, yo, p.x + PANEL_W, yo + MOD_H, accent);
        } else {
            g.fill(p.x, yo, p.x + PANEL_W, yo + MOD_H, skin.expandedOffBg);
        }
        g.drawString(font, "« " + m.name, p.x + 3, yo + (MOD_H - font.lineHeight) / 2, on ? skin.textOn : skin.textDim, skin.nameShadow);
        yo += MOD_H;

        // settings area background
        int settStart = yo;
        int totalSettH = totalSettingsH(m);
        int visH = Math.min(totalSettH, MAX_SET_H);
        g.fill(p.x, settStart, p.x + PANEL_W, settStart + visH, skin.settingsBg);

        // clamp scroll
        int maxScroll = Math.max(0, totalSettH - MAX_SET_H);
        if (p.scrollOff > maxScroll) p.scrollOff = maxScroll;
        if (p.scrollOff < 0) p.scrollOff = 0;

        // draw settings with scroll offset
        int sx = p.x + 4;
        int sw = PANEL_W - 8;
        int drawY = settStart - p.scrollOff;
        for (int i = 0; i < m.settings.size(); i++) {
            Setting s = m.settings.get(i);
            int sh = settH(s);

            // only draw if visible
            if (drawY + sh > settStart && drawY < settStart + visH) {
                if (s instanceof SliderS sl) {
                    drawSlider(g, sl, sx, drawY, sw, font, accent);
                } else if (s instanceof BoolS bs) {
                    drawBool(g, bs, sx, drawY, font, accent);
                } else if (s instanceof KeyS ks) {
                    drawBind(g, ks, sx, drawY, font, rebindTarget == ks);
                }
            }
            drawY += sh;
        }

        // scroll indicator
        if (totalSettH > MAX_SET_H) {
            float scrollPct = (float) p.scrollOff / maxScroll;
            int barH = Math.max(8, visH * visH / totalSettH);
            int barY = settStart + (int)((visH - barH) * scrollPct);
            g.fill(p.x + PANEL_W - 2, barY, p.x + PANEL_W, barY + barH, skin.scrollBar);
        }
    }

    // ── Slider (Raven-style: half-scale label + rounded bar) ────────────────
    private void drawSlider(GuiGraphics g, SliderS sl, int x, int y, int w, Font font, int accent) {
        double val = sl.get().getAsDouble();
        String txt = sl.label() + ": " + fmtVal(val);

        // half-scale label (Raven-style)
        g.pose().pushMatrix();
        g.pose().scale(0.5f, 0.5f);
        g.drawString(font, txt, x * 2, y * 2, skin.textOn, false);
        g.pose().popMatrix();

        // slider bar
        int barY = y + 6;
        int barH = S_SLIDER - 7;
        double pct = Math.max(0, Math.min(1, (val - sl.min()) / (sl.max() - sl.min())));
        int filled = (int)(w * pct);

        // bar background (dark, Raven: bordered rounded rect)
        g.fill(x, barY, x + w, barY + barH, skin.sliderTrack);
        drawBorder(g, x, barY, x + w, barY + barH, skin.sliderTrackBorder);

        // filled portion (accent-tinted, Raven uses purple — we use accent)
        if (filled > 0) {
            int fc = (skin.sliderFillAlpha << 24) | (accent & 0x00FFFFFF);
            g.fill(x, barY, x + filled, barY + barH, fc);
        }
    }

    // ── Boolean toggle (Raven-style: sliding switch) ────────────────────────
    private void drawBool(GuiGraphics g, BoolS bs, int x, int y, Font font, int accent) {
        boolean on = bs.get().getAsBoolean();

        int bw = 14, bh = 7;
        int by = y + (S_BOOL - bh) / 2;

        // track background
        g.fill(x, by, x + bw, by + bh, skin.boolTrack);
        drawBorder(g, x, by, x + bw, by + bh, skin.widgetBorder);

        // sliding indicator (Raven: green/red)
        int indW = bw / 2;
        int indX = on ? x + bw - indW : x;
        int indColor = on ? 0xFF55FF55 : 0xFFFF5555;
        g.fill(indX, by, indX + indW, by + bh, indColor);

        // half-scale label
        g.pose().pushMatrix();
        g.pose().scale(0.5f, 0.5f);
        g.drawString(font, bs.label(), (x + bw + 3) * 2, (y + S_BOOL / 2) * 2, skin.textOn, false);
        g.pose().popMatrix();
    }

    // ── Key bind ────────────────────────────────────────────────────────────
    private void drawBind(GuiGraphics g, KeyS ks, int x, int y, Font font, boolean binding) {
        String txt = binding ? "§ePress a key..." : "Bind: §f" + ModConfig.keyName(ks.get().getAsInt());
        g.pose().pushMatrix();
        g.pose().scale(0.5f, 0.5f);
        g.drawString(font, txt, x * 2, (y + S_BIND / 2) * 2, skin.textOn, false);
        g.pose().popMatrix();
    }

    // ── Drawing utilities ───────────────────────────────────────────────────
    private void drawBorder(GuiGraphics g, int x1, int y1, int x2, int y2, int c) {
        g.fill(x1, y1, x2, y1 + 1, c);      // top
        g.fill(x1, y2 - 1, x2, y2, c);      // bottom
        g.fill(x1, y1, x1 + 1, y2, c);      // left
        g.fill(x2 - 1, y1, x2, y2, c);      // right
    }

    /** Approximate vertical gradient via two halves (cheap, no GL shading). */
    private void drawGradientV(GuiGraphics g, int x1, int y1, int x2, int y2, int topC, int botC) {
        int midY = (y1 + y2) / 2;
        g.fill(x1, y1, x2, midY, topC);
        g.fill(x1, midY, x2, y2, botC);
    }

    /** Enabled-row background: accent vertical gradient (Dark/Glass) or a flat
     *  accent tint (Light), depending on the active skin. */
    private void drawEnabledBg(GuiGraphics g, int x1, int y1, int x2, int y2, int accent) {
        if (skin.enabledRowGradient) {
            int topC = (accent & 0x00FFFFFF) | 0xB0000000;
            int botC = (accent & 0x00FFFFFF) | 0x60000000;
            drawGradientV(g, x1, y1, x2, y2, topC, botC);
        } else {
            g.fill(x1, y1, x2, y2, (accent & 0x00FFFFFF) | (skin.enabledFlatAlpha << 24));
        }
    }

    private int panelH(Panel p) {
        if (!p.open) return HDR;
        if (p.expanded != null) {
            int totalS = totalSettingsH(p.expanded);
            return HDR + MOD_H + Math.min(totalS, MAX_SET_H);
        }
        return HDR + p.mods.size() * MOD_H;
    }

    private int totalSettingsH(ModEntry m) {
        int h = 0;
        for (Setting s : m.settings) h += settH(s);
        return h;
    }

    private int settH(Setting s) {
        if (s instanceof SliderS) return S_SLIDER;
        if (s instanceof BoolS)   return S_BOOL;
        if (s instanceof KeyS)    return S_BIND;
        return 0;
    }

    private String fmtVal(double v) {
        if (v == Math.floor(v) && !Double.isInfinite(v)) return String.format("%.0f", v);
        return String.format("%.1f", v);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  SIDEBAR LAYOUT (guiLayout == 1) — single centered window, fully separate
    //  geometry & hit-testing from the Raven panels. Shares the module data
    //  (panels), the GuiSkin colors and the drawSlider/Bool/Bind widgets.
    // ═════════════════════════════════════════════════════════════════════════
    private static final int SB_W     = 300;
    private static final int SB_H     = 196;
    private static final int SB_TITLE = 16;
    private static final int SB_SIDE  = 58;    // category column width
    private static final int SB_LIST  = 100;   // module column width
    private static final int SB_CAT_H = 18;
    private static final int SB_ROW_H = 14;

    private void ensureSidebarPos() {
        if (sbWinX < 0) { sbWinX = (width - SB_W) / 2; sbWinY = (height - SB_H) / 2; }
    }

    private void renderSidebar(GuiGraphics g, int mx, int my, int accent, Font font) {
        ensureSidebarPos();
        int x = sbWinX, y = sbWinY, x2 = x + SB_W, y2 = y + SB_H;
        int aTint = accent & 0x00FFFFFF;

        g.fill(x, y, x2, y2, skin.panelBg);

        // ── title bar (drag handle + close) ──
        boolean hoverTitle = mx >= x && mx < x2 - 14 && my >= y && my < y + SB_TITLE;
        g.fill(x, y, x2, y + SB_TITLE, hoverTitle ? skin.headerHover : skin.headerBg);
        g.drawString(font, "Valencia", x + 6, y + (SB_TITLE - font.lineHeight) / 2 + 1, skin.catLabel, skin.nameShadow);
        boolean hoverClose = mx >= x2 - 14 && mx < x2 && my >= y && my < y + SB_TITLE;
        g.drawString(font, "x", x2 - 9, y + (SB_TITLE - font.lineHeight) / 2 + 1, hoverClose ? 0xFFFF5555 : skin.textDim, false);
        g.fill(x, y + SB_TITLE - 1, x2, y + SB_TITLE, skin.headerUnderline);

        int contentY = y + SB_TITLE;
        int sideX2 = x + SB_SIDE, listX1 = sideX2 + 1, listX2 = listX1 + SB_LIST;
        g.fill(sideX2, contentY, sideX2 + 1, y2, skin.borderIdle);
        g.fill(listX2, contentY, listX2 + 1, y2, skin.borderIdle);

        // ── left: category tabs ──
        int cy = contentY + 2;
        for (int i = 0; i < panels.size(); i++) {
            Cat c = panels.get(i).cat;
            boolean sel = i == sbCatIdx;
            boolean hov = mx >= x && mx < sideX2 && my >= cy && my < cy + SB_CAT_H;
            if (sel)      g.fill(x, cy, sideX2, cy + SB_CAT_H, aTint | 0x50000000);
            else if (hov) g.fill(x, cy, sideX2, cy + SB_CAT_H, skin.rowHover);
            if (sel)      g.fill(x, cy, x + 2, cy + SB_CAT_H, accent);
            g.drawString(font, c.label, x + 6, cy + (SB_CAT_H - font.lineHeight) / 2, sel ? skin.textOn : skin.textDim, skin.nameShadow);
            cy += SB_CAT_H;
        }

        // ── middle: module list for the selected category ──
        Panel cat = panels.get(sbCatIdx);
        int listTop = contentY + 2, listBot = y2 - 2, listVis = listBot - listTop;
        int totalList = cat.mods.size() * SB_ROW_H;
        int maxScrollM = Math.max(0, totalList - listVis);
        sbScrollMods = Math.max(0, Math.min(sbScrollMods, maxScrollM));
        int ry = listTop - sbScrollMods;
        for (ModEntry m : cat.mods) {
            if (ry + SB_ROW_H > listTop && ry < listBot) {
                boolean on = m.enabled.getAsBoolean();
                boolean sel = m == sbExpanded;
                boolean hov = mx >= listX1 && mx < listX2 && my >= Math.max(ry, listTop) && my < Math.min(ry + SB_ROW_H, listBot);
                if (sel)      g.fill(listX1, ry, listX2, ry + SB_ROW_H, aTint | 0x40000000);
                else if (hov) g.fill(listX1, ry, listX2, ry + SB_ROW_H, skin.rowHover);
                int dotY = ry + (SB_ROW_H - 6) / 2;
                g.fill(listX1 + 4, dotY, listX1 + 10, dotY + 6, on ? 0xFF55FF55 : 0xFF606060);
                int tc = m.toggleable ? (on ? skin.textOn : skin.textDim) : skin.textOff;
                g.drawString(font, m.name, listX1 + 14, ry + (SB_ROW_H - font.lineHeight) / 2, tc, skin.nameShadow);
                if (!m.settings.isEmpty())
                    g.drawString(font, "›", listX2 - 8, ry + (SB_ROW_H - font.lineHeight) / 2, skin.textDim, false);
            }
            ry += SB_ROW_H;
        }
        if (totalList > listVis) {
            int barH = Math.max(8, listVis * listVis / totalList);
            int barY = listTop + (int)((listVis - barH) * ((float) sbScrollMods / maxScrollM));
            g.fill(listX2 - 2, barY, listX2, barY + barH, skin.scrollBar);
        }

        // ── right: settings for the selected module (reuses Raven widgets) ──
        int setX = listX2 + 5, setRight = x2 - 5, setW = setRight - setX;
        int setTop = contentY + 4, setBot = y2 - 3;
        if (sbExpanded == null) {
            g.drawString(font, "Pick a module ‹", listX2 + 6, contentY + 6, skin.textOff, false);
        } else {
            ModEntry m = sbExpanded;
            boolean on = m.enabled.getAsBoolean();
            g.drawString(font, m.name, setX, setTop, on ? skin.textOn : skin.textDim, skin.nameShadow);
            if (m.toggleable) {
                String st = on ? "ON" : "OFF";
                g.drawString(font, st, setRight - font.width(st), setTop, on ? 0xFF55FF55 : 0xFFFF5555, false);
            }
            int sListTop = setTop + 12;
            int totalSet = totalSettingsH(m), setVis = setBot - sListTop;
            int maxScrollS = Math.max(0, totalSet - setVis);
            sbScrollSett = Math.max(0, Math.min(sbScrollSett, maxScrollS));
            int dy = sListTop - sbScrollSett;
            for (Setting s : m.settings) {
                int sh = settH(s);
                if (dy + sh > sListTop && dy < setBot) {
                    if (s instanceof SliderS sl)    drawSlider(g, sl, setX, dy, setW, font, accent);
                    else if (s instanceof BoolS bs) drawBool(g, bs, setX, dy, font, accent);
                    else if (s instanceof KeyS ks)  drawBind(g, ks, setX, dy, font, rebindTarget == ks);
                }
                dy += sh;
            }
            if (totalSet > setVis) {
                int barH = Math.max(8, setVis * setVis / totalSet);
                int barY = sListTop + (int)((setVis - barH) * ((float) sbScrollSett / maxScrollS));
                g.fill(x2 - 2, barY, x2, barY + barH, skin.scrollBar);
            }
        }

        boolean hoverWin = mx >= x && mx < x2 && my >= y && my < y2;
        drawBorder(g, x, y, x2, y2, hoverWin ? (aTint | 0x80000000) : skin.borderIdle);
    }

    private boolean sbMouseClicked(int mx, int my, int btn) {
        ensureSidebarPos();
        int x = sbWinX, y = sbWinY, x2 = x + SB_W, y2 = y + SB_H;
        if (mx < x || mx >= x2 || my < y || my >= y2) return false;

        if (my < y + SB_TITLE) {                       // title bar
            if (btn == 0 && mx >= x2 - 14) { onClose(); return true; }
            if (btn == 0) { sbDragging = true; sbDragOX = x - mx; sbDragOY = y - my; }
            return true;
        }

        int contentY = y + SB_TITLE;
        int sideX2 = x + SB_SIDE, listX1 = sideX2 + 1, listX2 = listX1 + SB_LIST;

        if (mx < sideX2) {                             // category tabs
            int cy = contentY + 2;
            for (int i = 0; i < panels.size(); i++) {
                if (my >= cy && my < cy + SB_CAT_H) { sbCatIdx = i; sbExpanded = null; sbScrollMods = 0; sbScrollSett = 0; break; }
                cy += SB_CAT_H;
            }
            return true;
        }

        if (mx < listX2) {                             // module list
            int listTop = contentY + 2, listBot = y2 - 2;
            int ry = listTop - sbScrollMods;
            for (ModEntry m : panels.get(sbCatIdx).mods) {
                if (my >= Math.max(ry, listTop) && my < Math.min(ry + SB_ROW_H, listBot)) {
                    if (mx < listX1 + 12) { if (m.toggleable) m.toggle.run(); }   // dot → toggle
                    else { sbExpanded = (sbExpanded == m) ? null : m; sbScrollSett = 0; }  // row → select
                    break;
                }
                ry += SB_ROW_H;
            }
            return true;
        }

        if (sbExpanded != null) {                      // settings pane
            ModEntry m = sbExpanded;
            int setX = listX2 + 5, setRight = x2 - 5, setW = setRight - setX;
            int setTop = contentY + 4, setBot = y2 - 3;
            if (my >= setTop && my < setTop + 10) {
                if (btn == 0 && m.toggleable && mx >= setRight - 24) m.toggle.run();
                return true;
            }
            int sListTop = setTop + 12;
            int dy = sListTop - sbScrollSett;
            for (Setting s : m.settings) {
                int sh = settH(s);
                if (my >= Math.max(dy, sListTop) && my < Math.min(dy + sh, setBot)) {
                    if (btn == 0) sbHandleSettClick(s, mx, setX, setW);
                    break;
                }
                dy += sh;
            }
        }
        return true;
    }

    private void sbHandleSettClick(Setting s, int mx, int setX, int setW) {
        if (s instanceof SliderS sl) { sbSliderActive = sl; sbSliderX = setX; sbSliderW = setW; applySlider(sl, mx, setX, setW); }
        else if (s instanceof BoolS bs) bs.toggle().run();
        else if (s instanceof KeyS ks) rebindTarget = ks;
    }

    private boolean sbMouseScrolled(int mx, int my, double scrollY) {
        ensureSidebarPos();
        int x = sbWinX, y = sbWinY, x2 = x + SB_W, y2 = y + SB_H;
        if (mx < x || mx >= x2 || my < y || my >= y2) return false;
        int sideX2 = x + SB_SIDE, listX1 = sideX2 + 1, listX2 = listX1 + SB_LIST;
        if (mx >= listX1 && mx < listX2) { sbScrollMods -= (int)(scrollY * 8); return true; }
        if (mx >= listX2 && sbExpanded != null) { sbScrollSett -= (int)(scrollY * 8); return true; }
        return true;
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  TENACITY LAYOUT (guiLayout == 2) — single rounded window: a category rail
    //  on the left, rounded module cards with pill toggles + inline accordion
    //  settings on the right. Custom widgets (thumb slider, sliding pill toggle,
    //  key chip) with a knob-slide animation. Geometry/hit-testing are fully
    //  self-contained; colours come from the active GuiSkin so it pairs with any
    //  skin (best with Dark / Tenacity).
    // ═════════════════════════════════════════════════════════════════════════
    private static final int TEN_W = 344, TEN_H = 234;
    private static final int TEN_TITLE = 24;
    private static final int TEN_RAIL  = 84;
    private static final int TEN_CAT_H = 22;
    private static final int TEN_CARD_H = 22;
    private static final int TEN_SLIDER = 22, TEN_BOOL = 16, TEN_BIND = 16;

    private void ensureTenPos() {
        if (tnWinX < 0) { tnWinX = (width - TEN_W) / 2; tnWinY = (height - TEN_H) / 2; }
    }

    private void renderTenacity(GuiGraphics g, int mx, int my, int accent, Font font) {
        ensureTenPos();
        int x = tnWinX, y = tnWinY, x2 = x + TEN_W, y2 = y + TEN_H;
        int aT = accent & 0x00FFFFFF;

        roundRect(g, x, y, x2, y2, skin.panelBg);

        // ── title bar ──
        g.drawString(font, "Valencia", x + 10, y + (TEN_TITLE - font.lineHeight) / 2, skin.catLabel, skin.nameShadow);
        boolean hovClose = mx >= x2 - 16 && mx < x2 && my >= y && my < y + TEN_TITLE;
        g.drawString(font, "x", x2 - 12, y + (TEN_TITLE - font.lineHeight) / 2, hovClose ? 0xFFFF5555 : skin.textDim, false);
        g.fill(x + 10, y + TEN_TITLE - 1, x2 - 10, y + TEN_TITLE, accent);

        int contentY = y + TEN_TITLE;
        int railX2 = x + TEN_RAIL;
        g.fill(railX2, contentY + 2, railX2 + 1, y2 - 4, skin.borderIdle);   // rail divider

        // ── left rail: category tabs ──
        int cy = contentY + 6;
        for (int i = 0; i < panels.size(); i++) {
            Cat c = panels.get(i).cat;
            boolean sel = i == tnCatIdx;
            boolean hov = mx >= x + 6 && mx < railX2 - 4 && my >= cy && my < cy + TEN_CAT_H;
            if (sel)      roundRect(g, x + 6, cy, railX2 - 4, cy + TEN_CAT_H, aT | 0x40000000);
            else if (hov) roundRect(g, x + 6, cy, railX2 - 4, cy + TEN_CAT_H, skin.rowHover);
            if (sel) g.fill(x + 6, cy + 4, x + 8, cy + TEN_CAT_H - 4, accent);
            g.drawString(font, c.label, x + 14, cy + (TEN_CAT_H - font.lineHeight) / 2, sel ? skin.textOn : skin.textDim, skin.nameShadow);
            cy += TEN_CAT_H + 2;
        }

        // ── right: module cards (scrollable, clipped) ──
        int listX1 = railX2 + 6, listX2 = x2 - 6, listW = listX2 - listX1;
        int listTop = contentY + 6, listBot = y2 - 6, listVis = listBot - listTop;
        int total = tnContentH(panels.get(tnCatIdx));
        int maxScroll = Math.max(0, total - listVis);
        tnScroll = Math.max(0, Math.min(tnScroll, maxScroll));

        g.enableScissor(listX1, listTop, listX2, listBot);
        int ry = listTop - tnScroll;
        for (ModEntry m : panels.get(tnCatIdx).mods) {
            int ch = TEN_CARD_H + (m == tnExpanded ? tnExpandedH(m) : 0);
            if (ry + ch > listTop && ry < listBot) tnDrawCard(g, m, listX1, ry, listW, mx, my, font, accent);
            ry += ch + 4;
        }
        g.disableScissor();

        if (total > listVis) {
            int barH = Math.max(10, listVis * listVis / total);
            int barY = listTop + (int)((listVis - barH) * ((float) tnScroll / maxScroll));
            roundRect(g, listX2 - 2, barY, listX2, barY + barH, skin.scrollBar);
        }

        roundBorder(g, x, y, x2, y2, skin.borderIdle);
    }

    private void tnDrawCard(GuiGraphics g, ModEntry m, int x, int y, int w, int mx, int my, Font font, int accent) {
        int ch = TEN_CARD_H + (m == tnExpanded ? tnExpandedH(m) : 0);
        boolean on = m.enabled.getAsBoolean();
        boolean hovRow = mx >= x && mx < x + w && my >= y && my < y + TEN_CARD_H;

        roundRect(g, x, y, x + w, y + ch, hovRow ? skin.headerHover : skin.expandedOffBg);
        if (m == tnExpanded) g.fill(x + 1, y + TEN_CARD_H, x + w - 1, y + ch - 1, skin.settingsBg);
        if (on) g.fill(x, y + 3, x + 2, y + TEN_CARD_H - 3, accent);

        g.drawString(font, m.name, x + 8, y + (TEN_CARD_H - font.lineHeight) / 2, on ? skin.textOn : skin.textDim, skin.nameShadow);

        int pillW = 18, pillX = x + w - 8 - pillW, pillCy = y + TEN_CARD_H / 2;
        if (m.toggleable) tnPill(g, pillX, pillCy, on, tnKnobProg(m.name, on), accent);
        if (!m.settings.isEmpty()) {
            String chev = m == tnExpanded ? "-" : "+";
            int chx = m.toggleable ? pillX - 10 : x + w - 12;
            g.drawString(font, chev, chx, y + (TEN_CARD_H - font.lineHeight) / 2, skin.textDim, false);
        }

        if (m == tnExpanded) {
            int sy = y + TEN_CARD_H + 2, sx = x + 12, sw = w - 22;
            for (Setting s : m.settings) {
                if (s instanceof SliderS sl)    tnSlider(g, sl, sx, sy + 2, sw, font, accent);
                else if (s instanceof BoolS bs) tnBool(g, bs, sx, sy, sw, font, accent);
                else if (s instanceof KeyS ks)  tnBind(g, ks, sx, sy + 2, sw, font, rebindTarget == ks);
                sy += tenSettH(s);
            }
        }
    }

    private void tnPill(GuiGraphics g, int x, int cy, boolean on, float prog, int accent) {
        int w = 18, h = 10, y = cy - h / 2;
        roundRect(g, x, y, x + w, y + h, on ? (accent & 0x00FFFFFF) | 0xCC000000 : skin.boolTrack);
        int kd = h - 4, kx = x + 2 + (int)((w - 4 - kd) * prog);
        roundRect(g, kx, y + 2, kx + kd, y + 2 + kd, 0xFFFFFFFF);
    }

    private void tnSlider(GuiGraphics g, SliderS sl, int x, int y, int w, Font font, int accent) {
        double val = sl.get().getAsDouble();
        g.drawString(font, sl.label(), x, y, skin.textDim, skin.nameShadow);
        String vs = fmtVal(val);
        g.drawString(font, vs, x + w - font.width(vs), y, skin.textOn, skin.nameShadow);
        int barY = y + 12, barH = 4;
        double pct = Math.max(0, Math.min(1, (val - sl.min()) / (sl.max() - sl.min())));
        int filled = (int)(w * pct);
        roundRect(g, x, barY, x + w, barY + barH, skin.sliderTrack);
        if (filled > 0) roundRect(g, x, barY, x + filled, barY + barH, (accent & 0x00FFFFFF) | 0xFF000000);
        int tx = x + filled, tr = 4, tcy = barY + barH / 2;
        roundRect(g, tx - tr, tcy - tr, tx + tr, tcy + tr, 0xFFFFFFFF);
    }

    private void tnBool(GuiGraphics g, BoolS bs, int x, int y, int w, Font font, int accent) {
        boolean on = bs.get().getAsBoolean();
        g.drawString(font, bs.label(), x, y + (TEN_BOOL - font.lineHeight) / 2, skin.textDim, skin.nameShadow);
        int pillW = 18;
        tnPill(g, x + w - pillW, y + TEN_BOOL / 2, on, tnKnobProg(m_key(bs), on), accent);
    }

    private void tnBind(GuiGraphics g, KeyS ks, int x, int y, int w, Font font, boolean binding) {
        g.drawString(font, "Key", x, y + (TEN_BIND - font.lineHeight) / 2, skin.textDim, skin.nameShadow);
        String v = binding ? "..." : ModConfig.keyName(ks.get().getAsInt());
        int cw = font.width(v) + 8, cx = x + w - cw, ccy = y + TEN_BIND / 2;
        roundRect(g, cx, ccy - 6, x + w, ccy + 6, skin.sliderTrack);
        g.drawString(font, v, cx + 4, ccy - font.lineHeight / 2, binding ? 0xFFFFD050 : skin.textOn, false);
    }

    private String m_key(BoolS bs) { return (tnExpanded != null ? tnExpanded.name : "") + ":" + bs.label(); }

    private float tnKnobProg(String key, boolean on) {
        float target = on ? 1f : 0f;
        float cur = tnKnob.getOrDefault(key, target);
        cur += (target - cur) * 0.3f;
        if (Math.abs(target - cur) < 0.01f) cur = target;
        tnKnob.put(key, cur);
        return cur;
    }

    private int tenSettH(Setting s) {
        if (s instanceof SliderS) return TEN_SLIDER;
        if (s instanceof BoolS)   return TEN_BOOL;
        if (s instanceof KeyS)    return TEN_BIND;
        return 0;
    }

    private int tnExpandedH(ModEntry m) {
        int h = 4;
        for (Setting s : m.settings) h += tenSettH(s);
        return h;
    }

    private int tnContentH(Panel p) {
        int h = 0;
        for (ModEntry m : p.mods) h += TEN_CARD_H + (m == tnExpanded ? tnExpandedH(m) : 0) + 4;
        return h;
    }

    // ── Tenacity rounded-rect helpers (2px corner shave) ──
    private void roundRect(GuiGraphics g, int x1, int y1, int x2, int y2, int c) {
        if (x2 - x1 < 4 || y2 - y1 < 4) { g.fill(x1, y1, x2, y2, c); return; }
        g.fill(x1 + 2, y1,     x2 - 2, y1 + 1, c);
        g.fill(x1 + 1, y1 + 1, x2 - 1, y1 + 2, c);
        g.fill(x1,     y1 + 2, x2,     y2 - 2, c);
        g.fill(x1 + 1, y2 - 2, x2 - 1, y2 - 1, c);
        g.fill(x1 + 2, y2 - 1, x2 - 2, y2,     c);
    }

    private void roundBorder(GuiGraphics g, int x1, int y1, int x2, int y2, int c) {
        g.fill(x1 + 2, y1,     x2 - 2, y1 + 1, c);
        g.fill(x1 + 2, y2 - 1, x2 - 2, y2,     c);
        g.fill(x1,     y1 + 2, x1 + 1, y2 - 2, c);
        g.fill(x2 - 1, y1 + 2, x2,     y2 - 2, c);
        g.fill(x1 + 1, y1 + 1, x1 + 2, y1 + 2, c);
        g.fill(x2 - 2, y1 + 1, x2 - 1, y1 + 2, c);
        g.fill(x1 + 1, y2 - 2, x1 + 2, y2 - 1, c);
        g.fill(x2 - 2, y2 - 2, x2 - 1, y2 - 1, c);
    }

    private boolean tnMouseClicked(int mx, int my, int btn) {
        ensureTenPos();
        int x = tnWinX, y = tnWinY, x2 = x + TEN_W, y2 = y + TEN_H;
        if (mx < x || mx >= x2 || my < y || my >= y2) return false;

        if (my < y + TEN_TITLE) {                       // title bar
            if (btn == 0 && mx >= x2 - 16) { onClose(); return true; }
            if (btn == 0) { tnDragging = true; tnDragOX = x - mx; tnDragOY = y - my; }
            return true;
        }

        int contentY = y + TEN_TITLE, railX2 = x + TEN_RAIL;
        if (mx < railX2) {                              // rail tabs
            int cy = contentY + 6;
            for (int i = 0; i < panels.size(); i++) {
                if (my >= cy && my < cy + TEN_CAT_H) { tnCatIdx = i; tnExpanded = null; tnScroll = 0; break; }
                cy += TEN_CAT_H + 2;
            }
            return true;
        }

        int listX1 = railX2 + 6, listX2 = x2 - 6, listW = listX2 - listX1;
        int listTop = contentY + 6, listBot = y2 - 6;
        if (mx < listX1 || mx >= listX2 || my < listTop || my >= listBot) return true;

        int ry = listTop - tnScroll;
        for (ModEntry m : panels.get(tnCatIdx).mods) {
            int ch = TEN_CARD_H + (m == tnExpanded ? tnExpandedH(m) : 0);
            if (my >= ry && my < ry + TEN_CARD_H) {     // header row
                int pillW = 18, pillX = listX1 + listW - 8 - pillW;
                if (btn == 0 && m.toggleable && mx >= pillX - 2 && mx < pillX + pillW + 2) m.toggle.run();
                else if (!m.settings.isEmpty()) tnExpanded = (m == tnExpanded) ? null : m;
                else if (btn == 0 && m.toggleable) m.toggle.run();
                return true;
            }
            if (m == tnExpanded && my >= ry + TEN_CARD_H && my < ry + ch) {   // settings
                int sy = ry + TEN_CARD_H + 2, sx = listX1 + 12, sw = listW - 22;
                for (Setting s : m.settings) {
                    int sh = tenSettH(s);
                    if (my >= sy && my < sy + sh) { if (btn == 0) tnSettClick(s, mx, sx, sw); break; }
                    sy += sh;
                }
                return true;
            }
            ry += ch + 4;
        }
        return true;
    }

    private void tnSettClick(Setting s, int mx, int sx, int sw) {
        if (s instanceof SliderS sl) { tnSliderActive = sl; tnSliderX = sx; tnSliderW = sw; applySlider(sl, mx, sx, sw); }
        else if (s instanceof BoolS bs) bs.toggle().run();
        else if (s instanceof KeyS ks) rebindTarget = ks;
    }

    private boolean tnMouseScrolled(int mx, int my, double scrollY) {
        ensureTenPos();
        int x = tnWinX, y = tnWinY, x2 = x + TEN_W, y2 = y + TEN_H;
        if (mx < x || mx >= x2 || my < y || my >= y2) return false;
        if (mx >= x + TEN_RAIL) {
            int listVis = (y2 - 6) - (y + TEN_TITLE + 6);
            int maxScroll = Math.max(0, tnContentH(panels.get(tnCatIdx)) - listVis);
            tnScroll = Math.max(0, Math.min(tnScroll - (int)(scrollY * 12), maxScroll));
        }
        return true;
    }

    // ── Waifu ───────────────────────────────────────────────────────────────
    private static final String[] WAIFU_EXTS = {"png", "jpg", "jpeg", "bmp", "gif"};

    private void loadWaifu() {
        File dir = FabricLoader.getInstance().getConfigDir().resolve("valencia").toFile();
        waifuHint = dir.getAbsolutePath();
        try {
            if (!dir.exists() || !dir.isDirectory()) { waifuErr = "config dir missing"; return; }
            File f = null;
            for (String ext : WAIFU_EXTS) { File c = new File(dir, "waifu." + ext); if (c.exists()) { f = c; break; } }
            if (f == null) { waifuErr = "no waifu.{png,jpg,bmp,gif}"; return; }
            InputStream pngStream;
            if (f.getName().toLowerCase().endsWith(".png")) {
                pngStream = new FileInputStream(f);
            } else {
                BufferedImage img = ImageIO.read(f);
                if (img == null) { waifuErr = "decode failed"; return; }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "png", baos);
                pngStream = new ByteArrayInputStream(baos.toByteArray());
            }
            NativeImage ni = NativeImage.read(pngStream);
            waifuTexW = ni.getWidth(); waifuTexH = ni.getHeight();
            DynamicTexture dTex = new DynamicTexture(() -> "valencia-waifu", ni);
            Identifier loc = Identifier.fromNamespaceAndPath("valencia", "waifu");
            Minecraft.getInstance().getTextureManager().register(loc, dTex);
            waifuLoc = loc; waifuErr = null;
        } catch (Throwable t) { waifuErr = t.getClass().getSimpleName() + ": " + t.getMessage(); }
    }

    private void renderWaifu(GuiGraphics g) {
        Font font = Minecraft.getInstance().font;
        if (waifuLoc == null || waifuTexW <= 0 || waifuTexH <= 0) {
            String hint = waifuErr != null
                ? "§c[waifu] " + waifuErr
                : "§8[waifu: " + (waifuHint != null ? waifuHint : "config/valencia") + File.separator + "waifu.png]";
            g.drawString(font, hint, 4, height - 22, 0xFF888888, false);
            return;
        }
        try {
            int dispH = Math.min(height / 3, 150);
            int dispW = waifuTexW * dispH / waifuTexH;
            int x1 = 4, y1 = height - dispH - 24;
            g.blit(waifuLoc, x1, y1, x1 + dispW, y1 + dispH, 0f, 1f, 0f, 1f);
        } catch (Throwable t) {
            g.drawString(font, "§c[waifu err]", 4, height - 22, 0xFF888888, false);
        }
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  INPUT
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean dbl) {
        int mx = (int)event.x(), my = (int)event.y(), btn = event.button();

        if (ModConfig.get().guiLayout == 2) return tnMouseClicked(mx, my, btn);
        if (ModConfig.get().guiLayout == 1) return sbMouseClicked(mx, my, btn);

        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel p = panels.get(i);
            int ph = panelH(p);
            if (mx < p.x || mx >= p.x + PANEL_W || my < p.y || my >= p.y + ph) continue;

            // bring to front
            panels.remove(i);
            panels.add(p);

            // ── header click ────────────────────────────────────────────────
            if (my < p.y + HDR) {
                int symX = p.x + PANEL_W - 12;
                if (btn == 0 && mx >= symX) {
                    // +/- toggle
                    p.open = !p.open;
                    p.expanded = null;
                    p.scrollOff = 0;
                } else if (btn == 0) {
                    // drag start (Raven: drag by header name area)
                    p.dragging = true;
                    p.dragOX = p.x - mx;
                    p.dragOY = p.y - my;
                }
                return true;
            }

            if (!p.open) return true;

            // ── expanded settings view ──────────────────────────────────────
            if (p.expanded != null) {
                int yo = p.y + HDR;
                // module name row — left=toggle, right=collapse
                if (my >= yo && my < yo + MOD_H) {
                    if (btn == 0 && p.expanded.toggleable) p.expanded.toggle.run();
                    if (btn == 1) { p.expanded = null; p.scrollOff = 0; }
                    return true;
                }
                yo += MOD_H;

                // settings (with scroll offset)
                int settStart = yo;
                int drawY = settStart - p.scrollOff;
                for (int si = 0; si < p.expanded.settings.size(); si++) {
                    Setting s = p.expanded.settings.get(si);
                    int sh = settH(s);
                    int realY = drawY;

                    // hit test against visible area only
                    if (my >= Math.max(realY, settStart) && my < Math.min(realY + sh, settStart + MAX_SET_H)) {
                        if (btn == 0) handleSettClick(p, si, s, mx, realY);
                        return true;
                    }
                    drawY += sh;
                }
                return true;
            }

            // ── module list (Raven: left=toggle, right=expand settings) ─────
            int yo = p.y + HDR;
            for (ModEntry m : p.mods) {
                if (my >= yo && my < yo + MOD_H) {
                    if (btn == 0 && m.toggleable) m.toggle.run();
                    if (btn == 1 && !m.settings.isEmpty()) {
                        p.expanded = (p.expanded == m) ? null : m;
                        p.scrollOff = 0;
                    }
                    return true;
                }
                yo += MOD_H;
            }
            return true;
        }
        return super.mouseClicked(event, dbl);
    }

    private void handleSettClick(Panel p, int idx, Setting s, int mx, int settingY) {
        if (s instanceof SliderS sl) {
            sliderPanel = p; sliderIdx = idx;
            applySlider(sl, mx, p.x + 4, PANEL_W - 8);
        } else if (s instanceof BoolS bs) {
            bs.toggle().run();
        } else if (s instanceof KeyS ks) {
            rebindTarget = ks;
        }
    }

    private void applySlider(SliderS sl, int mx, int barX, int barW) {
        double pct = Math.max(0, Math.min(1, (double)(mx - barX) / barW));
        double val = sl.min() + pct * (sl.max() - sl.min());
        sl.set().accept(val);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        int mx = (int)event.x(), my = (int)event.y();

        if (ModConfig.get().guiLayout == 2) {
            if (tnDragging) { tnWinX = tnDragOX + mx; tnWinY = tnDragOY + my; return true; }
            if (tnSliderActive != null) { applySlider(tnSliderActive, mx, tnSliderX, tnSliderW); return true; }
            return super.mouseDragged(event, dx, dy);
        }

        if (ModConfig.get().guiLayout == 1) {
            if (sbDragging) { sbWinX = sbDragOX + mx; sbWinY = sbDragOY + my; return true; }
            if (sbSliderActive != null) { applySlider(sbSliderActive, mx, sbSliderX, sbSliderW); return true; }
            return super.mouseDragged(event, dx, dy);
        }

        // panel drag
        for (Panel p : panels) {
            if (p.dragging) {
                p.x = p.dragOX + mx;
                p.y = p.dragOY + my;
                return true;
            }
        }

        // slider drag
        if (sliderPanel != null && sliderPanel.expanded != null && sliderIdx >= 0) {
            Setting s = sliderPanel.expanded.settings.get(sliderIdx);
            if (s instanceof SliderS sl) applySlider(sl, mx, sliderPanel.x + 4, PANEL_W - 8);
            return true;
        }

        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        for (Panel p : panels) p.dragging = false;
        sliderPanel = null; sliderIdx = -1;
        sbDragging = false; sbSliderActive = null;
        tnDragging = false; tnSliderActive = null;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int mx = (int)mouseX, my = (int)mouseY;

        if (ModConfig.get().guiLayout == 2) return tnMouseScrolled(mx, my, scrollY);
        if (ModConfig.get().guiLayout == 1) return sbMouseScrolled(mx, my, scrollY);

        // find which panel the mouse is over and scroll its settings
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel p = panels.get(i);
            int ph = panelH(p);
            if (mx >= p.x && mx < p.x + PANEL_W && my >= p.y && my < p.y + ph) {
                if (p.open && p.expanded != null) {
                    int totalS = totalSettingsH(p.expanded);
                    if (totalS > MAX_SET_H) {
                        p.scrollOff -= (int)(scrollY * 6);
                        int maxScroll = totalS - MAX_SET_H;
                        if (p.scrollOff < 0) p.scrollOff = 0;
                        if (p.scrollOff > maxScroll) p.scrollOff = maxScroll;
                    }
                }
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (rebindTarget != null) {
            if (key != GLFW.GLFW_KEY_ESCAPE) rebindTarget.set().accept(key);
            rebindTarget = null;
            return true;
        }
        if (key == ModConfig.get().guiKey) { onClose(); return true; }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        saveEnabled();
        super.onClose();
    }

    private void saveEnabled() {
        ModConfig cfg = ModConfig.get();
        cfg.nofallEnabled    = NoFallMod.isEnabled();
        cfg.xrayEnabled      = XRayMod.isEnabled();
        cfg.maceAuraEnabled  = MaceAuraMod.isEnabled();
        cfg.noSlowEnabled    = NoSlowMod.isEnabled();
        cfg.bhopEnabled      = BHopMod.isEnabled();
        cfg.stepEnabled      = StepMod.isEnabled();
        cfg.killAuraEnabled  = KillAuraMod.isEnabled();
        cfg.velocityEnabled  = VelocityMod.isEnabled();
        cfg.fastPlaceEnabled = FastPlaceMod.isEnabled();
        cfg.critEnabled      = CritMod.isEnabled();
        cfg.scaffoldEnabled  = ScaffoldMod.isEnabled();
        cfg.timerEnabled     = TimerMod.isEnabled();
        cfg.spearAuraEnabled = SpearAuraMod.isEnabled();
        cfg.noCrashEnabled   = NoCrashMod.isEnabled();
        cfg.hitboxEnabled    = HitboxMod.isEnabled();
        cfg.espEnabled       = ESPMod.isEnabled();
        cfg.nameTagEnabled   = NameTagMod.isEnabled();
        cfg.netherCoordEnabled = NetherCoordMod.isEnabled();
        cfg.targetHudEnabled = TargetHudMod.isEnabled();
        cfg.autoFishEnabled  = AutoFishMod.isEnabled();
        cfg.autoTotemEnabled = AutoTotemMod.isEnabled();
        cfg.arrayListEnabled = ArrayListMod.isEnabled();
        cfg.flyEnabled       = FlyMod.isEnabled();
        cfg.save();
    }
}
