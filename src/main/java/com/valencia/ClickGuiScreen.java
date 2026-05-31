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
    private Panel  rebindPanel;
    private int    rebindIdx  = -1;
    private long   openTime;
    private GuiSkin skin;   // resolved each frame from cfg.guiStyle (live switch)

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

        add(Cat.MOVEMENT, new ModEntry("NoFall", NoFallMod::isEnabled, NoFallMod::toggleManual, true,
            List.of(new KeyS("Key", () -> cfg.nofallKey, v -> { cfg.nofallKey = v; cfg.save(); }))));
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

        add(Cat.RENDER, new ModEntry("TargetHUD",
            TargetHudMod::isEnabled, () -> { TargetHudMod.toggle(); cfg.targetHudEnabled = TargetHudMod.isEnabled(); cfg.save(); },
            true, List.of(
            new SliderS("Style", () -> cfg.targetHudStyle, v -> { cfg.targetHudStyle = (int)v; cfg.save(); }, 0, 2)
        )));

        add(Cat.RENDER, new ModEntry("ArrayList",
            ArrayListMod::isEnabled, () -> { ArrayListMod.toggle(); cfg.arrayListEnabled = ArrayListMod.isEnabled(); cfg.save(); }, true, List.of(
            new BoolS("Rainbow", () -> cfg.arrayListRainbow,    () -> { cfg.arrayListRainbow = !cfg.arrayListRainbow;       ArrayListMod.rainbow = cfg.arrayListRainbow;       cfg.save(); }),
            new BoolS("BG",      () -> cfg.arrayListBackground, () -> { cfg.arrayListBackground = !cfg.arrayListBackground; ArrayListMod.background = cfg.arrayListBackground; cfg.save(); })
        )));

        // ── Client ──────────────────────────────────────────────────────────
        add(Cat.CLIENT, new ModEntry("Theme", () -> false, () -> {}, false, List.of(
            new SliderS("GUI Style", () -> cfg.guiStyle, v -> { cfg.guiStyle = (int)v; cfg.save(); }, 0, 2),
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

        // panels
        for (Panel p : panels) renderPanel(g, p, mx, my, accent, font);
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
                    boolean binding = (rebindPanel == p && rebindIdx == i);
                    drawBind(g, ks, sx, drawY, font, binding);
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
        } else if (s instanceof KeyS) {
            rebindPanel = p; rebindIdx = idx;
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
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int mx = (int)mouseX, my = (int)mouseY;

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
        if (rebindPanel != null && rebindIdx >= 0) {
            if (key != GLFW.GLFW_KEY_ESCAPE && rebindPanel.expanded != null) {
                Setting s = rebindPanel.expanded.settings.get(rebindIdx);
                if (s instanceof KeyS ks) ks.set().accept(key);
            }
            rebindPanel = null; rebindIdx = -1;
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
        cfg.save();
    }
}
