package com.nofall;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.function.BooleanSupplier;

public class ClickGuiScreen extends Screen {

    private static final int PX = 10, PY = 10;
    private static final int W  = 140;
    private static final int H_HEADER = 16;
    private static final int ROW = 20, GAP = 2;

    record Mod(String name, BooleanSupplier enabled, Runnable toggle) {}

    private final Mod[] MODS = {
        new Mod("NoFall",   NoFallMod::isEnabled,   NoFallMod::toggleManual),
        new Mod("XRay",     XRayMod::isEnabled,     XRayMod::toggle),
        new Mod("MaceAura", MaceAuraMod::isEnabled, MaceAuraMod::toggle),
        new Mod("NoSlow",   NoSlowMod::isEnabled,   NoSlowMod::toggle),
    };

    private Button[] buttons;

    public ClickGuiScreen() {
        super(Component.empty());
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    protected void init() {
        buttons = new Button[MODS.length];
        for (int i = 0; i < MODS.length; i++) {
            final int idx = i;
            buttons[i] = Button.builder(
                    buildLabel(MODS[i]),
                    btn -> {
                        MODS[idx].toggle().run();
                        btn.setMessage(buildLabel(MODS[idx]));
                    })
                    .pos(PX + 3, PY + H_HEADER + i * (ROW + GAP))
                    .size(W - 6, ROW)
                    .build();
            addRenderableWidget(buttons[i]);
        }
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        int totalH = H_HEADER + MODS.length * (ROW + GAP) + 4;

        // 外框陰影
        g.fill(PX + 2, PY + 2, PX + W + 2, PY + totalH + 2, 0x55000000);
        // Panel 背景
        g.fill(PX, PY, PX + W, PY + totalH, 0xDD0d0d1a);
        // Header 漸層
        g.fill(PX, PY,              PX + W, PY + H_HEADER / 2, 0xFF1a3a6e);
        g.fill(PX, PY + H_HEADER/2, PX + W, PY + H_HEADER,     0xFF122850);
        g.drawString(font, "§b§lNoFall Mod", PX + 5, PY + 4, 0xFFFFFF, false);

        // 同步按鈕標籤（處理快速鍵切換後的狀態）
        if (buttons != null) {
            for (int i = 0; i < MODS.length; i++) {
                buttons[i].setMessage(buildLabel(MODS[i]));
            }
        }

        super.render(g, mx, my, delta);
    }

    private static Component buildLabel(Mod m) {
        String status = m.enabled().getAsBoolean() ? "§aON" : "§cOFF";
        return Component.literal(m.name() + "  " + status);
    }
}
