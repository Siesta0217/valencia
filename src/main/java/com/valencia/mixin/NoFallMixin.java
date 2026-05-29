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
