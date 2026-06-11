package com.valencia.mixin;

import com.valencia.FlyMod;
import com.valencia.FreecamMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Motion fly: overwrite the player's velocity each tick from input + look.
 *
 * Done at {@code aiStep} HEAD so vanilla's {@code move()} this tick uses our
 * velocity. moveRelative only adds horizontal input (never vertical) and the
 * gravity vanilla applies after the move only touches next tick's stored
 * velocity — which we overwrite again — so a neutral vertical input hovers
 * cleanly with no sink.
 */
@Mixin(LivingEntity.class)
public abstract class FlyMixin {

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void fly$tick(CallbackInfo ci) {
        if (!((Object) this instanceof LocalPlayer player)) return;
        if (!FlyMod.isActive()) return;

        // Don't drive movement while a screen is open (chat/GUI) — hover in
        // place. Same while Freecam is active: Fly reads raw GLFW keys, which
        // bypasses ClientInputMixin's input freeze, so without this WASD would
        // fly the body away while the camera is detached.
        boolean guiOpen = Minecraft.getInstance().screen != null || FreecamMod.isActive();
        long handle = GLFW.glfwGetCurrentContext();
        if (handle == 0L) return;

        boolean w  = !guiOpen && GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;
        boolean s  = !guiOpen && GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS;
        boolean a  = !guiOpen && GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS;
        boolean d  = !guiOpen && GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS;
        boolean up = !guiOpen && GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_SPACE) == GLFW.GLFW_PRESS;
        boolean dn = !guiOpen && GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS;

        float yr = (float) Math.toRadians(player.getYRot());
        double sinY = Math.sin(yr), cosY = Math.cos(yr);
        double mx = 0, mz = 0;
        if (w) { mx -= sinY; mz += cosY; }
        if (s) { mx += sinY; mz -= cosY; }
        if (d) { mx -= cosY; mz -= sinY; }
        if (a) { mx += cosY; mz += sinY; }

        double len = Math.sqrt(mx * mx + mz * mz);
        if (len > 0.001) { mx = mx / len * FlyMod.hSpeed; mz = mz / len * FlyMod.hSpeed; }
        else             { mx = 0; mz = 0; }

        double my = (up ? FlyMod.vSpeed : 0.0) - (dn ? FlyMod.vSpeed : 0.0);

        player.setDeltaMovement(mx, my, mz);
        player.fallDistance = 0;   // never accrue fall damage while flying
    }
}
