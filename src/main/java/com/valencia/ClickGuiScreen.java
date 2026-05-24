package com.valencia;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class ClickGuiScreen extends Screen {

    // ── Categories ────────────────────────────────────────────────────────────
    private enum Category {
        COMBAT("Combat"),
        MOVEMENT("Movement"),
        VISUALS("Visuals"),
        SETTINGS("Settings");
        final String label;
        Category(String l) { label = l; }
    }

    // ── Settings ──────────────────────────────────────────────────────────────
    interface Setting {}
    record SliderS(String label, DoubleSupplier get, DoubleConsumer set, double min, double max) implements Setting {}
    record BoolS(String label, BooleanSupplier get, Runnable toggle) implements Setting {}
    record KeyS(String label, IntSupplier get, IntConsumer set) implements Setting {}

    // ── Module entry ──────────────────────────────────────────────────────────
    record ModEntry(
        String name, Category cat,
        BooleanSupplier enabled, Runnable toggle,
        boolean toggleable,
        List<Setting> settings
    ) {}

    // ── Layout ────────────────────────────────────────────────────────────────
    private static final int COL_W  = 160;
    private static final int COL_GAP = 6;
    private static final int COL_Y  = 30;
    private static final int HDR_H  = 22;
    private static final int ROW_H  = 20;
    private static final int SET_H  = 14;
    private static final int INDENT = 6;

    // ── State ─────────────────────────────────────────────────────────────────
    private final List<ModEntry> mods = new ArrayList<>();
    private final Set<String> expanded = new HashSet<>();
    private String rebindMod = null;
    private int    rebindIdx = -1;
    private String dragMod  = null;
    private int    dragIdx  = -1;

    // ── Waifu ─────────────────────────────────────────────────────────────────
    private Identifier waifuLoc = null;
    private int        waifuTexW = 0;
    private int        waifuTexH = 0;
    private String     waifuHint = null;
    private String     waifuErr  = null;

    // ─────────────────────────────────────────────────────────────────────────

    public ClickGuiScreen() {
        super(Component.empty());
        buildMods();
        loadWaifu();
    }

    @Override public boolean isPauseScreen() { return false; }

    // ── Module list ───────────────────────────────────────────────────────────
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
                // Mode: 0=Jab, 1=Charge, 2=Auto (picks Charge if mounted or moving fast)
                new SliderS("Mode",      () -> cfg.spearMode,        v -> { cfg.spearMode      = (int)v;   SpearAuraMod.mode               = (int)v;   cfg.save(); }, 0, 2),
                new SliderS("Scan Rng",  () -> cfg.spearScanRange,   v -> { cfg.spearScanRange = (float)v; SpearAuraMod.SCAN_RANGE         = (float)v; cfg.save(); }, 3, 10),
                new SliderS("Min Reach", () -> cfg.spearMinReach,    v -> { cfg.spearMinReach  = (float)v; SpearAuraMod.MIN_REACH          = (float)v; cfg.save(); }, 0.5, 3),
                new SliderS("Max Reach", () -> cfg.spearMaxReach,    v -> { cfg.spearMaxReach  = (float)v; SpearAuraMod.MAX_REACH          = (float)v; cfg.save(); }, 2, 8),
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
            () -> {
                AutoFishMod.toggle();
                ModConfig.get().autoFishEnabled = AutoFishMod.isEnabled();
                ModConfig.get().save();
            },
            true,
            List.of(
                new SliderS("Bite Vy", () -> (double)cfg.autoFishBiteVy,
                    v -> { cfg.autoFishBiteVy = (float)v; AutoFishMod.biteVy = (float)v; cfg.save(); },
                    -0.2, -0.01),
                new SliderS("Recast", () -> (double)cfg.autoFishRecast,
                    v -> { cfg.autoFishRecast = (int)v; AutoFishMod.recastDelay = (int)v; cfg.save(); },
                    4, 40)
            )
        ));

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
                    v -> { cfg.elytraSafeHp = (float)v; ElytraGotoMod.safeHpThreshold = (float)v; cfg.save(); },
                    2, 20)
            )
        ));

        // Visuals
        mods.add(new ModEntry("XRay", Category.VISUALS,
            XRayMod::isEnabled, XRayMod::toggle, true,
            List.of(new KeyS("Key", () -> cfg.xrayKey, v -> { cfg.xrayKey = v; cfg.save(); }))));

        mods.add(new ModEntry("DimCoord", Category.VISUALS,
            NetherCoordMod::isEnabled,
            () -> {
                NetherCoordMod.toggle();
                ModConfig.get().netherCoordEnabled = NetherCoordMod.isEnabled();
                ModConfig.get().save();
            },
            true, List.<Setting>of()
        ));

        mods.add(new ModEntry("ESP", Category.VISUALS,
            ESPMod::isEnabled,
            () -> {
                ESPMod.toggle();
                ModConfig.get().espEnabled = ESPMod.isEnabled();
                ModConfig.get().save();
            },
            true,
            List.of(
                new BoolS("Players", () -> cfg.espPlayers,
                    () -> { cfg.espPlayers = !cfg.espPlayers; ESPMod.players = cfg.espPlayers; cfg.save(); }),
                new BoolS("Hostile", () -> cfg.espHostile,
                    () -> { cfg.espHostile = !cfg.espHostile; ESPMod.hostile = cfg.espHostile; cfg.save(); }),
                new BoolS("Animals", () -> cfg.espAnimals,
                    () -> { cfg.espAnimals = !cfg.espAnimals; ESPMod.animals = cfg.espAnimals; cfg.save(); }),
                new BoolS("Items",   () -> cfg.espItems,
                    () -> { cfg.espItems   = !cfg.espItems;   ESPMod.items   = cfg.espItems;   cfg.save(); })
            )
        ));

        // Settings — not toggleable, always show sliders
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

    // ── Waifu ─────────────────────────────────────────────────────────────────
    // NativeImage only decodes PNG; other formats go through ImageIO first.
    private static final String[] WAIFU_EXTS = {"png", "jpg", "jpeg", "bmp", "gif"};

    private void loadWaifu() {
        File dir = FabricLoader.getInstance().getConfigDir().resolve("valencia").toFile();
        waifuHint = dir.getAbsolutePath();
        try {
            if (!dir.exists() || !dir.isDirectory()) {
                waifuErr = "config dir 不存在";
                return;
            }

            File f = null;
            for (String ext : WAIFU_EXTS) {
                File candidate = new File(dir, "waifu." + ext);
                if (candidate.exists()) { f = candidate; break; }
            }
            if (f == null) {
                waifuErr = "找不到 waifu.{png,jpg,bmp,gif}";
                return;
            }

            // NativeImage only handles PNG; convert other formats via ImageIO
            InputStream pngStream;
            String name = f.getName().toLowerCase();
            if (name.endsWith(".png")) {
                pngStream = new FileInputStream(f);
            } else {
                BufferedImage img = ImageIO.read(f);
                if (img == null) { waifuErr = "ImageIO 解碼失敗"; return; }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "png", baos);
                pngStream = new ByteArrayInputStream(baos.toByteArray());
            }

            NativeImage ni = NativeImage.read(pngStream);
            waifuTexW = ni.getWidth();
            waifuTexH = ni.getHeight();

            // DynamicTexture(Supplier<String>, NativeImage) — the (NativeImage)
            // overload that older MC had is gone in 1.21.x.
            DynamicTexture dt = new DynamicTexture(() -> "valencia-waifu", ni);

            Identifier loc = Identifier.fromNamespaceAndPath("valencia", "waifu");
            Minecraft.getInstance().getTextureManager().register(loc, dt);
            waifuLoc = loc;
            waifuErr = null;
        } catch (Throwable t) {
            waifuErr = t.getClass().getSimpleName() + ": " + t.getMessage();
        }
    }

    // ── Render ────────────────────────────────────────────────────────────────
    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        ModConfig cfg = ModConfig.get();
        int accent = accent(cfg, 255);

        // Full-screen overlay
        g.fill(0, 0, width, height, argb(cfg.bgAlpha, 0, 0, 0));

        // Columns
        int totalW = Category.values().length * COL_W + (Category.values().length - 1) * COL_GAP;
        int startX = (width - totalW) / 2;

        for (Category cat : Category.values()) {
            int cx = startX + cat.ordinal() * (COL_W + COL_GAP);
            renderColumn(g, cat, cx, mx, my, cfg, accent);
        }

        renderWaifu(g);

        g.drawString(font, "§7Right Ctrl to close", 4, height - 10, 0xFFAAAAAA, false);
    }

    private void renderColumn(GuiGraphics g, Category cat, int cx, int mx, int my, ModConfig cfg, int accent) {
        int colH = HDR_H;
        for (ModEntry m : mods) {
            if (m.cat() != cat) continue;
            colH += ROW_H;
            if (expanded.contains(m.name())) colH += m.settings().size() * SET_H + 2;
        }

        g.fill(cx, COL_Y, cx + COL_W, COL_Y + colH, argb(180, 10, 10, 10));
        g.fill(cx, COL_Y, cx + COL_W, COL_Y + HDR_H, accent);
        g.drawCenteredString(font, cat.label, cx + COL_W / 2, COL_Y + (HDR_H - 8) / 2, 0xFFFFFFFF);

        int y = COL_Y + HDR_H;
        for (ModEntry m : mods) {
            if (m.cat() != cat) continue;
            y = renderRow(g, m, cx, y, mx, my, cfg, accent);
        }
    }

    private int renderRow(GuiGraphics g, ModEntry m, int cx, int y, int mx, int my, ModConfig cfg, int accent) {
        boolean on  = m.toggleable() && m.enabled().getAsBoolean();
        boolean hov = mx >= cx && mx < cx + COL_W && my >= y && my < y + ROW_H;
        boolean exp = expanded.contains(m.name());

        int bg = on  ? accent(cfg, 100)
               : hov ? argb(50, 255, 255, 255)
               :        argb(20, 255, 255, 255);
        g.fill(cx, y, cx + COL_W, y + ROW_H, bg);

        if (on) g.fill(cx, y, cx + 3, y + ROW_H, accent);

        int textCol = on ? 0xFFFFFFFF : 0xFFAAAAAA;
        g.drawString(font, m.name(), cx + INDENT + (on ? 3 : 0), y + (ROW_H - 8) / 2, textCol, false);

        if (!m.settings().isEmpty()) {
            String arr = exp ? "v" : ">";
            g.drawString(font, arr, cx + COL_W - 12, y + (ROW_H - 8) / 2, 0xFF666666, false);
        }

        y += ROW_H;

        if (exp) {
            for (int si = 0; si < m.settings().size(); si++) {
                Setting s = m.settings().get(si);
                g.fill(cx, y, cx + COL_W, y + SET_H, argb(120, 5, 5, 5));

                if (s instanceof SliderS sl)    renderSlider(g, sl, cx, y, cfg);
                else if (s instanceof BoolS bs) renderBool(g, bs, cx, y);
                else if (s instanceof KeyS ks)  renderKey(g, ks, m, si, cx, y);

                y += SET_H;
            }
            g.fill(cx, y, cx + COL_W, y + 2, accent(cfg, 80));
            y += 2;
        }

        return y;
    }

    private void renderSlider(GuiGraphics g, SliderS sl, int cx, int y, ModConfig cfg) {
        int accent = accent(cfg, 255);
        double val = sl.get().getAsDouble();
        double pct = Math.max(0, Math.min(1, (val - sl.min()) / (sl.max() - sl.min())));

        int tx = cx + INDENT;
        int tw = COL_W - INDENT * 2 - 28;
        int ty = y + SET_H / 2;

        g.drawString(font, sl.label(), tx, y + 1, 0xFF888888, false);
        g.fill(tx, ty, tx + tw, ty + 2, argb(80, 255, 255, 255));
        g.fill(tx, ty, tx + (int)(tw * pct), ty + 2, accent);
        int hx = tx + (int)(tw * pct);
        g.fill(hx - 2, y + 2, hx + 2, y + SET_H - 2, 0xFFFFFFFF);

        String valStr = isIntSlider(sl)
                      ? String.valueOf((int)Math.round(val))
                      : String.format("%.1f", val);
        g.drawString(font, valStr, cx + COL_W - 26, y + 1, 0xFFCCCCCC, false);
    }

    private void renderBool(GuiGraphics g, BoolS bs, int cx, int y) {
        boolean on = bs.get().getAsBoolean();
        g.drawString(font, bs.label(), cx + INDENT, y + 1, 0xFF888888, false);
        String tag = on ? "§aON" : "§cOFF";
        g.drawString(font, tag, cx + COL_W - 28, y + 1, 0xFFFFFFFF, false);
    }

    private void renderKey(GuiGraphics g, KeyS ks, ModEntry m, int si, int cx, int y) {
        boolean waiting = m.name().equals(rebindMod) && rebindIdx == si;
        g.drawString(font, ks.label(), cx + INDENT, y + 1, 0xFF888888, false);
        String keyTxt = waiting ? "§e..." : "§7[" + shortKey(ks.get().getAsInt()) + "]";
        g.drawString(font, keyTxt, cx + COL_W - 40, y + 1, 0xFFCCCCCC, false);
    }

    // ── Waifu rendering ───────────────────────────────────────────────────────
    private void renderWaifu(GuiGraphics g) {
        if (waifuLoc == null || waifuTexW <= 0 || waifuTexH <= 0) {
            String hint = waifuErr != null
                ? "§c[waifu] " + waifuErr + " — 放在 " + waifuHint
                : "§8[waifu: " + (waifuHint != null ? waifuHint : "config/valencia") + File.separator + "waifu.png]";
            g.drawString(font, hint, 4, height - 20, 0xFF888888, false);
            return;
        }
        try {
            int dispH = Math.min(height / 3, 150);
            int dispW = waifuTexW * dispH / waifuTexH;
            // 1.21.11 signature: blit(Identifier, x1, y1, x2, y2, u1, u2, v1, v2)
            // — corners, not (x,y,w,h). UV are normalized 0..1, not pixels.
            int x1 = 4;
            int y1 = height - dispH - 14;
            g.blit(waifuLoc, x1, y1, x1 + dispW, y1 + dispH, 0f, 1f, 0f, 1f);
        } catch (Throwable t) {
            g.drawString(font, "§c[waifu render err] " + t.getClass().getSimpleName(),
                4, height - 20, 0xFF888888, false);
        }
    }

    // ── Mouse ─────────────────────────────────────────────────────────────────
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean dbl) {
        int mx = (int)event.x(), my = (int)event.y(), btn = event.button();
        if (btn != 0) return super.mouseClicked(event, dbl);

        ModConfig cfg = ModConfig.get();
        int totalW = Category.values().length * COL_W + (Category.values().length - 1) * COL_GAP;
        int startX = (width - totalW) / 2;

        for (Category cat : Category.values()) {
            int cx = startX + cat.ordinal() * (COL_W + COL_GAP);
            int y  = COL_Y + HDR_H;

            for (ModEntry m : mods) {
                if (m.cat() != cat) continue;

                if (mx >= cx && mx < cx + COL_W && my >= y && my < y + ROW_H) {
                    boolean hasSettings = !m.settings().isEmpty();
                    boolean clickArrow  = hasSettings && mx >= cx + COL_W - 16;

                    if (clickArrow || !m.toggleable()) {
                        if (expanded.contains(m.name())) expanded.remove(m.name());
                        else expanded.add(m.name());
                    } else {
                        m.toggle().run();
                        saveEnabled(cfg);
                    }
                    return true;
                }
                y += ROW_H;

                if (expanded.contains(m.name())) {
                    for (int si = 0; si < m.settings().size(); si++) {
                        if (my >= y && my < y + SET_H && mx >= cx && mx < cx + COL_W) {
                            Setting s = m.settings().get(si);
                            if (s instanceof SliderS sl) {
                                int tx = cx + INDENT;
                                int tw = COL_W - INDENT * 2 - 28;
                                dragMod = m.name(); dragIdx = si;
                                applySlider(sl, mx, tx, tw);
                            } else if (s instanceof BoolS bs) {
                                bs.toggle().run();
                            } else if (s instanceof KeyS) {
                                rebindMod = m.name(); rebindIdx = si;
                            }
                            return true;
                        }
                        y += SET_H;
                    }
                    y += 2;
                }
            }
        }
        return super.mouseClicked(event, dbl);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragMod != null) {
            int mx = (int)event.x();
            int totalW = Category.values().length * COL_W + (Category.values().length - 1) * COL_GAP;
            int startX = (width - totalW) / 2;

            for (ModEntry m : mods) {
                if (!m.name().equals(dragMod)) continue;
                int cx = startX + m.cat().ordinal() * (COL_W + COL_GAP);
                if (dragIdx >= 0 && dragIdx < m.settings().size()) {
                    Setting s = m.settings().get(dragIdx);
                    if (s instanceof SliderS sl) {
                        int tx = cx + INDENT, tw = COL_W - INDENT * 2 - 28;
                        applySlider(sl, mx, tx, tw);
                    }
                }
            }
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragMod = null; dragIdx = -1;
        return super.mouseReleased(event);
    }

    // ── Keyboard ──────────────────────────────────────────────────────────────
    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (rebindMod != null) {
            if (key != GLFW.GLFW_KEY_ESCAPE) {
                for (ModEntry m : mods) {
                    if (!m.name().equals(rebindMod)) continue;
                    if (rebindIdx >= 0 && rebindIdx < m.settings().size()) {
                        Setting s = m.settings().get(rebindIdx);
                        if (s instanceof KeyS ks) ks.set().accept(key);
                    }
                }
            }
            rebindMod = null; rebindIdx = -1;
            return true;
        }
        if (key == ModConfig.get().guiKey) { onClose(); return true; }
        return super.keyPressed(event);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void applySlider(SliderS sl, int mx, int tx, int tw) {
        double pct = Math.max(0, Math.min(1, (double)(mx - tx) / tw));
        double raw = sl.min() + pct * (sl.max() - sl.min());
        // Treat as integer slider only when both bounds are whole numbers AND
        // the range has enough steps to justify int-snapping. A 0–1 range with
        // int snap collapses to just {0, 1} which is useless for floats like
        // Tower Spd (which wants 0.1 granularity).
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
