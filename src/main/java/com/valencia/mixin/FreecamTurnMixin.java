package com.valencia.mixin;

import com.valencia.FreecamMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While Freecam is active, redirect the local player's mouse-look into the
 * freecam's own yaw/pitch instead of rotating the body. Cancelling the vanilla
 * turn keeps the body's rotation frozen, so aiming the freecam sends no
 * rotation packets — the server sees the player standing perfectly still.
 *
 * Guarded to the local player only; turn() on any other entity is untouched.
 */
@Mixin(Entity.class)
public abstract class FreecamTurnMixin {

    @Inject(method = "turn", at = @At("HEAD"), cancellable = true)
    private void freecam$redirectTurn(double yRot, double xRot, CallbackInfo ci) {
        if (!FreecamMod.isActive()) return;
        if ((Object) this != Minecraft.getInstance().player) return;
        FreecamMod.applyTurn(yRot, xRot);
        ci.cancel();
    }
}
