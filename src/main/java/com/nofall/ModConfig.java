package com.nofall;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.lwjgl.glfw.GLFW;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.file.Path;

public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("nofall.json");

    public int nofallKey   = GLFW.GLFW_KEY_N;
    public int xrayKey     = GLFW.GLFW_KEY_X;
    public int maceAuraKey = GLFW.GLFW_KEY_Z;
    public int fastFoodKey = GLFW.GLFW_KEY_G;
    public int guiKey      = GLFW.GLFW_KEY_RIGHT_SHIFT;

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
