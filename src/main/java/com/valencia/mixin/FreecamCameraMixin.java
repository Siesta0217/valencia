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
 * While Freecam is active, override the camera position with the freecam's
 * world position. Rotation is left as vanilla set it (from the player entity),
 * so the mouse still aims the freecam. Injected at TAIL so it replaces the
 * final position vanilla computed.
 */
@Mixin(Camera.class)
public abstract class FreecamCameraMixin {

    @Shadow protected abstract void setPosition(double x, double y, double z);

    @Inject(method = "setup", at = @At("TAIL"))
    private void freecam$override(Level level, Entity entity, boolean detached,
                                  boolean reverse, float partial, CallbackInfo ci) {
        if (FreecamMod.isActive()) setPosition(FreecamMod.x, FreecamMod.y, FreecamMod.z);
    }
}
