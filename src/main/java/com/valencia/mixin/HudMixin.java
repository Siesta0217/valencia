package com.valencia.mixin;

import com.valencia.NetherCoordMod;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class HudMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void valencia$hud(GuiGraphics g, DeltaTracker delta, CallbackInfo ci) {
        NetherCoordMod.render(g);
    }
}
