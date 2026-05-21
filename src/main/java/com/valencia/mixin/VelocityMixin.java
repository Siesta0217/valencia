package com.valencia.mixin;

import com.valencia.VelocityMod;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class VelocityMixin {

    @Inject(method = "knockback", at = @At("HEAD"), cancellable = true)
    private void velocity$knockback(double strength, double ratioX, double ratioZ, CallbackInfo ci) {
        if (!((Object)this instanceof LocalPlayer)) return;
        if (!VelocityMod.isActive()) return;
        ci.cancel();
    }
}
