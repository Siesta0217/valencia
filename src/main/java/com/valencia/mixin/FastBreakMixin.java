package com.valencia.mixin;

import com.valencia.FastBreakMod;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * FastBreak: max the client destroy progress at the start of each
 * continueDestroyBlock tick. Vanilla then adds this tick's progress on top,
 * pushing it past 1.0 so the block breaks immediately (≈1 tick after the dig
 * starts). Server-side break-time validation still applies — see FastBreakMod.
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class FastBreakMixin {

    @Shadow private float destroyProgress;

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void fastBreak$boost(BlockPos pos, Direction face, CallbackInfoReturnable<Boolean> cir) {
        if (FastBreakMod.isActive()) this.destroyProgress = 1.0F;
    }
}
