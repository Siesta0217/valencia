package com.nofall.mixin;

import com.nofall.BHopMod;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class BHopMixin {

    @Shadow protected boolean jumping;
    @Shadow protected abstract void jumpFromGround();

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void bhop$jump(CallbackInfo ci) {
        if (!((Object)this instanceof LocalPlayer)) return;
        if (!BHopMod.isActive()) return;
        if (jumping) return;

        LivingEntity self = (LivingEntity)(Object)this;
        if (!self.onGround()) return;

        // Check WASD keys directly — triggers for all directions including strafe + diagonals
        long handle = GLFW.glfwGetCurrentContext();
        if (handle == 0L) return;
        boolean moving = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS
                      || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS
                      || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS
                      || GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS;
        if (!moving) return;

        jumpFromGround();

        float mult = BHopMod.speedMultiplier;
        if (mult != 1.0f) {
            Vec3 nv = self.getDeltaMovement();
            self.setDeltaMovement(nv.x * mult, nv.y, nv.z * mult);
        }
    }
}
