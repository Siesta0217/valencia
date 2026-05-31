package com.valencia;

/**
 * Color skin for the ClickGUI. {@code cfg.guiStyle} selects one; the
 * {@link ClickGuiScreen} resolves it once per frame so the look switches live.
 *
 * Style 0 (Dark) reproduces the original Raven palette exactly, so existing
 * users see no change after this update. Style 1 (Light) flips to a pale theme
 * with dark text; Style 2 (Glass) is a heavily translucent accent-tinted look.
 *
 * Fields are package-private and set inside the factory methods — a 20-arg
 * constructor would be unreadable and easy to mis-order.
 */
final class GuiSkin {

    int panelBg, headerBg, headerHover, headerUnderline, catLabel;
    int settingsBg, expandedOffBg, rowHover;
    int textOn, textDim, textOff;
    int sliderTrack, sliderTrackBorder, sliderFillAlpha;
    int boolTrack, widgetBorder, borderIdle, scrollBar;
    int enabledFlatAlpha;        // alpha of the flat enabled-row fill when !enabledRowGradient
    boolean enabledRowGradient;  // accent vertical gradient (Dark/Glass) vs flat accent tint (Light)
    boolean rainbowVersion;      // bottom-left "Valencia" string: rainbow vs flat accent
    boolean nameShadow;          // drop shadow on module names (off for dark-text Light theme)
    String name;

    private GuiSkin() {}

    /** @param accentRgb full-alpha accent (0xFFrrggbb); only the rgb part is used. */
    static GuiSkin of(int style, int accentRgb) {
        int a = accentRgb & 0x00FFFFFF;
        return switch (style) {
            case 1 -> light(a);
            case 2 -> glass(a);
            default -> dark(a);
        };
    }

    private static GuiSkin dark(int a) {
        GuiSkin s = new GuiSkin();
        s.name = "Dark";
        s.panelBg = 0xC8141414;
        s.headerBg = 0xFF1E1E1E;
        s.headerHover = 0xFF2D2D2D;
        s.headerUnderline = 0xFF000000 | a;
        s.catLabel = 0xFF000000 | a;
        s.settingsBg = 0xC0101010;
        s.expandedOffBg = 0xFF2A2A2A;
        s.rowHover = 0x40FFFFFF;
        s.textOn = 0xFFFFFFFF;
        s.textDim = 0xFFCCCCCC;
        s.textOff = 0xFF999999;
        s.sliderTrack = 0x30FFFFFF;
        s.sliderTrackBorder = 0x50FFFFFF;
        s.sliderFillAlpha = 0xC0;
        s.boolTrack = 0xFF000000;
        s.widgetBorder = 0x50FFFFFF;
        s.borderIdle = 0x40FFFFFF;
        s.scrollBar = 0x80FFFFFF;
        s.enabledRowGradient = true;
        s.enabledFlatAlpha = 0xC0;
        s.rainbowVersion = true;
        s.nameShadow = true;
        return s;
    }

    private static GuiSkin light(int a) {
        GuiSkin s = new GuiSkin();
        s.name = "Light";
        s.panelBg = 0xF0E6E6EA;
        s.headerBg = 0xFFFFFFFF;
        s.headerHover = 0xFFECECF2;
        s.headerUnderline = 0xFF000000 | a;
        s.catLabel = 0xFF000000 | a;
        s.settingsBg = 0xF0DBDBE2;
        s.expandedOffBg = 0xFFC9C9D1;
        s.rowHover = 0x33000000;
        s.textOn = 0xFF14141A;
        s.textDim = 0xFF3C3C44;
        s.textOff = 0xFF6C6C74;
        s.sliderTrack = 0x33000000;
        s.sliderTrackBorder = 0x55000000;
        s.sliderFillAlpha = 0xE0;
        s.boolTrack = 0xFFB6B6BE;
        s.widgetBorder = 0x55000000;
        s.borderIdle = 0x33000000;
        s.scrollBar = 0x66000000;
        s.enabledRowGradient = false;
        s.enabledFlatAlpha = 0x66;
        s.rainbowVersion = false;
        s.nameShadow = false;
        return s;
    }

    private static GuiSkin glass(int a) {
        GuiSkin s = new GuiSkin();
        s.name = "Glass";
        s.panelBg = 0x80101018;
        s.headerBg = 0x55000000 | a;
        s.headerHover = 0x80000000 | a;
        s.headerUnderline = 0xFF000000 | a;
        s.catLabel = 0xFFFFFFFF;
        s.settingsBg = 0x70101018;
        s.expandedOffBg = 0x90202028;
        s.rowHover = 0x40FFFFFF;
        s.textOn = 0xFFFFFFFF;
        s.textDim = 0xFFDDDDDD;
        s.textOff = 0xFFAAAAAA;
        s.sliderTrack = 0x40FFFFFF;
        s.sliderTrackBorder = 0x80000000 | a;
        s.sliderFillAlpha = 0xE0;
        s.boolTrack = 0x80000000;
        s.widgetBorder = 0x70000000 | a;
        s.borderIdle = 0x60000000 | a;
        s.scrollBar = 0xA0000000 | a;
        s.enabledRowGradient = true;
        s.enabledFlatAlpha = 0xC0;
        s.rainbowVersion = true;
        s.nameShadow = true;
        return s;
    }
}
