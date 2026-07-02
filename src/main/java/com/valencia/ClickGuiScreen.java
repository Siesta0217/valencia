package com.valencia;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * ClickGUI host screen. Owns the shared module data ({@link #panels}), the
 * per-frame {@link #skin}, and the key-rebind capture target, then forwards
 * render/input to whichever {@link GuiLayout} {@code cfg.guiLayout} selects:
 *   0 = Panels (Raven), 1 = Sidebar, 2 = Tenacity, 3 = Aurora Glass.
 *
 * Model types live in GuiModel.java, the module wiring in ModuleRegistry.java,
 * the shared drawing kit in GuiDraw.java, and each layout in its own Layout*.java.
 */
public class ClickGuiScreen extends Screen {

    // ── Shared state (read by the layouts) ──────────────────────────────────
    final List<Panel> panels = new ArrayList<>();
    KeyS rebindTarget;   // key setting currently capturing input; null = none
    GuiSkin skin;        // resolved each frame from cfg.guiStyle (live switch)

    private long openTime;

    // ── Layouts (one instance each; each keeps its own window/scroll state) ──
    private final LayoutPanels   panelsLayout   = new LayoutPanels(this);
    private final LayoutSidebar  sidebarLayout  = new LayoutSidebar(this);
    private final LayoutTenacity tenacityLayout = new LayoutTenacity(this);
    private final LayoutAurora   auroraLayout   = new LayoutAurora(this);
    private final LayoutGlass    glassLayout    = new LayoutGlass(this);
    private final GuiLayout[] layouts = { panelsLayout, sidebarLayout, tenacityLayout, auroraLayout, glassLayout };

    // ── Waifu background ─────────────────────────────────────────────────────
    private Identifier waifuLoc;
    private int waifuTexW, waifuTexH;
    private String waifuHint, waifuErr;

    public ClickGuiScreen() {
        super(Component.empty());
        panels.addAll(ModuleRegistry.build());
        loadWaifu();
        openTime = System.currentTimeMillis();
    }

    @Override public boolean isPauseScreen() { return false; }

    /** Screen dimensions for the layouts (Screen.width/height are protected). */
    int screenW() { return width; }
    int screenH() { return height; }

    private GuiLayout active(ModConfig cfg) {
        switch (cfg.guiLayout) {
            case 4:  return glassLayout;
            case 3:  return auroraLayout;
            case 2:  return tenacityLayout;
            case 1:  return sidebarLayout;
            default: return panelsLayout;
        }
    }

    /** Astolfo / rainbow color cycle (Raven-style), used for the version label. */
    private static int astolfo(int yOffset, float speed) {
        float hue = (float)((System.currentTimeMillis() % (long)speed) + yOffset) / speed;
        return java.awt.Color.HSBtoRGB(hue % 1f, 0.55f, 1.0f);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float dt) {
        super.render(g, mx, my, dt);
        ModConfig cfg = ModConfig.get();

        int accent = 0xFF000000 | (cfg.accentR << 16) | (cfg.accentG << 8) | cfg.accentB;
        skin = GuiSkin.of(cfg.guiStyle, accent);

        // Background. The Glass layout (4) frosts the world behind it via the
        // vanilla blur post-chain (real liquid glass needs the backdrop blurred,
        // not just a dark tint); other layouts keep the flat semi-transparent
        // dim. renderBlurredBackground also lays down a subtle scrim, so no
        // extra fill is needed there.
        float openAnim = Math.min(1f, (System.currentTimeMillis() - openTime) / 400f);
        boolean blurred = false;
        if (cfg.guiLayout == 4) {
            try { renderBlurredBackground(g); blurred = true; } catch (Throwable ignored) {}
        }
        if (!blurred) {
            int bgA = (int)(cfg.bgAlpha * openAnim);
            g.fill(0, 0, width, height, (bgA << 24));
        }

        renderWaifu(g);

        // version string at bottom-left
        Font font = Minecraft.getInstance().font;
        int verColor = skin.rainbowVersion ? (astolfo(0, 4890f) | 0xFF000000) : accent;
        g.drawString(font, "Valencia", 5, height - font.lineHeight - 3, verColor, true);

        active(cfg).render(g, mx, my, accent, font);
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  INPUT — forwarded to the active layout
    // ═════════════════════════════════════════════════════════════════════════

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean dbl) {
        int mx = (int)event.x(), my = (int)event.y(), btn = event.button();
        if (active(ModConfig.get()).mouseClicked(mx, my, btn)) return true;
        return super.mouseClicked(event, dbl);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        int mx = (int)event.x(), my = (int)event.y();
        if (active(ModConfig.get()).mouseDragged(mx, my)) return true;
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        for (GuiLayout l : layouts) l.mouseReleased();
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int mx = (int)mouseX, my = (int)mouseY;
        if (active(ModConfig.get()).mouseScrolled(mx, my, scrollY)) return true;
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (rebindTarget != null) {
            if (key != GLFW.GLFW_KEY_ESCAPE) rebindTarget.set().accept(key);
            rebindTarget = null;
            return true;
        }
        if (key == ModConfig.get().guiKey) { onClose(); return true; }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        saveEnabled();
        super.onClose();
    }

    private void saveEnabled() {
        // Registry-driven — this used to be the fifth hand-copied module list
        // (it had already drifted: Waypoints and AutoEat were missing).
        Modules.saveEnabled();
    }

    // ═════════════════════════════════════════════════════════════════════════
    //  WAIFU BACKGROUND
    // ═════════════════════════════════════════════════════════════════════════
    private static final String[] WAIFU_EXTS = {"png", "jpg", "jpeg", "bmp", "gif"};

    private void loadWaifu() {
        File dir = FabricLoader.getInstance().getConfigDir().resolve("valencia").toFile();
        waifuHint = dir.getAbsolutePath();
        try {
            if (!dir.exists() || !dir.isDirectory()) { waifuErr = "config dir missing"; return; }
            File f = null;
            for (String ext : WAIFU_EXTS) { File c = new File(dir, "waifu." + ext); if (c.exists()) { f = c; break; } }
            if (f == null) { waifuErr = "no waifu.{png,jpg,bmp,gif}"; return; }
            InputStream pngStream;
            if (f.getName().toLowerCase().endsWith(".png")) {
                pngStream = new FileInputStream(f);
            } else {
                BufferedImage img = ImageIO.read(f);
                if (img == null) { waifuErr = "decode failed"; return; }
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "png", baos);
                pngStream = new ByteArrayInputStream(baos.toByteArray());
            }
            NativeImage ni = NativeImage.read(pngStream);
            waifuTexW = ni.getWidth(); waifuTexH = ni.getHeight();
            DynamicTexture dTex = new DynamicTexture(() -> "valencia-waifu", ni);
            Identifier loc = Identifier.fromNamespaceAndPath("valencia", "waifu");
            Minecraft.getInstance().getTextureManager().register(loc, dTex);
            waifuLoc = loc; waifuErr = null;
        } catch (Throwable t) { waifuErr = t.getClass().getSimpleName() + ": " + t.getMessage(); }
    }

    private void renderWaifu(GuiGraphics g) {
        Font font = Minecraft.getInstance().font;
        if (waifuLoc == null || waifuTexW <= 0 || waifuTexH <= 0) {
            String hint = waifuErr != null
                ? "§c[waifu] " + waifuErr
                : "§8[waifu: " + (waifuHint != null ? waifuHint : "config/valencia") + File.separator + "waifu.png]";
            g.drawString(font, hint, 4, height - 22, 0xFF888888, false);
            return;
        }
        try {
            int dispH = Math.min(height / 3, 150);
            int dispW = waifuTexW * dispH / waifuTexH;
            int x1 = 4, y1 = height - dispH - 24;
            g.blit(waifuLoc, x1, y1, x1 + dispW, y1 + dispH, 0f, 1f, 0f, 1f);
        } catch (Throwable t) {
            g.drawString(font, "§c[waifu err]", 4, height - 22, 0xFF888888, false);
        }
    }
}
