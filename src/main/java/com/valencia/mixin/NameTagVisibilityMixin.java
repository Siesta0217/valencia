package com.valencia.mixin;

import com.valencia.NameTagMod;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class NameTagVisibilityMixin {

    @Inject(method = "shouldShowName", at = @At("HEAD"), cancellable = true)
    private void valencia$hideVanillaName(Entity entity, double distance, CallbackInfoReturnable<Boolean> cir) {
        if (NameTagMod.targets(entity)) {
            cir.setReturnValue(false);
        }
    }
}
