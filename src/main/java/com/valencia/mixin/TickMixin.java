package com.valencia.mixin;

import com.valencia.AutoEatMod;
import com.valencia.AutoFishMod;
import com.valencia.AutoTotemMod;
import com.valencia.AutoToolMod;
import com.valencia.AutoWalkMod;
import com.valencia.ElytraGotoMod;
import com.valencia.FastPlaceMod;
import com.valencia.FreecamMod;
import com.valencia.Keybinds;
import com.valencia.NoCrashMod;
import com.valencia.NukerMod;
import com.valencia.ScaffoldMod;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class TickMixin {

    @Shadow private int rightClickDelay;

    @Inject(method = "tick", at = @At("HEAD"))
    private void nofall$onTick(CallbackInfo ci) {
        Minecraft mc = (Minecraft) (Object) this;

        // Module toggle keys, Panic, and GUI open/close.
        Keybinds.poll(mc);

        // rightClickDelay lives behind a private field — reset it here where we
        // can @Shadow it, on behalf of FastPlace / Scaffold.
        if (FastPlaceMod.isActive()) rightClickDelay = 0;
        if (ScaffoldMod.consumeRightClickReset()) rightClickDelay = 0;

        // Per-tick module drivers.
        ElytraGotoMod.tick();
        AutoFishMod.tick();
        NoCrashMod.tick();
        AutoTotemMod.tick();
        FreecamMod.tick();
        AutoToolMod.tick();
        NukerMod.tick();
        AutoWalkMod.tick();
        AutoEatMod.tick();
    }
}
