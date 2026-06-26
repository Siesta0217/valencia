package com.valencia.mixin;

import com.valencia.FreecamMod;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While Freecam is active, override BOTH the camera position and rotation with
 * the freecam's own values (driven by mouse-look via FreecamTurnMixin). The
 * body keeps its frozen rotation, so nothing leaks to the server. Injected at
 * TAIL so it replaces the final transform vanilla computed.
 */
@Mixin(Camera.class)
public abstract class FreecamCameraMixin {

    @Shadow protected abstract void setPosition(double x, double y, double z);
    @Shadow protected abstract void setRotation(float yRot, float xRot);

    @Inject(method = "setup", at = @At("TAIL"))
    private void freecam$override(Level level, Entity entity, boolean detached,
                                  boolean reverse, float partial, CallbackInfo ci) {
        if (FreecamMod.isActive()) {
            setRotation(FreecamMod.yaw, FreecamMod.pitch);
            setPosition(FreecamMod.x, FreecamMod.y, FreecamMod.z);
        }
    }
}
