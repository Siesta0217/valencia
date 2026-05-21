package com.valencia.mixin;

import com.valencia.ScaffoldMod;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class ScaffoldMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void scaffold$tick(CallbackInfo ci) {
        ScaffoldMod.tick();
    }
}
