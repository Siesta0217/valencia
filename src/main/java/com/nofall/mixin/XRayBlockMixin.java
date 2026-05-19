package com.nofall.mixin;

import com.nofall.XRayMod;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class XRayBlockMixin {

    @Shadow
    public abstract Block getBlock();

    @Inject(method = "getRenderShape", at = @At("HEAD"), cancellable = true)
    private void xray$getRenderShape(CallbackInfoReturnable<RenderShape> cir) {
        if (XRayMod.isEnabled() && !XRayMod.isXRayBlock(getBlock())) {
            cir.setReturnValue(RenderShape.INVISIBLE);
        }
    }

    @Inject(method = "canOcclude", at = @At("HEAD"), cancellable = true)
    private void xray$canOcclude(CallbackInfoReturnable<Boolean> cir) {
        if (XRayMod.isEnabled()) {
            cir.setReturnValue(false);
        }
    }
}
