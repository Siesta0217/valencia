package com.valencia.mixin;

import com.valencia.ESPMod;
import com.valencia.KillAuraMod;
import com.valencia.MaceAuraMod;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class GlowMixin {

    @Inject(method = "isCurrentlyGlowing", at = @At("RETURN"), cancellable = true)
    private void glow$target(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return; // already glowing for another reason
        Entity self = (Entity)(Object)this;
        if ((KillAuraMod.isEnabled() && self == KillAuraMod.glowTarget)
         || (MaceAuraMod.isEnabled() && self == MaceAuraMod.glowTarget)
         || ESPMod.shouldGlow(self)) {
            cir.setReturnValue(true);
        }
    }
}
