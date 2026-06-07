package com.valencia.mixin;

import com.valencia.FreecamMod;
import net.minecraft.client.player.ClientInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While Freecam is active, blank the player's movement input each tick so the
 * body stays put — WASD then only drives the detached camera. Rotation is not
 * touched, so the mouse still turns the view (and thus the freecam direction).
 */
@Mixin(ClientInput.class)
public abstract class ClientInputMixin {

    @Shadow public Input keyPresses;
    @Shadow protected Vec2 moveVector;

    @Inject(method = "tick", at = @At("TAIL"))
    private void freecam$freeze(CallbackInfo ci) {
        if (!FreecamMod.isActive()) return;
        keyPresses = Input.EMPTY;
        moveVector = Vec2.ZERO;
    }
}
