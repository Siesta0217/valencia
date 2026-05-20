package com.nofall.mixin;

import com.nofall.ClickGuiScreen;
import com.nofall.MaceAuraMod;
import com.nofall.ModConfig;
import com.nofall.NoFallMod;
import com.nofall.NoSlowMod;
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

    @Unique private boolean nofall$prevN = false;
    @Unique private boolean nofall$prevX = false;
    @Unique private boolean nofall$prevM = false;
    @Unique private boolean nofall$prevG = false;
    @Unique private boolean nofall$prevGui = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void nofall$onTick(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;

        long handle = GLFW.glfwGetCurrentContext();
        if (handle == 0L) return;

        ModConfig cfg = ModConfig.get();
        boolean nDown   = GLFW.glfwGetKey(handle, cfg.nofallKey)   == GLFW.GLFW_PRESS;
        boolean xDown   = GLFW.glfwGetKey(handle, cfg.xrayKey)     == GLFW.GLFW_PRESS;
        boolean mDown   = GLFW.glfwGetKey(handle, cfg.maceAuraKey) == GLFW.GLFW_PRESS;
        boolean gDown   = GLFW.glfwGetKey(handle, cfg.noSlowKey)   == GLFW.GLFW_PRESS;
        boolean guiDown = GLFW.glfwGetKey(handle, cfg.guiKey)      == GLFW.GLFW_PRESS;

        if (mc.screen == null) {
            if (nDown && !nofall$prevN) {
                NoFallMod.toggleManual();
                msg(mc, "§7[NoFall] " + (NoFallMod.isEnabled() ? "§aON" : "§cOFF"));
            }
            if (xDown && !nofall$prevX) {
                XRayMod.toggle();
                msg(mc, "§7[XRay] " + (XRayMod.isEnabled() ? "§aON" : "§cOFF"));
            }
            if (mDown && !nofall$prevM) {
                MaceAuraMod.toggle();
                msg(mc, "§7[MaceAura] " + (MaceAuraMod.isEnabled() ? "§aON" : "§cOFF"));
            }
            if (gDown && !nofall$prevG) {
                NoSlowMod.toggle();
                msg(mc, "§7[NoSlow] " + (NoSlowMod.isEnabled() ? "§aON" : "§cOFF"));
            }
            if (guiDown && !nofall$prevGui) {
                mc.setScreen(new ClickGuiScreen());
            }
        } else if (mc.screen instanceof ClickGuiScreen && guiDown && !nofall$prevGui) {
            mc.setScreen(null);
        }

        nofall$prevN   = nDown;
        nofall$prevX   = xDown;
        nofall$prevM   = mDown;
        nofall$prevG   = gDown;
        nofall$prevGui = guiDown;
    }

    private static void msg(Minecraft mc, String text) {
        if (mc.player != null)
            mc.player.displayClientMessage(Component.literal(text), true);
    }
}
