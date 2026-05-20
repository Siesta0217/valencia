package com.valencia;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

public class ClickGuiScreen extends Screen {

    // Panel position (draggable)
    private int px = 30, py = 40;
    private boolean headerDrag;
    private int dragDx, dragDy;

    // Layout
    private static final int W        = 192;
    private static final int HEADER_H = 22;
    private static final int ROW_H    = 22;
    private static final int SUB_H    = 17;
    private static final int PAD      = 5;
    private static final int INDENT   = 8;
    private static final int KEY_W    = 36;
    private static final int KEY_H    = 13;
    private static final int ARR_W    = 10;
    private static final int VAL_W    = 28;

    // Colors ??Raven palette extended
    private static final int C_SHADOW   = 0x66000000;
    private static final int C_BG       = 0xEE0C0C1C;
    private static final int C_HDR_TOP  = 0xFF200855;
    private static final int C_HDR_BOT  = 0xFF0D0330;
    private static final int C_BORDER   = 0xFF7020CC;
    private static final int C_DIVIDER  = 0xFF5010AA;
    private static final int C_ROW_ALT  = 0x14FFFFFF;
    private static final int C_BAR_ON   = 0xFF00C060;
    private static final int C_BAR_OFF  = 0xFF992020;
    private static final int C_TEXT     = 0xFFEEEEEE;
    private static final int C_DIM      = 0xFF777777;
    private static final int C_KEY_BG   = 0xFF252535;
    private static final int C_KEY_ACT  = 0xFF553388;
    private static final int C_SLD_BG   = 0xFF1A1A2A;
    private static final int C_SLD_FG   = 0xFF7020CC;
    private static final int C_SLD_HND  = 0xFFAA55FF;
    private static final int C_CHK_ON   = 0xFF00BB55;
    private static final int C_CHK_OFF  = 0xFF3A3A3A;
    private static final int C_SUB_BG   = 0xFF090912;

    // Setting types
    interface Setting {}
    record SliderS(String label, DoubleSupplier getter, DoubleConsumer setter, double min, double max)
        implements Setting {}
    record BoolS(String label, BooleanSupplier getter, Runnable toggle)
        implements Setting {}

    // Module descriptor
    record Mod(String name, BooleanSupplier enabled, Runnable toggle,
               IntSupplier keyGet, IntConsumer keySet, List<Setting> settings) {}

    private final Mod[] MODS;
    private final boolean[] expanded;
    private final float[]   hoverT;

    // Rebind
    private int rebindIdx = -1;

    // Slider drag
    private boolean sliderDrag = false;
    private int sliderMod = -1, sliderSub = -1;
    private boolean configDirty = false;

    public ClickGuiScreen() {
        super(Component.empty());
        ModConfig cfg = ModConfig.get();

        MODS = new Mod[]{
            new Mod("NoFall",   NoFallMod::isEnabled,   NoFallMod::toggleManual,
                () -> cfg.nofallKey,
                k  -> { cfg.nofallKey    = k; cfg.save(); },
                List.of()),

            new Mod("XRay",     XRayMod::isEnabled,     XRayMod::toggle,
                () -> cfg.xrayKey,
                k  -> { cfg.xrayKey     = k; cfg.save(); },
                List.of()),

            new Mod("MaceAura", MaceAuraMod::isEnabled, MaceAuraMod::toggle,
                () -> cfg.maceAuraKey,
                k  -> { cfg.maceAuraKey = k; cfg.save(); },
                List.of(
                    new SliderS("Det Range",
                        () -> cfg.maceDetectRange,
                        v  -> { cfg.maceDetectRange  = (float)v; MaceAuraMod.RANGE        = (float)v; },
                        1.0, 10.0),
                    new SliderS("Atk Range",
                        () -> cfg.maceAttackRange,
                        v  -> { cfg.maceAttackRange  = (float)v; MaceAuraMod.ATTACK_RANGE = (float)v; },
                        1.0, 6.0)
                )),

            new Mod("KillAura", KillAuraMod::isEnabled, KillAuraMod::toggle,
                () -> cfg.killAuraKey,
                k  -> { cfg.killAuraKey = k; cfg.save(); },
                List.of(
                    new SliderS("Range",
                        () -> cfg.killRange,
                        v  -> { cfg.killRange        = (float)v; KillAuraMod.RANGE        = (float)v; },
                        1.0, 10.0),
                    new SliderS("Atk Range",
                        () -> cfg.killAttackRange,
                        v  -> { cfg.killAttackRange  = (float)v; KillAuraMod.ATTACK_RANGE = (float)v; },
                        1.0, 6.0),
                    new SliderS("Atk Delay",
                        () -> (double)cfg.killAttackDelay,
                        v  -> { cfg.killAttackDelay  = (int)Math.round(v); KillAuraMod.attackDelay = cfg.killAttackDelay; },
                        2, 20),
                    new BoolS("Single",
                        () -> cfg.killSingle,
                        ()  -> { cfg.killSingle = !cfg.killSingle; KillAuraMod.singleMode = cfg.killSingle; cfg.save(); }),
                    new BoolS("Hostile",
                        () -> cfg.killHostile,
                        ()  -> { cfg.killHostile = !cfg.killHostile; KillAuraMod.targetHostile = cfg.killHostile; cfg.save(); }),
                    new BoolS("Animals",
                        () -> cfg.killAnimals,
                        ()  -> { cfg.killAnimals  = !cfg.killAnimals;  KillAuraMod.targetAnimals  = cfg.killAnimals;  cfg.save(); }),
                    new BoolS("Players",
                        () -> cfg.killPlayers,
                        ()  -> { cfg.killPlayers  = !cfg.killPlayers;  KillAuraMod.targetPlayers  = cfg.killPlayers;  cfg.save(); })
                )),

            new Mod("NoSlow",   NoSlowMod::isEnabled,   NoSlowMod::toggle,
                () -> cfg.noSlowKey,
                k  -> { cfg.noSlowKey   = k; cfg.save(); },
                List.of()),

            new Mod("BHop",     BHopMod::isEnabled,     BHopMod::toggle,
                () -> cfg.bhopKey,
                k  -> { cfg.bhopKey    = k; cfg.save(); },
                List.of(
                    new SliderS("Speed",
                        () -> cfg.bhopSpeed,
                        v  -> { cfg.bhopSpeed = (float)v; BHopMod.speedMultiplier = (float)v; },
                        0.5, 2.5)
                )),

            new Mod("Step",     StepMod::isEnabled,     StepMod::toggle,
                () -> cfg.stepKey,
                k  -> { cfg.stepKey    = k; cfg.save(); },
                List.of()),
        };

        expanded = new boolean[MODS.length];
        hoverT   = new float[MODS.length];
    }

    @Override public boolean isPauseScreen() { return false; }

    // ?? Layout helpers ????????????????????????????????????????????????????????

    private int panelH() {
        int h = HEADER_H;
        for (int i = 0; i < MODS.length; i++) {
            h += ROW_H;
            if (expanded[i]) h += MODS[i].settings().size() * SUB_H;
        }
        return h + PAD;
    }

    private int rowY(int i) {
        int y = py + HEADER_H;
        for (int j = 0; j < i; j++) {
            y += ROW_H;
            if (expanded[j]) y += MODS[j].settings().size() * SUB_H;
        }
        return y;
    }

    private int subRowY(int mod, int sub) {
        return rowY(mod) + ROW_H + sub * SUB_H;
    }

    private int trackX(String label) {
        return px + PAD + INDENT + font.width(label) + 4;
    }

    private int trackW(String label) {
        return W - PAD * 2 - INDENT - font.width(label) - 4 - VAL_W;
    }

    // ?? Rendering ?????????????????????????????????????????????????????????????

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        int ph = panelH();

        // Drop shadow
        g.fill(px + 4, py + 4, px + W + 4, py + ph + 4, C_SHADOW);
        // Background
        g.fill(px, py, px + W, py + ph, C_BG);
        // Border
        border(g, px, py, W, ph, C_BORDER);
        // Header gradient
        g.fill(px + 1, py + 1,            px + W - 1, py + HEADER_H / 2, C_HDR_TOP);
        g.fill(px + 1, py + HEADER_H / 2, px + W - 1, py + HEADER_H,     C_HDR_BOT);
        g.fill(px + 1, py + HEADER_H,     px + W - 1, py + HEADER_H + 1, C_DIVIDER);
        // Title
        drawRainbow(g, "Valencia", px + PAD, py + 7, System.currentTimeMillis());

        for (int i = 0; i < MODS.length; i++) {
            renderRow(g, i, mx, my);
            if (expanded[i]) renderSettings(g, i);
        }

        super.render(g, mx, my, delta);
    }

    private void renderRow(GuiGraphics g, int i, int mx, int my) {
        Mod m  = MODS[i];
        int ry = rowY(i);
        boolean on  = m.enabled().getAsBoolean();
        boolean hov = mx >= px && mx < px + W && my >= ry && my < ry + ROW_H;

        hoverT[i] += ((hov ? 1f : 0f) - hoverT[i]) * 0.18f;
        if (i % 2 == 0) g.fill(px + 1, ry, px + W - 1, ry + ROW_H, C_ROW_ALT);
        if (hoverT[i] > 0.004f) {
            int a = (int)(hoverT[i] * 0x38);
            g.fill(px + 1, ry, px + W - 1, ry + ROW_H, (a << 24) | 0x00FFFFFF);
        }

        // Left state bar
        g.fill(px + 1, ry, px + 4, ry + ROW_H, on ? C_BAR_ON : C_BAR_OFF);

        // Module name
        g.drawString(font, m.name(), px + PAD + 4, ry + (ROW_H - 8) / 2, C_TEXT, false);

        // Expand arrow
        if (!m.settings().isEmpty()) {
            int ax = px + W - PAD - KEY_W - ARR_W - 4;
            g.drawString(font, expanded[i] ? "v" : ">", ax, ry + (ROW_H - 8) / 2, C_DIM, false);
        }

        // Key badge
        int kx = px + W - PAD - KEY_W;
        int ky = ry + (ROW_H - KEY_H) / 2;
        boolean rebinding = rebindIdx == i;
        g.fill(kx, ky, kx + KEY_W, ky + KEY_H, rebinding ? C_KEY_ACT : C_KEY_BG);
        border(g, kx, ky, KEY_W, KEY_H, rebinding ? 0xFFAA77FF : 0xFF444466);
        String keyLabel = rebinding ? "..." : shortKeyName(m.keyGet().getAsInt());
        int klw = font.width(keyLabel);
        g.drawString(font, keyLabel, kx + (KEY_W - klw) / 2, ky + (KEY_H - 7) / 2, 0xFFCCCCFF, false);
    }

    private void renderSettings(GuiGraphics g, int mod) {
        List<Setting> settings = MODS[mod].settings();
        for (int j = 0; j < settings.size(); j++) {
            int sy = subRowY(mod, j);
            g.fill(px + 1, sy, px + W - 1, sy + SUB_H, C_SUB_BG);
            g.fill(px + 1, sy, px + 2, sy + SUB_H, 0xFF441188); // left accent

            Setting s = settings.get(j);
            if (s instanceof SliderS ss) renderSlider(g, ss, sy);
            else if (s instanceof BoolS bs) renderBool(g, bs, sy);
        }
    }

    private void renderSlider(GuiGraphics g, SliderS ss, int sy) {
        int tx = trackX(ss.label());
        int tw = trackW(ss.label());
        int ty = sy + (SUB_H - 3) / 2;

        g.drawString(font, ss.label(), px + PAD + INDENT, sy + (SUB_H - 8) / 2, C_DIM, false);

        g.fill(tx, ty, tx + tw, ty + 3, C_SLD_BG);

        double val = ss.getter().getAsDouble();
        double t   = Math.max(0.0, Math.min(1.0, (val - ss.min()) / (ss.max() - ss.min())));
        int fillW  = (int)(tw * t);
        g.fill(tx, ty, tx + fillW, ty + 3, C_SLD_FG);

        // Handle nub
        int hx = tx + fillW - 2;
        g.fill(hx, ty - 2, hx + 4, ty + 5, C_SLD_HND);

        // Value label
        String valStr = String.format("%.1f", val);
        g.drawString(font, valStr, tx + tw + 3, sy + (SUB_H - 8) / 2, C_TEXT, false);
    }

    private void renderBool(GuiGraphics g, BoolS bs, int sy) {
        boolean val = bs.getter().getAsBoolean();
        int cx = px + PAD + INDENT;
        int cy = sy + (SUB_H - 8) / 2;

        g.fill(cx, cy, cx + 8, cy + 8, val ? C_CHK_ON : C_CHK_OFF);
        if (val) g.fill(cx + 2, cy + 2, cx + 6, cy + 6, 0xFFFFFFFF);

        g.drawString(font, bs.label(), cx + 12, cy, C_TEXT, false);
    }

    // ?? Draw helpers ??????????????????????????????????????????????????????????

    private void border(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x,         y,         x + w,     y + 1,     c);
        g.fill(x,         y + h - 1, x + w,     y + h,     c);
        g.fill(x,         y,         x + 1,     y + h,     c);
        g.fill(x + w - 1, y,         x + w,     y + h,     c);
    }

    private void drawRainbow(GuiGraphics g, String text, int x, int y, long t) {
        int cx = x;
        for (int i = 0; i < text.length(); i++) {
            String ch = String.valueOf(text.charAt(i));
            if (ch.equals(" ")) { cx += font.width(" "); continue; }
            g.drawString(font, ch, cx, y, rainbow(t, i * 120), false);
            cx += font.width(ch);
        }
    }

    private static int rainbow(long t, int offset) {
        float hue = ((t + offset) % 4000) / 4000f;
        int rgb = java.awt.Color.HSBtoRGB(hue, 0.7f, 1.0f);
        return 0xFF000000 | (rgb & 0x00FFFFFF);
    }

    private static String shortKeyName(int key) {
        String full = ModConfig.keyName(key);
        return switch (full) {
            case "RIGHT_CONTROL" -> "RCTRL";
            case "LEFT_CONTROL"  -> "LCTRL";
            case "RIGHT_SHIFT"   -> "RSHFT";
            case "LEFT_SHIFT"    -> "LSHFT";
            case "RIGHT_ALT"     -> "RALT";
            case "LEFT_ALT"      -> "LALT";
            default -> full.length() > 5 ? full.substring(0, 5) : full;
        };
    }

    // ?? Input ?????????????????????????????????????????????????????????????????

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
        int mx = (int)event.x(), my = (int)event.y(), btn = event.button();
        if (btn != 0) return super.mouseClicked(event, isDoubleClick);

        // Header drag
        if (mx >= px && mx < px + W && my >= py && my < py + HEADER_H) {
            headerDrag = true;
            dragDx = mx - px;
            dragDy = my - py;
            return true;
        }

        // Module rows
        for (int i = 0; i < MODS.length; i++) {
            int ry = rowY(i);
            if (my < ry || my >= ry + ROW_H || mx < px || mx >= px + W) continue;

            int kx = px + W - PAD - KEY_W;
            int ky = ry + (ROW_H - KEY_H) / 2;

            // Key badge
            if (mx >= kx && mx < kx + KEY_W && my >= ky && my < ky + KEY_H) {
                rebindIdx = (rebindIdx == i) ? -1 : i;
                return true;
            }

            // Expand arrow
            if (!MODS[i].settings().isEmpty()) {
                int ax = px + W - PAD - KEY_W - ARR_W - 4;
                if (mx >= ax && mx < kx - 2) {
                    expanded[i] = !expanded[i];
                    return true;
                }
            }

            // Toggle module
            MODS[i].toggle().run();
            return true;
        }

        // Sub-rows (sliders & bools)
        for (int i = 0; i < MODS.length; i++) {
            if (!expanded[i]) continue;
            List<Setting> settings = MODS[i].settings();
            for (int j = 0; j < settings.size(); j++) {
                int sy = subRowY(i, j);
                if (my < sy || my >= sy + SUB_H || mx < px || mx >= px + W) continue;

                Setting s = settings.get(j);
                if (s instanceof SliderS ss) {
                    int tx = trackX(ss.label());
                    int tw = trackW(ss.label());
                    if (mx >= tx - 2 && mx <= tx + tw + 2) {
                        sliderDrag = true;
                        sliderMod  = i;
                        sliderSub  = j;
                        applySlider(ss, mx, tx, tw);
                        return true;
                    }
                } else if (s instanceof BoolS bs) {
                    bs.toggle().run();
                    return true;
                }
            }
        }

        return super.mouseClicked(event, isDoubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            headerDrag = false;
            if (sliderDrag) {
                sliderDrag = false;
                if (configDirty) { ModConfig.get().save(); configDirty = false; }
            }
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (event.button() != 0) return super.mouseDragged(event, dx, dy);
        int mx = (int)event.x(), my = (int)event.y();

        if (headerDrag) {
            px = mx - dragDx;
            py = my - dragDy;
            return true;
        }

        if (sliderDrag && sliderMod >= 0 && sliderSub >= 0) {
            Setting s = MODS[sliderMod].settings().get(sliderSub);
            if (s instanceof SliderS ss) {
                int tx = trackX(ss.label());
                int tw = trackW(ss.label());
                applySlider(ss, mx, tx, tw);
                configDirty = true;
            }
            return true;
        }

        return super.mouseDragged(event, dx, dy);
    }

    private void applySlider(SliderS ss, int mx, int tx, int tw) {
        double t   = Math.max(0.0, Math.min(1.0, (double)(mx - tx) / tw));
        double raw = ss.min() + t * (ss.max() - ss.min());
        double val = Math.round(raw * 10.0) / 10.0;
        ss.setter().accept(val);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();

        if (rebindIdx >= 0) {
            if (key != GLFW.GLFW_KEY_ESCAPE) {
                MODS[rebindIdx].keySet().accept(key);
            }
            rebindIdx = -1;
            return true;
        }

        if (key == ModConfig.get().guiKey) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }
}
