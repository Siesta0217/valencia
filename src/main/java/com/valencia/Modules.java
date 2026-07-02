package com.valencia;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Central module registry — the single source of truth for the module roster.
 *
 * Before this existed, four hand-copied lists had to stay in sync by eye:
 * Keybinds.TOGGLES, Keybinds.panic(), Keybinds.saveEnabled(), and the enable
 * restores in NoFallMod.onInitializeClient() (plus ArrayListMod.EXTRAS).
 * Adding a module meant touching 4–5 places and the compiler caught none of
 * the omissions. Every one of those is now a derived view of {@link #ALL}.
 *
 * Design rules (do not break):
 *  - Modules stay static-utility classes; a {@link ModuleDef} only holds
 *    method refs to the existing static getters/toggles.
 *  - ALWAYS go through {@code toggle()} — never flip a boolean directly.
 *    Some toggles have side effects (XRay rebuilds chunks + sets gamma,
 *    AutoWalk releases the held key, Freecam swaps the camera).
 *  - Config access inside lambdas must be lazy ({@code () -> cfg().x}), never
 *    captured at list-build time.
 *  - {@code panicable} is a curated set, not "has a keybind": panic kills
 *    gameplay modules (incl. keyless ElytraGoto/AutoFish/NoCrash) but leaves
 *    visuals (XRay/NameTag/ESP/HUDs) on — that's intentional.
 *  - Per-module tuning fields (~120 slider/color restores in NoFallMod) are
 *    deliberately NOT in the registry — they sit next to aura tuning, which
 *    is hands-off territory.
 */
public final class Modules {

    /** One module's identity + everything the derived views need. */
    public record ModuleDef(
        String label, Cat category,
        BooleanSupplier enabled, Runnable toggle,
        IntSupplier keyGet, IntConsumer keySet,              // null ⇒ no keybind
        BooleanSupplier persistGet, Consumer<Boolean> persistSet,
        String persistField,                                 // ModConfig field name; null ⇒ not persisted
        boolean panicable,
        boolean surfaceInArrayList                           // keyless modules the ArrayList HUD still shows
    ) {
        public boolean hasKey()    { return keyGet != null; }
        public boolean persisted() { return persistGet != null; }
    }

    private static ModConfig cfg() { return ModConfig.get(); }

    /**
     * The roster. Key-toggleable modules first, in the exact order the old
     * Keybinds.TOGGLES had (KEYED preserves this order for the poll array);
     * keyless modules after.
     */
    public static final List<ModuleDef> ALL = List.of(
        // ── key-toggleable (order = old Keybinds.TOGGLES) ──────────────────
        new ModuleDef("NoFall",    Cat.MOVEMENT, NoFallMod::isEnabled,    NoFallMod::toggleManual,
            () -> cfg().nofallKey,    v -> cfg().nofallKey = v,
            () -> cfg().nofallEnabled,    v -> cfg().nofallEnabled = v,    "nofallEnabled",    true,  false),
        new ModuleDef("XRay",      Cat.RENDER,   XRayMod::isEnabled,      XRayMod::toggle,
            () -> cfg().xrayKey,      v -> cfg().xrayKey = v,
            () -> cfg().xrayEnabled,      v -> cfg().xrayEnabled = v,      "xrayEnabled",      false, false),
        new ModuleDef("MaceAura",  Cat.COMBAT,   MaceAuraMod::isEnabled,  MaceAuraMod::toggle,
            () -> cfg().maceAuraKey,  v -> cfg().maceAuraKey = v,
            () -> cfg().maceAuraEnabled,  v -> cfg().maceAuraEnabled = v,  "maceAuraEnabled",  true,  false),
        new ModuleDef("NoSlow",    Cat.MOVEMENT, NoSlowMod::isEnabled,    NoSlowMod::toggle,
            () -> cfg().noSlowKey,    v -> cfg().noSlowKey = v,
            () -> cfg().noSlowEnabled,    v -> cfg().noSlowEnabled = v,    "noSlowEnabled",    true,  false),
        new ModuleDef("BHop",      Cat.MOVEMENT, BHopMod::isEnabled,      BHopMod::toggle,
            () -> cfg().bhopKey,      v -> cfg().bhopKey = v,
            () -> cfg().bhopEnabled,      v -> cfg().bhopEnabled = v,      "bhopEnabled",      true,  false),
        new ModuleDef("Step",      Cat.MOVEMENT, StepMod::isEnabled,      StepMod::toggle,
            () -> cfg().stepKey,      v -> cfg().stepKey = v,
            () -> cfg().stepEnabled,      v -> cfg().stepEnabled = v,      "stepEnabled",      true,  false),
        new ModuleDef("KillAura",  Cat.COMBAT,   KillAuraMod::isEnabled,  KillAuraMod::toggle,
            () -> cfg().killAuraKey,  v -> cfg().killAuraKey = v,
            () -> cfg().killAuraEnabled,  v -> cfg().killAuraEnabled = v,  "killAuraEnabled",  true,  false),
        new ModuleDef("Velocity",  Cat.MOVEMENT, VelocityMod::isEnabled,  VelocityMod::toggle,
            () -> cfg().velocityKey,  v -> cfg().velocityKey = v,
            () -> cfg().velocityEnabled,  v -> cfg().velocityEnabled = v,  "velocityEnabled",  true,  false),
        new ModuleDef("FastPlace", Cat.MOVEMENT, FastPlaceMod::isEnabled, FastPlaceMod::toggle,
            () -> cfg().fastPlaceKey, v -> cfg().fastPlaceKey = v,
            () -> cfg().fastPlaceEnabled, v -> cfg().fastPlaceEnabled = v, "fastPlaceEnabled", true,  false),
        new ModuleDef("CritHit",   Cat.COMBAT,   CritMod::isEnabled,      CritMod::toggle,
            () -> cfg().critKey,      v -> cfg().critKey = v,
            () -> cfg().critEnabled,      v -> cfg().critEnabled = v,      "critEnabled",      true,  false),
        new ModuleDef("Scaffold",  Cat.PLAYER,   ScaffoldMod::isEnabled,  ScaffoldMod::toggle,
            () -> cfg().scaffoldKey,  v -> cfg().scaffoldKey = v,
            () -> cfg().scaffoldEnabled,  v -> cfg().scaffoldEnabled = v,  "scaffoldEnabled",  true,  false),
        new ModuleDef("Timer",     Cat.MOVEMENT, TimerMod::isEnabled,     TimerMod::toggle,
            () -> cfg().timerKey,     v -> cfg().timerKey = v,
            () -> cfg().timerEnabled,     v -> cfg().timerEnabled = v,     "timerEnabled",     true,  false),
        new ModuleDef("SpearAura", Cat.COMBAT,   SpearAuraMod::isEnabled, SpearAuraMod::toggle,
            () -> cfg().spearAuraKey, v -> cfg().spearAuraKey = v,
            () -> cfg().spearAuraEnabled, v -> cfg().spearAuraEnabled = v, "spearAuraEnabled", true,  false),
        new ModuleDef("NameTag",   Cat.RENDER,   NameTagMod::isEnabled,   NameTagMod::toggle,
            () -> cfg().nameTagKey,   v -> cfg().nameTagKey = v,
            () -> cfg().nameTagEnabled,   v -> cfg().nameTagEnabled = v,   "nameTagEnabled",   false, false),
        new ModuleDef("AutoTotem", Cat.COMBAT,   AutoTotemMod::isEnabled, AutoTotemMod::toggle,
            () -> cfg().autoTotemKey, v -> cfg().autoTotemKey = v,
            () -> cfg().autoTotemEnabled, v -> cfg().autoTotemEnabled = v, "autoTotemEnabled", true,  false),
        new ModuleDef("Fly",       Cat.MOVEMENT, FlyMod::isEnabled,       FlyMod::toggle,
            () -> cfg().flyKey,       v -> cfg().flyKey = v,
            () -> cfg().flyEnabled,       v -> cfg().flyEnabled = v,       "flyEnabled",       true,  false),
        // Freecam: enabled state intentionally NOT persisted (don't boot into
        // a detached camera) → persist* = null.
        new ModuleDef("Freecam",   Cat.RENDER,   FreecamMod::isEnabled,   FreecamMod::toggle,
            () -> cfg().freecamKey,   v -> cfg().freecamKey = v,
            null, null, null, true, false),
        new ModuleDef("FastBreak", Cat.PLAYER,   FastBreakMod::isEnabled, FastBreakMod::toggle,
            () -> cfg().fastBreakKey, v -> cfg().fastBreakKey = v,
            () -> cfg().fastBreakEnabled, v -> cfg().fastBreakEnabled = v, "fastBreakEnabled", true,  false),
        new ModuleDef("AutoTool",  Cat.PLAYER,   AutoToolMod::isEnabled,  AutoToolMod::toggle,
            () -> cfg().autoToolKey,  v -> cfg().autoToolKey = v,
            () -> cfg().autoToolEnabled,  v -> cfg().autoToolEnabled = v,  "autoToolEnabled",  true,  false),
        new ModuleDef("Nuker",     Cat.PLAYER,   NukerMod::isEnabled,     NukerMod::toggle,
            () -> cfg().nukerKey,     v -> cfg().nukerKey = v,
            () -> cfg().nukerEnabled,     v -> cfg().nukerEnabled = v,     "nukerEnabled",     true,  false),
        new ModuleDef("AutoWalk",  Cat.MOVEMENT, AutoWalkMod::isEnabled,  AutoWalkMod::toggle,
            () -> cfg().autoWalkKey,  v -> cfg().autoWalkKey = v,
            () -> cfg().autoWalkEnabled,  v -> cfg().autoWalkEnabled = v,  "autoWalkEnabled",  true,  false),

        // ── keyless ────────────────────────────────────────────────────────
        // ElytraGoto: enabled comes from `.nf goto`, never persisted.
        new ModuleDef("ElytraGoto", Cat.PLAYER,  ElytraGotoMod::isEnabled, ElytraGotoMod::toggle,
            null, null, null, null, null, true, true),
        new ModuleDef("AutoFish",  Cat.PLAYER,   AutoFishMod::isEnabled,  AutoFishMod::toggle,
            null, null,
            () -> cfg().autoFishEnabled,  v -> cfg().autoFishEnabled = v,  "autoFishEnabled",  true,  true),
        new ModuleDef("NoCrash",   Cat.PLAYER,   NoCrashMod::isEnabled,   NoCrashMod::toggle,
            null, null,
            () -> cfg().noCrashEnabled,   v -> cfg().noCrashEnabled = v,   "noCrashEnabled",   true,  true),
        new ModuleDef("AutoEat",   Cat.PLAYER,   AutoEatMod::isEnabled,   AutoEatMod::toggle,
            null, null,
            () -> cfg().autoEatEnabled,   v -> cfg().autoEatEnabled = v,   "autoEatEnabled",   true,  true),
        new ModuleDef("Hitbox",    Cat.COMBAT,   HitboxMod::isEnabled,    HitboxMod::toggle,
            null, null,
            () -> cfg().hitboxEnabled,    v -> cfg().hitboxEnabled = v,    "hitboxEnabled",    false, true),
        new ModuleDef("ESP",       Cat.RENDER,   ESPMod::isEnabled,       ESPMod::toggle,
            null, null,
            () -> cfg().espEnabled,       v -> cfg().espEnabled = v,       "espEnabled",       false, true),
        new ModuleDef("DimCoord",  Cat.RENDER,   NetherCoordMod::isEnabled, NetherCoordMod::toggle,
            null, null,
            () -> cfg().netherCoordEnabled, v -> cfg().netherCoordEnabled = v, "netherCoordEnabled", false, false),
        new ModuleDef("TargetHUD", Cat.RENDER,   TargetHudMod::isEnabled, TargetHudMod::toggle,
            null, null,
            () -> cfg().targetHudEnabled, v -> cfg().targetHudEnabled = v, "targetHudEnabled", false, false),
        new ModuleDef("Waypoints", Cat.RENDER,   WaypointsMod::isEnabled, WaypointsMod::toggle,
            null, null,
            () -> cfg().waypointsEnabled, v -> cfg().waypointsEnabled = v, "waypointsEnabled", false, false),
        new ModuleDef("ArrayList", Cat.RENDER,   ArrayListMod::isEnabled, ArrayListMod::toggle,
            null, null,
            () -> cfg().arrayListEnabled, v -> cfg().arrayListEnabled = v, "arrayListEnabled", false, false)
    );

    /** Key-toggleable modules, stable order (drives the keypress poll array). */
    public static final List<ModuleDef> KEYED =
        ALL.stream().filter(ModuleDef::hasKey).toList();

    private Modules() {}

    /** Panic: kill every gameplay module (leave visuals on). */
    public static void panic() {
        for (ModuleDef d : ALL)
            if (d.panicable() && d.enabled().getAsBoolean()) d.toggle().run();
    }

    /** Persist the enabled state of every persisted module, then save. */
    public static void saveEnabled() {
        for (ModuleDef d : ALL)
            if (d.persisted()) d.persistSet().accept(d.enabled().getAsBoolean());
        cfg().save();
    }

    /** Restore enabled states from config (startup). Always via toggle(). */
    public static void restoreEnabled() {
        for (ModuleDef d : ALL)
            if (d.persisted() && d.persistGet().getAsBoolean() != d.enabled().getAsBoolean())
                d.toggle().run();
    }

    /**
     * Startup sanity check — the "compiler substitute" that keeps the registry
     * from becoming a fifth drifting list. Reflectively scans ModConfig for
     * boolean {@code *Enabled} fields and diffs them against the persistField
     * names declared here. Any mismatch (new module's config field not bound,
     * or a descriptor pointing at a renamed/removed field) is logged once.
     */
    public static void verify() {
        Set<String> cfgFields = new HashSet<>();
        for (Field f : ModConfig.class.getFields())
            if (f.getType() == boolean.class && f.getName().endsWith("Enabled"))
                cfgFields.add(f.getName());

        Set<String> bound = new HashSet<>();
        for (ModuleDef d : ALL)
            if (d.persistField() != null) bound.add(d.persistField());

        List<String> unbound = new ArrayList<>();
        for (String f : cfgFields) if (!bound.contains(f)) unbound.add(f);
        List<String> dangling = new ArrayList<>();
        for (String f : bound) if (!cfgFields.contains(f)) dangling.add(f);

        if (!unbound.isEmpty())
            System.err.println("[Valencia] Modules.verify: ModConfig fields not bound to any ModuleDef: " + unbound);
        if (!dangling.isEmpty())
            System.err.println("[Valencia] Modules.verify: ModuleDef persistField missing from ModConfig: " + dangling);
    }
}
