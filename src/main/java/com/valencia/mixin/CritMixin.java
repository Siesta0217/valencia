package com.valencia.mixin;

import com.valencia.CritMod;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class CritMixin {

    @Inject(method = "attack", at = @At("HEAD"))
    private void crit$onAttack(Entity target, CallbackInfo ci) {
        if (!CritMod.isActive()) return;
        if (!((Object)this instanceof LocalPlayer)) return;
        ((LivingEntity)(Object)this).fallDistance = 1.0f;
    }
}
