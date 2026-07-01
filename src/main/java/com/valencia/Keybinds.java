package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.BooleanSupplier;

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

    /** Minimal (label, enabled) pair exposed for HUDs such as the ArrayList. */
    public record ModuleEntry(String label, BooleanSupplier enabled) {}

    /**
     * Every key-toggleable module as (label, enabled) — derived straight from
     * {@link Modules#KEYED}, so this can never drift from the real roster.
     */
    public static final List<ModuleEntry> TOGGLE_ENTRIES =
        Modules.KEYED.stream().map(d -> new ModuleEntry(d.label(), d.enabled())).toList();

    private static final boolean[] prevToggle = new boolean[Modules.KEYED.size()];
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
            for (int i = 0; i < Modules.KEYED.size(); i++) {
                Modules.ModuleDef t = Modules.KEYED.get(i);
                boolean d = down(handle, t.keyGet().getAsInt());
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
            for (int i = 0; i < Modules.KEYED.size(); i++)
                prevToggle[i] = down(handle, Modules.KEYED.get(i).keyGet().getAsInt());
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
        if (FreecamMod.isEnabled())    FreecamMod.toggle();
        if (ElytraGotoMod.isEnabled()) ElytraGotoMod.toggle();
        if (AutoFishMod.isEnabled())   AutoFishMod.toggle();
        if (NoCrashMod.isEnabled())    NoCrashMod.toggle();
        if (FastBreakMod.isEnabled())  FastBreakMod.toggle();
        if (AutoToolMod.isEnabled())   AutoToolMod.toggle();
        if (NukerMod.isEnabled())      NukerMod.toggle();
        if (AutoWalkMod.isEnabled())   AutoWalkMod.toggle();
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
        cfg.fastBreakEnabled  = FastBreakMod.isEnabled();
        cfg.autoToolEnabled   = AutoToolMod.isEnabled();
        cfg.nukerEnabled      = NukerMod.isEnabled();
        cfg.autoWalkEnabled   = AutoWalkMod.isEnabled();
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
