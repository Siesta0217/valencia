package com.valencia;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * Shared ClickGUI data model — extracted verbatim from ClickGuiScreen so every
 * layout (Panels / Sidebar / Tenacity / Aurora) and the ModuleRegistry can
 * reference the same types. Package-private, no behaviour.
 */
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

class ModEntry {
    final String name;
    final BooleanSupplier enabled;
    final Runnable toggle;
    final boolean toggleable;
    final List<Setting> settings;
    ModEntry(String n, BooleanSupplier e, Runnable t, boolean tog, List<Setting> s) {
        name = n; enabled = e; toggle = t; toggleable = tog; settings = s;
    }
}

class Panel {
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
