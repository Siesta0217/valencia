package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;

/**
 * Central keybind poller — edge-detects every configured toggle key once per
 * client tick and flips the matching module.
 *
 * Replaces the 15 hand-written {@code prevX} fields + parallel read/toggle/save
 * blocks that used to live in {@code TickMixin}. Adding a module is now a single
 * row in {@link #TOGGLES}; the {@code prev} state lives in one array. The GUI
 * open/close and Panic edges are handled alongside the table.
 */
public final class Keybinds {

    private record Toggle(IntSupplier key, BooleanSupplier enabled, Runnable toggle, String label) {}

    private static ModConfig cfg() { return ModConfig.get(); }

    private static final List<Toggle> TOGGLES = List.of(
        new Toggle(() -> cfg().nofallKey,    NoFallMod::isEnabled,    NoFallMod::toggleManual, "NoFall"),
        new Toggle(() -> cfg().xrayKey,      XRayMod::isEnabled,      XRayMod::toggle,         "XRay"),
        new Toggle(() -> cfg().maceAuraKey,  MaceAuraMod::isEnabled,  MaceAuraMod::toggle,     "MaceAura"),
        new Toggle(() -> cfg().noSlowKey,    NoSlowMod::isEnabled,    NoSlowMod::toggle,       "NoSlow"),
        new Toggle(() -> cfg().bhopKey,      BHopMod::isEnabled,      BHopMod::toggle,         "BHop"),
        new Toggle(() -> cfg().stepKey,      StepMod::isEnabled,      StepMod::toggle,         "Step"),
        new Toggle(() -> cfg().killAuraKey,  KillAuraMod::isEnabled,  KillAuraMod::toggle,     "KillAura"),
        new Toggle(() -> cfg().velocityKey,  VelocityMod::isEnabled,  VelocityMod::toggle,     "Velocity"),
        new Toggle(() -> cfg().fastPlaceKey, FastPlaceMod::isEnabled, FastPlaceMod::toggle,    "FastPlace"),
        new Toggle(() -> cfg().critKey,      CritMod::isEnabled,      CritMod::toggle,         "CritHit"),
        new Toggle(() -> cfg().scaffoldKey,  ScaffoldMod::isEnabled,  ScaffoldMod::toggle,     "Scaffold"),
        new Toggle(() -> cfg().timerKey,     TimerMod::isEnabled,     TimerMod::toggle,        "Timer"),
        new Toggle(() -> cfg().spearAuraKey, SpearAuraMod::isEnabled, SpearAuraMod::toggle,    "SpearAura"),
        new Toggle(() -> cfg().nameTagKey,   NameTagMod::isEnabled,   NameTagMod::toggle,      "NameTag"),
        new Toggle(() -> cfg().autoTotemKey, AutoTotemMod::isEnabled, AutoTotemMod::toggle,    "AutoTotem"),
        new Toggle(() -> cfg().flyKey,       FlyMod::isEnabled,       FlyMod::toggle,          "Fly")
    );

    /** Minimal (label, enabled) pair exposed for HUDs such as the ArrayList. */
    public record ModuleEntry(String label, BooleanSupplier enabled) {}

    /**
     * Every key-toggleable module as (label, enabled). The ArrayList HUD reads
     * this instead of hand-copying the roster, so adding a row to {@link #TOGGLES}
     * automatically surfaces it there too — no parallel list to keep in sync.
     */
    public static final List<ModuleEntry> TOGGLE_ENTRIES =
        TOGGLES.stream().map(t -> new ModuleEntry(t.label(), t.enabled())).toList();

    private static final boolean[] prevToggle = new boolean[TOGGLES.size()];
    private static boolean prevGui   = false;
    private static boolean prevPanic = false;

    private Keybinds() {}

    /** Called once per client tick from TickMixin. */
    public static void poll(Minecraft mc) {
        long handle = GLFW.glfwGetCurrentContext();
        if (handle == 0L) return;
        ModConfig cfg = ModConfig.get();

        boolean guiDown   = down(handle, cfg.guiKey);
        boolean panicDown = down(handle, cfg.panicKey);

        if (mc.screen == null) {
            for (int i = 0; i < TOGGLES.size(); i++) {
                Toggle t = TOGGLES.get(i);
                boolean d = down(handle, t.key().getAsInt());
                if (d && !prevToggle[i]) {
                    t.toggle().run();
                    saveEnabled();
                    msg(mc, "§7[" + t.label() + "] " + state(t.enabled().getAsBoolean()));
                }
                prevToggle[i] = d;
            }
            if (panicDown && !prevPanic) panic(mc);
            if (guiDown && !prevGui) mc.setScreen(new ClickGuiScreen());
        } else {
            // Keep prev states fresh while a screen is open so closing it and
            // releasing the key doesn't fire a stale edge.
            for (int i = 0; i < TOGGLES.size(); i++)
                prevToggle[i] = down(handle, TOGGLES.get(i).key().getAsInt());
            if (mc.screen instanceof ClickGuiScreen && guiDown && !prevGui) mc.setScreen(null);
        }

        prevGui   = guiDown;
        prevPanic = panicDown;
    }

    /** Panic: kill every gameplay-affecting module in one keypress (go legit). */
    private static void panic(Minecraft mc) {
        if (NoFallMod.isEnabled())     NoFallMod.toggleManual();
        if (KillAuraMod.isEnabled())   KillAuraMod.toggle();
        if (MaceAuraMod.isEnabled())   MaceAuraMod.toggle();
        if (SpearAuraMod.isEnabled())  SpearAuraMod.toggle();
        if (CritMod.isEnabled())       CritMod.toggle();
        if (ScaffoldMod.isEnabled())   ScaffoldMod.toggle();
        if (TimerMod.isEnabled())      TimerMod.toggle();
        if (BHopMod.isEnabled())       BHopMod.toggle();
        if (StepMod.isEnabled())       StepMod.toggle();
        if (VelocityMod.isEnabled())   VelocityMod.toggle();
        if (FastPlaceMod.isEnabled())  FastPlaceMod.toggle();
        if (NoSlowMod.isEnabled())     NoSlowMod.toggle();
        if (AutoTotemMod.isEnabled())  AutoTotemMod.toggle();
        if (FlyMod.isEnabled())        FlyMod.toggle();
        if (ElytraGotoMod.isEnabled()) ElytraGotoMod.toggle();
        if (AutoFishMod.isEnabled())   AutoFishMod.toggle();
        if (NoCrashMod.isEnabled())    NoCrashMod.toggle();
        saveEnabled();
        msg(mc, "§c[Panic] all modules OFF");
    }

    /** Persist the enabled state of every key-toggleable module. */
    private static void saveEnabled() {
        ModConfig cfg = ModConfig.get();
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
        cfg.spearAuraEnabled  = SpearAuraMod.isEnabled();
        cfg.nameTagEnabled    = NameTagMod.isEnabled();
        cfg.autoTotemEnabled  = AutoTotemMod.isEnabled();
        cfg.flyEnabled        = FlyMod.isEnabled();
        cfg.save();
    }

    private static boolean down(long handle, int key) {
        return key > 0 && GLFW.glfwGetKey(handle, key) == GLFW.GLFW_PRESS;
    }

    private static String state(boolean on) { return on ? "§aON" : "§cOFF"; }

    private static void msg(Minecraft mc, String text) {
        if (mc.player != null) mc.player.displayClientMessage(Component.literal(text), true);
    }
}
