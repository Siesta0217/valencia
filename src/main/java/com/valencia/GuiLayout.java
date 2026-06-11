package com.valencia;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * One ClickGUI presentation. {@link ClickGuiScreen} owns one instance per style
 * (Panels / Sidebar / Tenacity / Aurora), keeps the shared module data + skin,
 * and forwards render/input to whichever layout {@code cfg.guiLayout} selects.
 * Each layout holds its own window position, scroll and drag state.
 *
 * Mouse methods take already-unpacked int coords; returning {@code true} means
 * the event was consumed.
 */
interface GuiLayout {
    void render(GuiGraphics g, int mx, int my, int accent, Font font);
    boolean mouseClicked(int mx, int my, int btn);
    boolean mouseDragged(int mx, int my);
    void mouseReleased();
    boolean mouseScrolled(int mx, int my, double scrollY);
}
