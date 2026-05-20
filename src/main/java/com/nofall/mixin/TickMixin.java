package com.nofall.mixin;

import com.nofall.BHopMod;
import com.nofall.ClickGuiScreen;
import com.nofall.KillAuraMod;
import com.nofall.MaceAuraMod;
import com.nofall.ModConfig;
import com.nofall.NoFallMod;
import com.nofall.NoSlowMod;
import com.nofall.StepMod;
import com.nofall.XRayMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class TickMixin {

    @Unique private boolean nofall$prevN    = false;
    @Unique private boolean nofall$prevX    = false;
    @Unique private boolean nofall$prevM    = false;
    @Unique private boolean nofall$prevG    = false;
    @Unique private boolean nofall$prevGui  = false;
    @Unique private boolean nofall$prevB    = false;
    @Unique private boolean nofall$prevH    = false;
    @Unique private boolean nofall$prevK    = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void nofall$onTick(CallbackInfo ci) {
        Minecraft mc = (Minecraft)(Object)this;

        long handle = GLFW.glfwGetCurrentContext();
        if (handle == 0L) return;

        ModConfig cfg = ModConfig.get();
        boolean nDown   = GLFW.glfwGetKey(handle, cfg.nofallKey)   == GLFW.GLFW_PRESS;
        boolean xDown   = GLFW.glfwGetKey(handle, cfg.xrayKey)     == GLFW.GLFW_PRESS;
        boolean mDown   = GLFW.glfwGetKey(handle, cfg.maceAuraKey) == GLFW.GLFW_PRESS;
        boolean gDown   = GLFW.glfwGetKey(handle, cfg.noSlowKey)   == GLFW.GLFW_PRESS;
        boolean guiDown = GLFW.glfwGetKey(handle, cfg.guiKey)      == GLFW.GLFW_PRESS;
        boolean bDown   = GLFW.glfwGetKey(handle, cfg.bhopKey)     == GLFW.GLFW_PRESS;
        boolean hDown   = GLFW.glfwGetKey(handle, cfg.stepKey)     == GLFW.GLFW_PRESS;
        boolean kDown   = GLFW.glfwGetKey(handle, cfg.killAuraKey) == GLFW.GLFW_PRESS;

        if (mc.screen == null) {
            if (nDown && !nofall$prevN)   { NoFallMod.toggleManual(); msg(mc, "§7[NoFall] "   + state(NoFallMod.isEnabled())); }
            if (xDown && !nofall$prevX)   { XRayMod.toggle();         msg(mc, "§7[XRay] "     + state(XRayMod.isEnabled()));   }
            if (mDown && !nofall$prevM)   { MaceAuraMod.toggle();     msg(mc, "§7[MaceAura] " + state(MaceAuraMod.isEnabled())); }
            if (gDown && !nofall$prevG)   { NoSlowMod.toggle();       msg(mc, "§7[NoSlow] "   + state(NoSlowMod.isEnabled())); }
            if (bDown && !nofall$prevB)   { BHopMod.toggle();         msg(mc, "§7[BHop] "     + state(BHopMod.isEnabled()));   }
            if (hDown && !nofall$prevH)   { StepMod.toggle();         msg(mc, "§7[Step] "     + state(StepMod.isEnabled()));   }
            if (kDown && !nofall$prevK)   { KillAuraMod.toggle();     msg(mc, "§7[KillAura] " + state(KillAuraMod.isEnabled())); }
            if (guiDown && !nofall$prevGui) mc.setScreen(new ClickGuiScreen());
        } else if (mc.screen instanceof ClickGuiScreen && guiDown && !nofall$prevGui) {
            mc.setScreen(null);
        }

        nofall$prevN   = nDown;
        nofall$prevX   = xDown;
        nofall$prevM   = mDown;
        nofall$prevG   = gDown;
        nofall$prevGui = guiDown;
        nofall$prevB   = bDown;
        nofall$prevH   = hDown;
        nofall$prevK   = kDown;
    }

    private static String state(boolean on) { return on ? "§aON" : "§cOFF"; }

    private static void msg(Minecraft mc, String text) {
        if (mc.player != null)
            mc.player.displayClientMessage(Component.literal(text), true);
    }
}
