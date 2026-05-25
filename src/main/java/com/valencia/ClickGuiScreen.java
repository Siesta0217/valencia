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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class ClickGuiScreen extends Screen {

    // ── Categories ───────────────────────────────────────────────────────────
    private enum Category {
        COMBAT("Combat"),
        MOVEMENT("Movement"),
        VISUALS("Visuals"),
        SETTINGS("Settings");
        final String label;
        Category(String l) { label = l; }
    }

    // ── Setting types ────────────────────────────────────────────────────────
    interface Setting {}
    record SliderS(String label, DoubleSupplier get, DoubleConsumer set, double min, double max) implements Setting {}
    record BoolS(String label, BooleanSupplier get, Runnable toggle) implements Setting {}
    record KeyS(String label, IntSupplier get, IntConsumer set) implements Setting {}

    record ModEntry(
        String name, Category cat,
        BooleanSupplier enabled, Runnable toggle,
        boolean toggleable,
        List<Setting> settings
    ) {}

    // ── Discord-inspired palette ─────────────────────────────────────────────
    private static final int C_OUTER    = 0xFF1E1F22;
    private static final int C_SIDEBAR  = 0xFF1E1F22;
    private static final int C_LIST     = 0xFF2B2D31;
    private static final int C_PANEL    = 0xFF313338;
    private static final int C_DIV      = 0xFF1A1B1E;
    private static final int C_HOVER    = 0xFF35373C;
    private static final int C_SEL      = 0xFF404249;
    private static final int C_GREEN    = 0xFF57F287;
    private static final int C_RED      = 0xFFED4245;
    private static final int C_TEXT     = 0xFFDBDEE1;
    private static final int C_DIM      = 0xFF949BA4;
    private static final int C_MUTE     = 0xFF6D6F78;
    private static final int C_HASH     = 0xFF80848E;

    // ── Layout constants ─────────────────────────────────────────────────────
    private static final int WIN_W     = 440;
    private static final int WIN_H     = 310;
    private static final int SIDE_W    = 44;
    private static final int LIST_W    = 130;
    private static final int HDR_H     = 28;
    private static final int CAT_SZ    = 32;
    private static final int CAT_GAP   = 5;
    private static final int MOD_H     = 20;
    private static final int SET_H     = 16;
    private static final int PAD       = 6;

    // ── GUI state ────────────────────────────────────────────────────────────
    private final List<ModEntry> mods = new ArrayList<>();
    private Category selCat  = Category.COMBAT;
    private String   selMod  = null;
    private int modScroll    = 0;
    private int setScroll    = 0;
    private String rebindMod = null;
    private int    rebindIdx = -1;
    private String dragMod   = null;
    private int    dragIdx   = -1;

    // window drag
    private int  wx, wy;
    private boolean wDrag = false;
    private int  wdx, wdy;

    // ── Waifu ────────────────────────────────────────────────────────────────
    private Identifier waifuLoc = null;
    private int        waifuTexW, waifuTexH;
    private String     waifuHint, waifuErr;

    // ─────────────────────────────────────────────────────────────────────────

    public ClickGuiScreen() {
        super(Component.empty());
        buildMods();
        loadWaifu();
    }

    @Override public boolean isPauseScreen() { return false; }

    // ── Module list (kept verbatim) ──────────────────────────────────────────
    private void buildMods() {
        ModConfig cfg = ModConfig.get();

        // Combat
        mods.add(new ModEntry("KillAura", Category.COMBAT,
            KillAuraMod::isEnabled, KillAuraMod::toggle, true,
            List.of(
                new SliderS("Range",     () -> cfg.killRange,        v -> { cfg.killRange        = (float)v; KillAuraMod.RANGE        = (float)v; cfg.save(); }, 1, 10),
                new SliderS("Atk Range", () -> cfg.killAttackRange,  v -> { cfg.killAttackRange  = (float)v; KillAuraMod.ATTACK_RANGE = (float)v; cfg.save(); }, 1, 8),
                new SliderS("Atk Delay", () -> cfg.killAttackDelay,  v -> { cfg.killAttackDelay  = (int)v;   KillAuraMod.attackDelay  = (int)v;   cfg.save(); }, 2, 20),
                new BoolS("Single",      () -> cfg.killSingle,       () -> { cfg.killSingle      = !cfg.killSingle;      KillAuraMod.singleMode    = cfg.killSingle;      cfg.save(); }),
                new BoolS("Hostile",     () -> cfg.killHostile,      () -> { cfg.killHostile     = !cfg.killHostile;     KillAuraMod.targetHostile = cfg.killHostile;     cfg.save(); }),
                new BoolS("Animals",     () -> cfg.killAnimals,      () -> { cfg.killAnimals     = !cfg.killAnimals;     KillAuraMod.targetAnimals = cfg.killAnimals;     cfg.save(); }),
                new BoolS("Players",     () -> cfg.killPlayers,      () -> { cfg.killPlayers     = !cfg.killPlayers;     KillAuraMod.targetPlayers = cfg.killPlayers;     cfg.save(); }),
                new BoolS("Raycast",     () -> cfg.killRaycast,      () -> { cfg.killRaycast     = !cfg.killRaycast;     KillAuraMod.raycast       = cfg.killRaycast;     cfg.save(); }),
                new BoolS("Skip Invis",  () -> cfg.killSkipInvis,    () -> { cfg.killSkipInvis   = !cfg.killSkipInvis;   KillAuraMod.skipInvisible = cfg.killSkipInvis;   cfg.save(); }),
                new BoolS("Wait Cool",   () -> cfg.killWaitCool,     () -> { cfg.killWaitCool    = !cfg.killWaitCool;    KillAuraMod.waitCooldown  = cfg.killWaitCool;    cfg.save(); }),
                new BoolS("Smooth Rot",  () -> cfg.killSmoothRot,    () -> { cfg.killSmoothRot   = !cfg.killSmoothRot;   KillAuraMod.smoothRot     = cfg.killSmoothRot;   cfg.save(); }),
                new SliderS("Max Turn",  () -> cfg.killMaxTurn,      v -> { cfg.killMaxTurn      = (int)v;               KillAuraMod.maxTurnDeg    = (int)v;               cfg.save(); }, 10, 180),
                new BoolS("Body Lock",   () -> cfg.killBodyLock,     () -> { cfg.killBodyLock    = !cfg.killBodyLock;    KillAuraMod.bodyLock      = cfg.killBodyLock;    cfg.save(); }),
                new BoolS("Vis Body",    () -> cfg.killVisBody,      () -> { cfg.killVisBody     = !cfg.killVisBody;     KillAuraMod.visibleBody   = cfg.killVisBody;     cfg.save(); }),
                new KeyS("Key",          () -> cfg.killAuraKey,      v -> { cfg.killAuraKey = v; cfg.save(); })
            )));

        mods.add(new ModEntry("MaceAura", Category.COMBAT,
            MaceAuraMod::isEnabled, MaceAuraMod::toggle, true,
            List.of(
                new SliderS("Det Range", () -> cfg.maceDetectRange, v -> { cfg.maceDetectRange = (float)v; MaceAuraMod.RANGE        = (float)v; cfg.save(); }, 1, 12),
                new SliderS("Atk Range", () -> cfg.maceAttackRange, v -> { cfg.maceAttackRange = (float)v; MaceAuraMod.ATTACK_RANGE = (float)v; cfg.save(); }, 1, 8),
                new BoolS("Hostile",     () -> cfg.maceHostile,     () -> { cfg.maceHostile = !cfg.maceHostile; MaceAuraMod.targetHostile = cfg.maceHostile; cfg.save(); }),
                new BoolS("Animals",     () -> cfg.maceAnimals,     () -> { cfg.maceAnimals = !cfg.maceAnimals; MaceAuraMod.targetAnimals = cfg.maceAnimals; cfg.save(); }),
                new BoolS("Players",     () -> cfg.macePlayers,     () -> { cfg.macePlayers = !cfg.macePlayers; MaceAuraMod.targetPlayers = cfg.macePlayers; cfg.save(); }),
                new KeyS("Key",          () -> cfg.maceAuraKey,     v -> { cfg.maceAuraKey = v; cfg.save(); })
            )));

        mods.add(new ModEntry("SpearAura", Category.COMBAT,
            SpearAuraMod::isEnabled, SpearAuraMod::toggle, true,
            List.of(
                new SliderS("Mode",      () -> cfg.spearMode,        v -> { cfg.spearMode      = (int)v;   SpearAuraMod.mode               = (int)v;   cfg.save(); }, 0, 2),
                new SliderS("Scan Rng",  () -> cfg.spearScanRange,   v -> { cfg.spearScanRange = (float)v; SpearAuraMod.SCAN_RANGE         = (float)v; cfg.save(); }, 3, 10),
                new SliderS("Min Reach", () -> cfg.spearMinReach,    v -> { cfg.spearMinReach  = (float)v; SpearAuraMod.MIN_REACH          = (float)v; cfg.save(); }, 0.5, 3),
                new SliderS("Max Reach", () -> cfg.spearMaxReach,    v -> { cfg.spearMaxReach  = (float)v; SpearAuraMod.MAX_REACH          = (float)v; cfg.save(); }, 2, 12),
                new SliderS("Chg Ticks", () -> cfg.spearChargeTicks, v -> { cfg.spearChargeTicks = (int)v; SpearAuraMod.chargeReleaseTicks = (int)v;   cfg.save(); }, 4, 30),
                new BoolS("Hostile",     () -> cfg.spearHostile,     () -> { cfg.spearHostile  = !cfg.spearHostile;  SpearAuraMod.targetHostile = cfg.spearHostile; cfg.save(); }),
                new BoolS("Animals",     () -> cfg.spearAnimals,     () -> { cfg.spearAnimals  = !cfg.spearAnimals;  SpearAuraMod.targetAnimals = cfg.spearAnimals; cfg.save(); }),
                new BoolS("Players",     () -> cfg.spearPlayers,     () -> { cfg.spearPlayers  = !cfg.spearPlayers;  SpearAuraMod.targetPlayers = cfg.spearPlayers; cfg.save(); }),
                new BoolS("Step Back",   () -> cfg.spearStepBack,    () -> { cfg.spearStepBack = !cfg.spearStepBack; SpearAuraMod.autoStepBack  = cfg.spearStepBack; cfg.save(); }),
                new KeyS("Key",          () -> cfg.spearAuraKey,     v -> { cfg.spearAuraKey = v; cfg.save(); })
            )));

        mods.add(new ModEntry("CritHit", Category.COMBAT,
            CritMod::isEnabled, CritMod::toggle, true,
            List.of(new KeyS("Key", () -> cfg.critKey, v -> { cfg.critKey = v; cfg.save(); }))));

        mods.add(new ModEntry("Hitbox", Category.COMBAT,
            HitboxMod::isEnabled,
            () -> { HitboxMod.toggle(); cfg.hitboxEnabled = HitboxMod.isEnabled(); cfg.save(); },
            true,
            List.of(
                new SliderS("Expand",  () -> (double)cfg.hitboxExpand,
                    v -> { cfg.hitboxExpand = (float)v; HitboxMod.expand = (float)v; cfg.save(); }, 0.05, 1.0),
                new BoolS("Players",   () -> cfg.hitboxPlayers,
                    () -> { cfg.hitboxPlayers = !cfg.hitboxPlayers; HitboxMod.players = cfg.hitboxPlayers; cfg.save(); }),
                new BoolS("Hostile",   () -> cfg.hitboxHostile,
                    () -> { cfg.hitboxHostile = !cfg.hitboxHostile; HitboxMod.hostile = cfg.hitboxHostile; cfg.save(); }),
                new BoolS("Animals",   () -> cfg.hitboxAnimals,
                    () -> { cfg.hitboxAnimals = !cfg.hitboxAnimals; HitboxMod.animals = cfg.hitboxAnimals; cfg.save(); })
            )));

        // Movement
        mods.add(new ModEntry("BHop", Category.MOVEMENT,
            BHopMod::isEnabled, BHopMod::toggle, true,
            List.of(
                new SliderS("Speed",     () -> (double)cfg.bhopSpeed,      v -> { cfg.bhopSpeed      = (float)v; BHopMod.speedMultiplier = (float)v; cfg.save(); }, 0.5, 2.5),
                new BoolS("Low Hop",     () -> cfg.bhopLowHop,             () -> { cfg.bhopLowHop    = !cfg.bhopLowHop;          BHopMod.lowHop     = cfg.bhopLowHop;     cfg.save(); }),
                new SliderS("Jump Hgt",  () -> (double)cfg.bhopJumpHeight, v -> { cfg.bhopJumpHeight = (float)v; BHopMod.jumpHeight     = (float)v; cfg.save(); }, 0.1, 1.0),
                new SliderS("Boost",     () -> (double)cfg.bhopBoost,      v -> { cfg.bhopBoost      = (float)v; BHopMod.boost          = (float)v; cfg.save(); }, 1.0, 1.5),
                new BoolS("KB Boost",    () -> cfg.bhopKBBoost,            () -> { cfg.bhopKBBoost   = !cfg.bhopKBBoost;         BHopMod.kbBoost    = cfg.bhopKBBoost;    cfg.save(); }),
                new KeyS("Key",          () -> cfg.bhopKey,                v -> { cfg.bhopKey = v; cfg.save(); })
            )));

        mods.add(new ModEntry("Velocity", Category.MOVEMENT,
            VelocityMod::isEnabled, VelocityMod::toggle, true,
            List.of(
                new SliderS("Horiz", () -> cfg.velocityHoriz, v -> { cfg.velocityHoriz = (int)v; VelocityMod.horizontal = (int)v; cfg.save(); }, 0, 200),
                new SliderS("Vert",  () -> cfg.velocityVert,  v -> { cfg.velocityVert  = (int)v; VelocityMod.vertical   = (int)v; cfg.save(); }, 0, 200),
                new KeyS("Key",      () -> cfg.velocityKey,   v -> { cfg.velocityKey = v; cfg.save(); })
            )));

        mods.add(new ModEntry("FastPlace", Category.MOVEMENT,
            FastPlaceMod::isEnabled, FastPlaceMod::toggle, true,
            List.of(new KeyS("Key", () -> cfg.fastPlaceKey, v -> { cfg.fastPlaceKey = v; cfg.save(); }))));

        mods.add(new ModEntry("AutoFish", Category.MOVEMENT,
            AutoFishMod::isEnabled,
            () -> { AutoFishMod.toggle(); cfg.autoFishEnabled = AutoFishMod.isEnabled(); cfg.save(); },
            true,
            List.of(
                new SliderS("Bite Vy", () -> (double)cfg.autoFishBiteVy,
                    v -> { cfg.autoFishBiteVy = (float)v; AutoFishMod.biteVy = (float)v; cfg.save(); }, -0.2, -0.01),
                new SliderS("Recast", () -> (double)cfg.autoFishRecast,
                    v -> { cfg.autoFishRecast = (int)v; AutoFishMod.recastDelay = (int)v; cfg.save(); }, 4, 40)
            )));

        mods.add(new ModEntry("Scaffold", Category.MOVEMENT,
            ScaffoldMod::isEnabled, ScaffoldMod::toggle, true,
            List.of(
                new BoolS("Tower",       () -> cfg.scaffoldTower,         () -> { cfg.scaffoldTower      = !cfg.scaffoldTower;      ScaffoldMod.tower      = cfg.scaffoldTower;      cfg.save(); }),
                new BoolS("Tower Move",  () -> cfg.scaffoldTowerMove,     () -> { cfg.scaffoldTowerMove  = !cfg.scaffoldTowerMove;  ScaffoldMod.towerMove  = cfg.scaffoldTowerMove;  cfg.save(); }),
                new SliderS("Tower Spd", () -> cfg.scaffoldTowerSpeed,    v -> { cfg.scaffoldTowerSpeed = (float)v;                ScaffoldMod.towerSpeed = (float)v;               cfg.save(); }, 0.0, 1.0),
                new BoolS("Fake Hand",   () -> cfg.scaffoldFakeHand,      () -> { cfg.scaffoldFakeHand   = !cfg.scaffoldFakeHand;   ScaffoldMod.fakeHand   = cfg.scaffoldFakeHand;   cfg.save(); }),
                new BoolS("Silent Rot",  () -> cfg.scaffoldSilentRot,     () -> { cfg.scaffoldSilentRot  = !cfg.scaffoldSilentRot;  ScaffoldMod.silentRot  = cfg.scaffoldSilentRot;  cfg.save(); }),
                new BoolS("Auto Sw",     () -> cfg.scaffoldAutoSwitch,    () -> { cfg.scaffoldAutoSwitch = !cfg.scaffoldAutoSwitch; ScaffoldMod.autoSwitch = cfg.scaffoldAutoSwitch; cfg.save(); }),
                new BoolS("Sw Back",     () -> cfg.scaffoldSwitchBack,    () -> { cfg.scaffoldSwitchBack = !cfg.scaffoldSwitchBack; ScaffoldMod.switchBack = cfg.scaffoldSwitchBack; cfg.save(); }),
                new SliderS("Delay",     () -> cfg.scaffoldPlaceDelay,    v -> { cfg.scaffoldPlaceDelay  = (int)v;                  ScaffoldMod.placeDelay = (int)v;                  cfg.save(); }, 0, 10),
                new KeyS("Key",          () -> cfg.scaffoldKey,           v -> { cfg.scaffoldKey         = v;                       cfg.save(); })
            )));

        mods.add(new ModEntry("NoFall", Category.MOVEMENT,
            NoFallMod::isEnabled, NoFallMod::toggleManual, true,
            List.of(new KeyS("Key", () -> cfg.nofallKey, v -> { cfg.nofallKey = v; cfg.save(); }))));

        mods.add(new ModEntry("NoSlow", Category.MOVEMENT,
            NoSlowMod::isEnabled, NoSlowMod::toggle, true,
            List.of(new KeyS("Key", () -> cfg.noSlowKey, v -> { cfg.noSlowKey = v; cfg.save(); }))));

        mods.add(new ModEntry("Step", Category.MOVEMENT,
            StepMod::isEnabled, StepMod::toggle, true,
            List.of(
                new SliderS("Height", () -> cfg.stepHeight, v -> { cfg.stepHeight = (float)v; StepMod.stepHeight = (float)v; cfg.save(); }, 1.0, 3.0),
                new KeyS("Key",       () -> cfg.stepKey,    v -> { cfg.stepKey = v; cfg.save(); })
            )));

        mods.add(new ModEntry("Timer", Category.MOVEMENT,
            TimerMod::isEnabled, TimerMod::toggle, true,
            List.of(
                new SliderS("Speed", () -> (double)cfg.timerSpeed, v -> { cfg.timerSpeed = (float)v; TimerMod.speed = (float)v; cfg.save(); }, 1.0, 3.0),
                new KeyS("Key",      () -> cfg.timerKey,           v -> { cfg.timerKey = v; cfg.save(); })
            )));

        mods.add(new ModEntry("ElytraGoto", Category.MOVEMENT,
            ElytraGotoMod::isEnabled, ElytraGotoMod::toggle, true,
            List.of(
                new SliderS("Safe HP", () -> (double)cfg.elytraSafeHp,
                    v -> { cfg.elytraSafeHp = (float)v; ElytraGotoMod.safeHpThreshold = (float)v; cfg.save(); }, 2, 20)
            )));

        mods.add(new ModEntry("NoCrash", Category.MOVEMENT,
            NoCrashMod::isEnabled,
            () -> { NoCrashMod.toggle(); cfg.noCrashEnabled = NoCrashMod.isEnabled(); cfg.save(); },
            true,
            List.of(
                new SliderS("Look Ahead", () -> (double)cfg.noCrashLookAhead,
                    v -> { cfg.noCrashLookAhead = (float)v; NoCrashMod.lookahead = (float)v; cfg.save(); }, 2, 10),
                new SliderS("Max Speed",  () -> (double)cfg.noCrashMaxSpeed,
                    v -> { cfg.noCrashMaxSpeed  = (float)v; NoCrashMod.maxSpeed  = (float)v; cfg.save(); }, 0.1, 1.0)
            )));

        // Visuals
        mods.add(new ModEntry("XRay", Category.VISUALS,
            XRayMod::isEnabled, XRayMod::toggle, true,
            List.of(new KeyS("Key", () -> cfg.xrayKey, v -> { cfg.xrayKey = v; cfg.save(); }))));

        mods.add(new ModEntry("DimCoord", Category.VISUALS,
            NetherCoordMod::isEnabled,
            () -> { NetherCoordMod.toggle(); cfg.netherCoordEnabled = NetherCoordMod.isEnabled(); cfg.save(); },
            true, List.<Setting>of()
        ));

        mods.add(new ModEntry("ESP", Category.VISUALS,
            ESPMod::isEnabled,
            () -> { ESPMod.toggle(); cfg.espEnabled = ESPMod.isEnabled(); cfg.save(); },
            true,
            List.of(
                new BoolS("Players", () -> cfg.espPlayers,
                    () -> { cfg.espPlayers = !cfg.espPlayers; ESPMod.players = cfg.espPlayers; cfg.save(); }),
                new BoolS("Hostile", () -> cfg.espHostile,
                    () -> { cfg.espHostile = !cfg.espHostile; ESPMod.hostile = cfg.espHostile; cfg.save(); }),
                new BoolS("Animals", () -> cfg.espAnimals,
                    () -> { cfg.espAnimals = !cfg.espAnimals; ESPMod.animals = cfg.espAnimals; cfg.save(); }),
                new BoolS("Items",   () -> cfg.espItems,
                    () -> { cfg.espItems   = !cfg.espItems;   ESPMod.items   = cfg.espItems;   cfg.save(); }),
                new BoolS("Box",     () -> cfg.espShowBox,
                    () -> { cfg.espShowBox = !cfg.espShowBox; ESPMod.showBox = cfg.espShowBox; cfg.save(); }),
                new BoolS("Corner",  () -> cfg.espCornerBox,
                    () -> { cfg.espCornerBox = !cfg.espCornerBox; ESPMod.cornerBox = cfg.espCornerBox; cfg.save(); }),
                new BoolS("Name",    () -> cfg.espShowName,
                    () -> { cfg.espShowName = !cfg.espShowName; ESPMod.showName = cfg.espShowName; cfg.save(); }),
                new BoolS("Health",  () -> cfg.espShowHealth,
                    () -> { cfg.espShowHealth = !cfg.espShowHealth; ESPMod.showHealth = cfg.espShowHealth; cfg.save(); })
            )));

        // Settings — non-toggleable
        mods.add(new ModEntry("Theme Color", Category.SETTINGS,
            () -> false, () -> {}, false,
            List.of(
                new SliderS("Red",      () -> cfg.accentR, v -> { cfg.accentR = (int)v; cfg.save(); }, 0, 255),
                new SliderS("Green",    () -> cfg.accentG, v -> { cfg.accentG = (int)v; cfg.save(); }, 0, 255),
                new SliderS("Blue",     () -> cfg.accentB, v -> { cfg.accentB = (int)v; cfg.save(); }, 0, 255),
                new SliderS("BG Alpha", () -> cfg.bgAlpha, v -> { cfg.bgAlpha = (int)v; cfg.save(); }, 60, 240)
            )));

        mods.add(new ModEntry("GUI Key", Category.SETTINGS,
            () -> false, () -> {}, false,
            List.of(new KeyS("Key", () -> cfg.guiKey, v -> { cfg.guiKey = v; cfg.save(); }))));
    }

    // ── Waifu loading ────────────────────────────────────────────────────────
    private static final String[] WAIFU_EXTS = {"png", "jpg", "jpeg", "bmp", "gif"};

    private void loadWaifu() {
        File dir = FabricLoader.getInstance().getConfigDir().resolve("valencia").toFile();
        waifuHint = dir.getAbsolutePath();
        try {
            if (!dir.exists() || !dir.isDirectory()) { waifuErr = "config dir missing"; return; }
            File f = null;
            for (String ext : WAIFU_EXTS) {
                File c = new File(dir, "waifu." + ext);
                if (c.exists()) { f = c; break; }
            }
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
            waifuTexW = ni.getWidth();
            waifuTexH = ni.getHeight();
            DynamicTexture dt = new DynamicTexture(() -> "valencia-waifu", ni);
            Identifier loc = Identifier.fromNamespaceAndPath("valencia", "waifu");
            Minecraft.getInstance().getTextureManager().register(loc, dt);
            waifuLoc = loc;
            waifuErr = null;
        } catch (Throwable t) {
            waifuErr = t.getClass().getSimpleName() + ": " + t.getMessage();
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  RENDER — Discord-style 3-panel layout
    // ══════════════════════════════════════════════════════════════════════════
    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        ModConfig cfg = ModConfig.get();
        int accent = accent(cfg, 255);

        if (wx == 0 && wy == 0) { wx = (width - WIN_W) / 2; wy = (height - WIN_H) / 2; }

        // dim overlay
        g.fill(0, 0, width, height, 0x80000000);

        int x0 = wx, y0 = wy, x1 = wx + WIN_W, y1 = wy + WIN_H;
        int bodyY = y0 + HDR_H, bodyH = WIN_H - HDR_H;

        // ── Header (accent-colored, draggable) ───────────────────────────────
        g.fill(x0, y0, x1, y0 + HDR_H, accent);
        g.drawCenteredString(font, "§lValencia", x0 + WIN_W / 2, y0 + (HDR_H - 8) / 2, 0xFFFFFFFF);

        // ── Sidebar ──────────────────────────────────────────────────────────
        g.fill(x0, bodyY, x0 + SIDE_W, y1, C_SIDEBAR);
        g.fill(x0 + SIDE_W, bodyY, x0 + SIDE_W + 1, y1, C_DIV);

        int cy = bodyY + CAT_GAP + 4;
        for (Category cat : Category.values()) {
            boolean sel = cat == selCat;
            boolean hov = mx >= x0 + 4 && mx < x0 + SIDE_W - 2 && my >= cy && my < cy + CAT_SZ;

            // pill indicator
            if (sel)       g.fill(x0, cy + 4, x0 + 3, cy + CAT_SZ - 4, 0xFFFFFFFF);
            else if (hov)  g.fill(x0, cy + 10, x0 + 3, cy + CAT_SZ - 10, 0xFFCCCCCC);

            // button bg
            int bx = x0 + 7;
            g.fill(bx, cy, bx + CAT_SZ - 2, cy + CAT_SZ, sel ? accent : hov ? C_HOVER : 0xFF36393F);

            // letter
            g.drawCenteredString(font, cat.label.substring(0, 1), bx + (CAT_SZ - 2) / 2, cy + (CAT_SZ - 8) / 2, 0xFFFFFFFF);

            // tooltip
            if (hov && !sel) {
                int tw = font.width(cat.label) + 8;
                g.fill(x0 + SIDE_W + 4, cy + 8, x0 + SIDE_W + 4 + tw, cy + 20, 0xEE111111);
                g.drawString(font, cat.label, x0 + SIDE_W + 8, cy + 10, 0xFFFFFFFF, false);
            }

            cy += CAT_SZ + CAT_GAP;
        }

        // ── Module list ──────────────────────────────────────────────────────
        int lx = x0 + SIDE_W + 1, lx1b = lx + LIST_W;
        g.fill(lx, bodyY, lx1b, y1, C_LIST);

        // category title
        g.drawString(font, selCat.label, lx + PAD, bodyY + 6, C_DIM, false);
        g.fill(lx + PAD, bodyY + 18, lx1b - PAD, bodyY + 19, C_DIV);

        int modTop = bodyY + 22;
        int my0 = modTop + modScroll;
        for (ModEntry m : mods) {
            if (m.cat() != selCat) continue;
            if (my0 + MOD_H > modTop && my0 < y1) {
                boolean on   = m.toggleable() && m.enabled().getAsBoolean();
                boolean hov  = mx >= lx && mx < lx1b && my >= my0 && my < my0 + MOD_H && my >= modTop;
                boolean isSel = m.name().equals(selMod);

                if (isSel)      g.fill(lx, my0, lx1b, my0 + MOD_H, C_SEL);
                else if (hov)   g.fill(lx, my0, lx1b, my0 + MOD_H, C_HOVER);

                // enabled bar
                if (on) g.fill(lx, my0, lx + 2, my0 + MOD_H, C_GREEN);

                g.drawString(font, "#", lx + PAD + 1, my0 + (MOD_H - 8) / 2, C_HASH, false);
                int nc = on ? C_GREEN : isSel ? C_TEXT : C_DIM;
                g.drawString(font, m.name(), lx + PAD + 12, my0 + (MOD_H - 8) / 2, nc, false);

                if (!m.settings().isEmpty())
                    g.drawString(font, "›", lx1b - 10, my0 + (MOD_H - 8) / 2, C_MUTE, false);
            }
            my0 += MOD_H;
        }

        g.fill(lx1b, bodyY, lx1b + 1, y1, C_DIV);

        // ── Settings panel ───────────────────────────────────────────────────
        int px = lx1b + 1, pw = x1 - px;
        g.fill(px, bodyY, x1, y1, C_PANEL);

        if (selMod != null) {
            ModEntry mod = findMod(selMod);
            if (mod != null) renderSettings(g, mod, px, bodyY, pw, bodyH, mx, my, cfg, accent);
        } else {
            g.drawCenteredString(font, "§8← Select a module", px + pw / 2, bodyY + bodyH / 2 - 4, C_MUTE);
        }

        // ── Waifu ────────────────────────────────────────────────────────────
        renderWaifu(g);

        // close hint
        g.drawString(font, "§7" + shortKey(cfg.guiKey) + " to close",
            x0 + 4, y1 + 3, 0xFF555555, false);
    }

    // ── Settings panel rendering ─────────────────────────────────────────────
    private void renderSettings(GuiGraphics g, ModEntry mod, int px, int topY, int pw, int h, int mx, int my, ModConfig cfg, int accent) {
        boolean on = mod.toggleable() && mod.enabled().getAsBoolean();

        // header row: name + ON/OFF toggle button
        g.drawString(font, "§f" + mod.name(), px + PAD, topY + 6, C_TEXT, true);
        if (mod.toggleable()) {
            int tbx = px + pw - 44, tby = topY + 4;
            g.fill(tbx, tby, tbx + 36, tby + 16, on ? accent : 0xFF4F545C);
            g.drawCenteredString(font, on ? "ON" : "OFF", tbx + 18, tby + 4, 0xFFFFFFFF);
        }
        g.fill(px + PAD, topY + 22, px + pw - PAD, topY + 23, C_DIV);

        if (mod.settings().isEmpty()) {
            g.drawCenteredString(font, "§8No settings", px + pw / 2, topY + h / 2, C_MUTE);
            return;
        }

        int setTop = topY + 26;
        int sy = setTop + setScroll;
        for (int i = 0; i < mod.settings().size(); i++) {
            if (sy + SET_H > setTop && sy < topY + h) {
                Setting s = mod.settings().get(i);
                if (s instanceof SliderS sl)    renderSlider(g, sl, px, sy, pw, accent);
                else if (s instanceof BoolS bs) renderBool(g, bs, px, sy, pw, accent);
                else if (s instanceof KeyS ks)  renderKey(g, ks, mod, i, px, sy, pw);
            }
            sy += SET_H;
        }
    }

    private void renderSlider(GuiGraphics g, SliderS sl, int px, int y, int pw, int accent) {
        double val = sl.get().getAsDouble();
        double pct = Math.max(0, Math.min(1, (val - sl.min()) / (sl.max() - sl.min())));

        int labW = 52;
        int valW = 30;
        int trackX = px + PAD + labW;
        int trackW = pw - PAD * 2 - labW - valW;
        int trackY = y + SET_H / 2;

        g.drawString(font, sl.label(), px + PAD, y + 2, C_DIM, false);
        g.fill(trackX, trackY, trackX + trackW, trackY + 2, 0xFF4F545C);
        g.fill(trackX, trackY, trackX + (int)(trackW * pct), trackY + 2, accent);
        int hx = trackX + (int)(trackW * pct);
        g.fill(hx - 2, y + 2, hx + 2, y + SET_H - 2, 0xFFFFFFFF);

        String valStr = isIntSlider(sl) ? String.valueOf((int)Math.round(val)) : String.format("%.1f", val);
        g.drawString(font, valStr, px + pw - valW, y + 2, C_DIM, false);
    }

    private void renderBool(GuiGraphics g, BoolS bs, int px, int y, int pw, int accent) {
        boolean on = bs.get().getAsBoolean();
        g.drawString(font, bs.label(), px + PAD, y + 2, C_DIM, false);
        // toggle pill
        int tx = px + pw - 34, ty = y + 2;
        g.fill(tx, ty, tx + 24, ty + 12, on ? accent : 0xFF4F545C);
        int kx = on ? tx + 14 : tx + 2;
        g.fill(kx, ty + 2, kx + 8, ty + 10, 0xFFFFFFFF);
    }

    private void renderKey(GuiGraphics g, KeyS ks, ModEntry m, int si, int px, int y, int pw) {
        boolean waiting = m.name().equals(rebindMod) && rebindIdx == si;
        g.drawString(font, ks.label(), px + PAD, y + 2, C_DIM, false);
        String keyTxt = waiting ? "§e..." : "§7[" + shortKey(ks.get().getAsInt()) + "]";
        g.drawString(font, keyTxt, px + pw - 48, y + 2, C_DIM, false);
    }

    // ── Waifu ────────────────────────────────────────────────────────────────
    private void renderWaifu(GuiGraphics g) {
        if (waifuLoc == null || waifuTexW <= 0 || waifuTexH <= 0) {
            String hint = waifuErr != null
                ? "§c[waifu] " + waifuErr
                : "§8[waifu: " + (waifuHint != null ? waifuHint : "config/valencia") + File.separator + "waifu.png]";
            g.drawString(font, hint, 4, height - 12, 0xFF888888, false);
            return;
        }
        try {
            int dispH = Math.min(height / 3, 150);
            int dispW = waifuTexW * dispH / waifuTexH;
            int x1 = 4, y1 = height - dispH - 14;
            g.blit(waifuLoc, x1, y1, x1 + dispW, y1 + dispH, 0f, 1f, 0f, 1f);
        } catch (Throwable t) {
            g.drawString(font, "§c[waifu err]", 4, height - 12, 0xFF888888, false);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  INPUT
    // ══════════════════════════════════════════════════════════════════════════

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean dbl) {
        int mx = (int)event.x(), my = (int)event.y(), btn = event.button();
        int bodyY = wy + HDR_H;

        // ── Header drag ──────────────────────────────────────────────────────
        if (btn == 0 && mx >= wx && mx < wx + WIN_W && my >= wy && my < bodyY) {
            wDrag = true; wdx = wx - mx; wdy = wy - my;
            return true;
        }

        // ── Sidebar category ─────────────────────────────────────────────────
        if (btn == 0) {
            int cy = bodyY + CAT_GAP + 4;
            for (Category cat : Category.values()) {
                if (mx >= wx + 4 && mx < wx + SIDE_W - 2 && my >= cy && my < cy + CAT_SZ) {
                    selCat = cat; selMod = null; modScroll = 0; setScroll = 0;
                    return true;
                }
                cy += CAT_SZ + CAT_GAP;
            }
        }

        // ── Module list ──────────────────────────────────────────────────────
        int lx = wx + SIDE_W + 1, lx1b = lx + LIST_W;
        int modTop = bodyY + 22;
        if (mx >= lx && mx < lx1b && my >= modTop) {
            int my0 = modTop + modScroll;
            for (ModEntry m : mods) {
                if (m.cat() != selCat) continue;
                if (my >= my0 && my < my0 + MOD_H && my0 + MOD_H > modTop) {
                    if (btn == 0) {
                        // left click: select (show settings)
                        selMod = m.name().equals(selMod) ? null : m.name();
                        setScroll = 0;
                    } else if (btn == 1) {
                        // right click: toggle on/off
                        if (m.toggleable()) { m.toggle().run(); saveEnabled(ModConfig.get()); }
                    }
                    return true;
                }
                my0 += MOD_H;
            }
        }

        // ── Settings panel: toggle button ────────────────────────────────────
        int px = lx1b + 1, pw = wx + WIN_W - px;
        if (btn == 0 && selMod != null && mx >= px) {
            ModEntry mod = findMod(selMod);
            if (mod != null && mod.toggleable()) {
                int tbx = px + pw - 44, tby = bodyY + 4;
                if (mx >= tbx && mx < tbx + 36 && my >= tby && my < tby + 20) {
                    mod.toggle().run(); saveEnabled(ModConfig.get());
                    return true;
                }
            }
        }

        // ── Settings panel: individual settings ──────────────────────────────
        if (btn == 0 && selMod != null && mx >= px && mx < wx + WIN_W) {
            ModEntry mod = findMod(selMod);
            if (mod != null && !mod.settings().isEmpty()) {
                int setTop = bodyY + 26;
                int sy = setTop + setScroll;
                for (int i = 0; i < mod.settings().size(); i++) {
                    if (my >= sy && my < sy + SET_H && sy + SET_H > setTop) {
                        Setting s = mod.settings().get(i);
                        if (s instanceof SliderS sl) {
                            dragMod = selMod; dragIdx = i;
                            int trackX = px + PAD + 52;
                            int trackW = pw - PAD * 2 - 52 - 30;
                            applySlider(sl, mx, trackX, trackW);
                        } else if (s instanceof BoolS bs) {
                            bs.toggle().run();
                        } else if (s instanceof KeyS) {
                            rebindMod = mod.name(); rebindIdx = i;
                        }
                        return true;
                    }
                    sy += SET_H;
                }
            }
        }

        return super.mouseClicked(event, dbl);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        int mx = (int)event.x(), my = (int)event.y();

        // window drag
        if (wDrag) { wx = wdx + mx; wy = wdy + my; return true; }

        // slider drag
        if (dragMod != null) {
            ModEntry mod = findMod(dragMod);
            if (mod != null && dragIdx >= 0 && dragIdx < mod.settings().size()) {
                Setting s = mod.settings().get(dragIdx);
                if (s instanceof SliderS sl) {
                    int lx1b = wx + SIDE_W + 1 + LIST_W + 1;
                    int pw = wx + WIN_W - lx1b;
                    int trackX = lx1b + PAD + 52;
                    int trackW = pw - PAD * 2 - 52 - 30;
                    applySlider(sl, mx, trackX, trackW);
                }
            }
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        wDrag = false; dragMod = null; dragIdx = -1;
        return super.mouseReleased(event);
    }

    // ── Scroll ───────────────────────────────────────────────────────────────
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int mx = (int)mouseX, my = (int)mouseY;
        int bodyY = wy + HDR_H;
        int lx = wx + SIDE_W + 1, lx1b = lx + LIST_W;
        int px = lx1b + 1;

        int step = (int)(scrollY * 8);

        // scroll module list
        if (mx >= lx && mx < lx1b && my >= bodyY) {
            modScroll += step;
            if (modScroll > 0) modScroll = 0;
            return true;
        }
        // scroll settings
        if (mx >= px && mx < wx + WIN_W && my >= bodyY) {
            setScroll += step;
            if (setScroll > 0) setScroll = 0;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    // ── Keyboard ─────────────────────────────────────────────────────────────
    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (rebindMod != null) {
            if (key != GLFW.GLFW_KEY_ESCAPE) {
                ModEntry mod = findMod(rebindMod);
                if (mod != null && rebindIdx >= 0 && rebindIdx < mod.settings().size()) {
                    Setting s = mod.settings().get(rebindIdx);
                    if (s instanceof KeyS ks) ks.set().accept(key);
                }
            }
            rebindMod = null; rebindIdx = -1;
            return true;
        }
        if (key == ModConfig.get().guiKey) { onClose(); return true; }
        return super.keyPressed(event);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private ModEntry findMod(String name) {
        for (ModEntry m : mods) if (m.name().equals(name)) return m;
        return null;
    }

    private void applySlider(SliderS sl, int mx, int tx, int tw) {
        double pct = Math.max(0, Math.min(1, (double)(mx - tx) / tw));
        double raw = sl.min() + pct * (sl.max() - sl.min());
        boolean intRange = isIntSlider(sl);
        double val = intRange ? Math.round(raw) : Math.round(raw * 10.0) / 10.0;
        sl.set().accept(val);
    }

    private static boolean isIntSlider(SliderS sl) {
        return (int)sl.min() == sl.min()
            && (int)sl.max() == sl.max()
            && sl.max() - sl.min() >= 2
            && sl.max() - sl.min() <= 30;
    }

    private static int accent(ModConfig cfg, int alpha) {
        return argb(alpha, cfg.accentR, cfg.accentG, cfg.accentB);
    }

    private static int argb(int a, int r, int g, int b) {
        return (Math.min(255, Math.max(0, a)) << 24)
             | (Math.min(255, Math.max(0, r)) << 16)
             | (Math.min(255, Math.max(0, g)) << 8)
             |  Math.min(255, Math.max(0, b));
    }

    private static void saveEnabled(ModConfig cfg) {
        cfg.nofallEnabled     = NoFallMod.isEnabled();
        cfg.xrayEnabled       = XRayMod.isEnabled();
        cfg.maceAuraEnabled   = MaceAuraMod.isEnabled();
        cfg.noSlowEnabled     = NoSlowMod.isEnabled();
        cfg.bhopEnabled       = BHopMod.isEnabled();
        cfg.stepEnabled       = StepMod.isEnabled();
        cfg.killAuraEnabled   = KillAuraMod.isEnabled();
        cfg.velocityEnabled   = VelocityMod.isEnabled();
        cfg.fastPlaceEnabled  = FastPlaceMod.isEnabled();
        cfg.critEnabled       = CritMod.isEnabled();
        cfg.scaffoldEnabled   = ScaffoldMod.isEnabled();
        cfg.timerEnabled      = TimerMod.isEnabled();
        cfg.save();
    }

    private static String shortKey(int key) {
        String s = ModConfig.keyName(key);
        return switch (s) {
            case "RIGHT_CONTROL" -> "RCTRL";
            case "LEFT_CONTROL"  -> "LCTRL";
            case "RIGHT_SHIFT"   -> "RSHIFT";
            case "LEFT_SHIFT"    -> "LSHIFT";
            default -> s.length() > 6 ? s.substring(0, 6) : s;
        };
    }
}
