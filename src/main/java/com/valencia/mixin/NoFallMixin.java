package com.valencia.mixin;

import com.valencia.NoFallMod;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class NoFallMixin {

    @Unique
    private boolean nofall$savedOnGround;
    @Unique
    private boolean nofall$active;

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void nofall$beforeSendPosition(CallbackInfo ci) {
        nofall$active = false;
        if (!NoFallMod.isEnabled()) return;

        LivingEntity self = (LivingEntity) (Object) this;

        // Skip while elytra-flying — spoofing onGround mid-flight desyncs the
        // server's fall-flying state and breaks deploy/landing.
        if (self.isFallFlying()) return;

        // Smart mode: only spoof when the fall would actually deal damage, so the
        // server doesn't get an airborne onGround=true claim every single tick
        // (the pattern modern anti-cheats flag). Walking / small hops stay honest.
        if (NoFallMod.mode == 1 && ((Entity) (Object) this).fallDistance <= 2.0) return;

        nofall$active = true;
        nofall$savedOnGround = self.onGround();
        self.setOnGround(true);
    }

    @Inject(method = "sendPosition", at = @At("RETURN"))
    private void nofall$afterSendPosition(CallbackInfo ci) {
        if (!nofall$active) return;
        ((Entity) (Object) this).setOnGround(nofall$savedOnGround);
    }
}
