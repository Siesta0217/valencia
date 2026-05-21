package com.valencia;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;

public class StepMod {

    private static boolean enabled = false;

    /** Step height in blocks. Vanilla default is 0.6 (auto-step over half-slabs). */
    public static float stepHeight = 1.0f;

    private static final double VANILLA_STEP = 0.6;

    public static boolean isEnabled() { return enabled; }
    public static void toggle()       { enabled = !enabled; }

    public static boolean isActive() {
        if (!enabled) return false;
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null;
    }

    /**
     * Apply the configured step height by writing to the player's STEP_HEIGHT
     * attribute. Called every tick from StepMixin. The attribute replaced the
     * old {@code Entity.maxUpStep} field in MC 1.20.5+, which is why the
     * @Shadow approach used to crash on this Lunar Client build.
     */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        AttributeInstance attr = p.getAttribute(Attributes.STEP_HEIGHT);
        if (attr == null) return;
        double desired = isActive() ? stepHeight : VANILLA_STEP;
        if (attr.getBaseValue() != desired) attr.setBaseValue(desired);
    }
}
