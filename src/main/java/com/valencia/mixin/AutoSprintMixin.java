package com.valencia.mixin;

import com.valencia.AutoSprintMod;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class AutoSprintMixin {

    @Inject(method = "aiStep", at = @At("RETURN"))
    private void sprint$tail(CallbackInfo ci) {
        if (!AutoSprintMod.isActive()) return;
        LocalPlayer self = (LocalPlayer)(Object)this;

        long h = GLFW.glfwGetCurrentContext();
        if (h == 0L) return;

        boolean moving =
            GLFW.glfwGetKey(h, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS ||
            GLFW.glfwGetKey(h, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS ||
            GLFW.glfwGetKey(h, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS ||
            GLFW.glfwGetKey(h, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS;

        if (moving) self.setSprinting(true);
    }
}
