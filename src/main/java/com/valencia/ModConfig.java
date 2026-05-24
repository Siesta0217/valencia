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

    // Theme colors
    public int accentR  = 0;
    public int accentG  = 170;
    public int accentB  = 255;
    public int bgAlpha  = 160;

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

    // ESP
    public boolean espEnabled = false;
    public boolean espPlayers = true;
    public boolean espHostile = false;
    public boolean espAnimals = false;
    public boolean espItems   = false;
    public boolean espShowBox = false;
    public int     espBoxColor = 0xFF80FF40;

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

    public static String keyName(int glfwKey) {
        for (Field f : GLFW.class.getFields()) {
            if (f.getName().startsWith("GLFW_KEY_")) {
                try {
                    if (f.getInt(null) == glfwKey)
                        return f.getName().replace("GLFW_KEY_", "");
                } catch (Exception ignored) {}
            }
        }
        return "KEY_" + glfwKey;
    }
}
