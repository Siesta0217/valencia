package com.nofall.mixin;

import com.nofall.StepMod;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class StepMixin {

    @Shadow protected float maxUpStep;

    @Inject(method = "tick", at = @At("HEAD"))
    private void step$applyHeight(CallbackInfo ci) {
        if (!((Object)this instanceof net.minecraft.client.player.LocalPlayer)) return;
        maxUpStep = StepMod.isActive() ? 1.0f : 0.6f;
    }
}
