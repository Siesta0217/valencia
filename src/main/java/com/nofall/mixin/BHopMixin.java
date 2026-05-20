package com.nofall.mixin;

import com.nofall.BHopMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class BHopMixin {

    @Shadow protected boolean jumping;
    @Shadow protected abstract void jumpFromGround();

    @Inject(method = "aiStep", at = @At("HEAD"))
    private void bhop$jump(CallbackInfo ci) {
        if (!((Object)this instanceof net.minecraft.client.player.LocalPlayer)) return;
        if (!BHopMod.isActive()) return;
        if (jumping) return; // player is already pressing space, don't interfere

        LivingEntity self = (LivingEntity)(Object)this;
        if (!self.onGround()) return;

        Vec3 vel = self.getDeltaMovement();
        if (vel.x * vel.x + vel.z * vel.z < 1e-6) return;

        jumpFromGround();

        float mult = BHopMod.speedMultiplier;
        if (mult != 1.0f) {
            Vec3 nv = self.getDeltaMovement();
            self.setDeltaMovement(nv.x * mult, nv.y, nv.z * mult);
        }
    }
}
