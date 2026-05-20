package com.valencia.mixin;

import com.valencia.BHopMod;
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
    private void bhop$tick(CallbackInfo ci) {
        if (!((Object)this instanceof LocalPlayer)) return;
        if (!BHopMod.isActive()) return;

        LivingEntity self = (LivingEntity)(Object)this;
        LocalPlayer player = (LocalPlayer) self;

        long handle = GLFW.glfwGetCurrentContext();
        if (handle == 0L) return;

        boolean wKey = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_W) == GLFW.GLFW_PRESS;
        boolean sKey = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_S) == GLFW.GLFW_PRESS;
        boolean aKey = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_A) == GLFW.GLFW_PRESS;
        boolean dKey = GLFW.glfwGetKey(handle, GLFW.GLFW_KEY_D) == GLFW.GLFW_PRESS;
        boolean moving = wKey || sKey || aKey || dKey;

        if (self.onGround()) {
            // Auto-jump when any WASD key held; skip if vanilla jump key already pressed
            if (!moving || jumping) return;
            jumpFromGround();
            float mult = BHopMod.speedMultiplier;
            if (mult != 1.0f) {
                Vec3 nv = self.getDeltaMovement();
                self.setDeltaMovement(nv.x * mult, nv.y, nv.z * mult);
            }
        } else {
            // Air steering: velocity direction = wish direction, speed preserved
            if (!moving) return;

            float yr = (float) Math.toRadians(player.getYRot());
            float sinY = (float) Math.sin(yr);
            float cosY = (float) Math.cos(yr);

            float wishX = 0, wishZ = 0;
            if (wKey) { wishX -= sinY; wishZ += cosY; }
            if (sKey) { wishX += sinY; wishZ -= cosY; }
            if (dKey) { wishX -= cosY; wishZ -= sinY; }
            if (aKey) { wishX += cosY; wishZ += sinY; }

            float len = (float) Math.sqrt(wishX * wishX + wishZ * wishZ);
            if (len < 0.001f) return;
            wishX /= len;
            wishZ /= len;

            Vec3 vel = self.getDeltaMovement();
            double hspeed = Math.sqrt(vel.x * vel.x + vel.z * vel.z);

            // Maintain current speed; enforce a minimum so we're always moving while keys held
            double baseSpeed = 0.22 * BHopMod.speedMultiplier;
            double targetSpeed = Math.max(hspeed, baseSpeed);

            // Set horizontal velocity direction to wish direction at target speed
            self.setDeltaMovement(wishX * targetSpeed, vel.y, wishZ * targetSpeed);
        }
    }
}
