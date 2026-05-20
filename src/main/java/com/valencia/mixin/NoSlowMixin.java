package com.valencia.mixin;

import com.valencia.NoSlowMod;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LocalPlayer.class)
public abstract class NoSlowMixin {

    @Shadow
    private boolean isSlowDueToUsingItem() {
        throw new AssertionError();
    }

    @Redirect(method = "modifyInput",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"),
            require = 0)
    private boolean nofall$keepFullInputSpeed(LocalPlayer self) {
        return !NoSlowMod.isEnabled() && self.isUsingItem();
    }

    @Redirect(method = "aiStep",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;isSlowDueToUsingItem()Z"),
            require = 0)
    private boolean nofall$keepSprintInAiStep(LocalPlayer self) {
        return !NoSlowMod.isEnabled() && ((NoSlowMixin) (Object) self).isSlowDueToUsingItem();
    }

    @Redirect(method = "canStartSprinting",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;isSlowDueToUsingItem()Z"),
            require = 0)
    private boolean nofall$keepSprintStart(LocalPlayer self) {
        return !NoSlowMod.isEnabled() && ((NoSlowMixin) (Object) self).isSlowDueToUsingItem();
    }
}
