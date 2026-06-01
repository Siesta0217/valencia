package com.valencia;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Path;

public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("valencia.json");

    public int nofallKey   = GLFW.GLFW_KEY_N;
    public int xrayKey     = GLFW.GLFW_KEY_X;
    public int maceAuraKey = GLFW.GLFW_KEY_Z;
    public int noSlowKey   = GLFW.GLFW_KEY_G;
    public int guiKey      = GLFW.GLFW_KEY_RIGHT_CONTROL;
    public int bhopKey       = GLFW.GLFW_KEY_B;
    public int stepKey       = GLFW.GLFW_KEY_H;
    public int killAuraKey   = GLFW.GLFW_KEY_K;
    public int velocityKey   = GLFW.GLFW_KEY_C;
    public int fastPlaceKey  = GLFW.GLFW_KEY_F;
    public int critKey       = GLFW.GLFW_KEY_R;
    public int scaffoldKey   = GLFW.GLFW_KEY_J;
    public int timerKey      = GLFW.GLFW_KEY_T;
    public int spearAuraKey  = GLFW.GLFW_KEY_U;
    public int nameTagKey    = GLFW.GLFW_KEY_Y;
    public int autoTotemKey  = GLFW.GLFW_KEY_O;
    public int flyKey        = GLFW.GLFW_KEY_V;
    public int panicKey      = GLFW.GLFW_KEY_DELETE;

    // Fly (motion-based, server-usable)
    public boolean flyEnabled = false;
    public float   flyHSpeed  = 1.0f;
    public float   flyVSpeed  = 1.0f;

    // NoFall: 0=Always spoof onGround, 1=Smart (only when a fall would deal damage)
    public int nofallMode = 0;
    // Reset the vanilla server's airborne counter periodically so sustained
    // flight/hover isn't kicked as "Flying is not enabled" (allow-flight=false).
    public boolean nofallNoFlightKick = true;

    // SpearAura tuning
    public float spearScanRange   = 7.0f;
    public float spearMinReach    = 1.6f;
    public float spearMaxReach    = 5.5f;
    public int   spearMode        = 2;    // 0=Jab, 1=Charge, 2=Auto
    public boolean spearPlayers   = true;
    public boolean spearHostile   = true;
    public boolean spearAnimals   = false;
    public boolean spearStepBack  = true;
    public int   spearChargeTicks = 12;
    public boolean spearAuraEnabled = false;
    public boolean spearRaycast   = true;
    public boolean spearSkipInvis = true;
    public boolean spearSmoothRot = true;
    public int     spearMaxTurn   = 60;

    // NoCrash (elytra wall damage mitigation)
    public boolean noCrashEnabled  = false;
    public float   noCrashLookAhead = 4.0f;
    public float   noCrashMaxSpeed  = 0.4f;

    // Hitbox (entity bounding box expansion)
    public boolean hitboxEnabled = false;
    public float   hitboxExpand  = 0.3f;
    public boolean hitboxPlayers = true;
    public boolean hitboxHostile = true;
    public boolean hitboxAnimals = true;

    // MaceAura tuning
    public float maceDetectRange = 6.0f;
    public float maceAttackRange = 3.5f;
    public boolean maceHostile   = true;
    public boolean maceAnimals   = false;
    public boolean macePlayers   = true;
    public boolean maceRaycast   = true;
    public boolean maceSkipInvis = true;
    public boolean maceSmoothRot = true;
    public int     maceMaxTurn   = 60;

    // Step tuning
    public float stepHeight = 1.0f;

    // KillAura tuning
    public float killRange       = 4.0f;
    public float killAttackRange = 3.0f;
    public boolean killHostile   = true;
    public boolean killAnimals   = false;
    public boolean killPlayers   = false;

    // BHop tuning
    public float   bhopSpeed      = 1.0f;
    public boolean bhopLowHop     = false;
    public float   bhopJumpHeight = 0.5f;
    public float   bhopBoost      = 1.0f;
    public boolean bhopKBBoost    = false;

    // KillAura mode
    public boolean killSingle      = false;
    public int     killAttackDelay = 10;

    // KillAura behavior
    public boolean killRaycast    = true;
    public boolean killSkipInvis  = true;
    public boolean killWaitCool   = true;
    public boolean killSmoothRot  = true;
    public int     killMaxTurn    = 60;
    public boolean killBodyLock   = false;
    public boolean killVisBody    = false;
    public boolean killGcd        = true;   // GCD-snap silent-aim rotation
    public int     killCpsJitter  = 0;      // random ticks added to attack delay

    // Theme colors
    public int accentR  = 0;
    public int accentG  = 170;
    public int accentB  = 255;
    public int bgAlpha  = 160;

    // ClickGUI skin (colors): 0=Dark (Raven, original), 1=Light, 2=Glass
    public int guiStyle = 0;
    // ClickGUI layout: 0=Panels (Raven scattered, original), 1=Sidebar window
    public int guiLayout = 0;

    // Module enabled states (persisted across sessions)
    public boolean nofallEnabled     = true;
    public boolean xrayEnabled       = false;
    public boolean maceAuraEnabled   = false;
    public boolean noSlowEnabled     = false;
    public boolean bhopEnabled       = false;
    public boolean stepEnabled       = false;
    public boolean killAuraEnabled   = false;
    public boolean velocityEnabled   = false;
    public boolean fastPlaceEnabled  = false;
    public boolean critEnabled       = false;
    public boolean scaffoldEnabled   = false;
    public boolean timerEnabled      = false;

    // Timer tuning
    public float timerSpeed = 2.0f;

    // ElytraGoto tuning
    public float elytraSafeHp = 4.0f;

    // NetherCoord HUD
    public boolean netherCoordEnabled = true;

    // AutoFish
    public boolean autoFishEnabled = false;
    public float   autoFishBiteVy  = -0.04f;
    public int     autoFishRecast  = 12;

    // NameTag
    public boolean nameTagEnabled        = false;
    public boolean nameTagPlayers        = true;
    public boolean nameTagHostile        = false;
    public boolean nameTagAnimals        = false;
    public boolean nameTagShowArmor      = true;
    public boolean nameTagShowHands      = true;
    public boolean nameTagShowDurability = true;
    public boolean nameTagShowHpBar      = true;
    public boolean nameTagShowHpText     = true;
    public float   nameTagScale          = 1.0f;
    public float   nameTagMaxDistance    = 64.0f;
    public boolean nameTagUseTheme       = true;
    public int     nameTagR              = 0;
    public int     nameTagG              = 170;
    public int     nameTagB              = 255;

    // ESP
    public boolean espEnabled  = false;
    public boolean espPlayers  = true;
    public boolean espHostile  = false;
    public boolean espAnimals  = false;
    public boolean espItems    = false;
    public int     espLineThick    = 1;
    public float   espMaxDistance  = 80.0f;
    public boolean espShowName     = true;
    public boolean espShowHp       = true;
    public boolean espShowDistance = true;
    public boolean espShowTracer   = false;
    public boolean espGlow         = false;
    public int     espRed          = 255;
    public int     espGreen        = 255;
    public int     espBlue         = 255;

    // TargetHUD
    public boolean targetHudEnabled = false;
    public int     targetHudStyle   = 0;   // 0=Classic, 1=Compact, 2=Gradient

    // AutoTotem
    public boolean autoTotemEnabled = false;

    // ArrayList HUD
    public boolean arrayListEnabled    = false;
    public boolean arrayListRainbow    = true;
    public boolean arrayListBackground = true;

    // Scaffold tuning
    public boolean scaffoldTower      = false;
    public boolean scaffoldTowerMove  = true;
    public float   scaffoldTowerSpeed = 0.5f;
    public boolean scaffoldAutoSwitch = true;
    public boolean scaffoldSwitchBack = true;
    public int     scaffoldPlaceDelay = 0;
    public boolean scaffoldSilentRot  = true;
    public boolean scaffoldFakeHand   = false;

    // Velocity tuning
    public int velocityHoriz = 0;
    public int velocityVert  = 0;

    private static ModConfig instance;

    public static ModConfig get() {
        if (instance == null) instance = load();
        return instance;
    }

    private static ModConfig load() {
        File file = CONFIG_PATH.toFile();
        if (file.exists()) {
            try (Reader r = new FileReader(file)) {
                ModConfig cfg = GSON.fromJson(r, ModConfig.class);
                if (cfg != null) return cfg;
            } catch (Exception ignored) {}
        }
        ModConfig def = new ModConfig();
        def.save();
        return def;
    }

    public void save() {
        try (Writer w = new FileWriter(CONFIG_PATH.toFile())) {
            GSON.toJson(this, w);
        } catch (Exception ignored) {}
    }

    // GLFW_KEY_* reflection is expensive and was previously run per-frame for
    // every KeyS widget in the ClickGUI. Reflect the constants once into two
    // lookup maps and serve every call from cache.
    private static java.util.Map<Integer, String> keyCodeToName;
    private static java.util.Map<String, Integer> keyNameToCode;

    private static void buildKeyMaps() {
        java.util.Map<Integer, String> c2n = new java.util.HashMap<>();
        java.util.Map<String, Integer> n2c = new java.util.HashMap<>();
        for (Field f : GLFW.class.getFields()) {
            String n = f.getName();
            if (!n.startsWith("GLFW_KEY_")) continue;
            try {
                int code = f.getInt(null);
                String name = n.substring("GLFW_KEY_".length());
                c2n.putIfAbsent(code, name);   // first name wins (matches old behavior)
                n2c.put(name, code);
            } catch (Exception ignored) {}
        }
        keyCodeToName = c2n;
        keyNameToCode = n2c;
    }

    public static String keyName(int glfwKey) {
        if (keyCodeToName == null) buildKeyMaps();
        String name = keyCodeToName.get(glfwKey);
        return name != null ? name : "KEY_" + glfwKey;
    }

    /** Resolve a GLFW key name (without the GLFW_KEY_ prefix) to its code, or -1. */
    public static int keyCode(String name) {
        if (keyNameToCode == null) buildKeyMaps();
        Integer code = keyNameToCode.get(name);
        return code != null ? code : -1;
    }
}
