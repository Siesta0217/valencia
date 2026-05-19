package com.nofall.mixin;

import com.nofall.NoFallMod;
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

        // 鞘翅飛行中不介入，避免卡傷害
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
