package com.valencia.mixin;

import com.valencia.TimerMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Player-side speedhack: tick the LocalPlayer extra times per Minecraft.tick().
 *
 * Each extra tick re-runs the player's full tick logic (movement, input,
 * position packets), so the player appears to move N× faster. A recursion
 * guard prevents the extra ticks from re-entering this injection.
 */
@Mixin(Minecraft.class)
public abstract class TimerMixin {

    @Shadow public LocalPlayer player;

    @Unique private static boolean timer$recursing = false;

    @Inject(method = "tick", at = @At("RETURN"))
    private void timer$extraTicks(CallbackInfo ci) {
        if (timer$recursing) return;
        if (!TimerMod.isActive() || player == null) return;

        TimerMod.accumulator += TimerMod.speed - 1.0f;
        int extras = (int) TimerMod.accumulator;
        TimerMod.accumulator -= extras;
        if (extras <= 0) return;

        timer$recursing = true;
        try {
            for (int i = 0; i < extras; i++) {
                if (player == null) break;
                player.tick();
            }
        } catch (Throwable ignored) {
        } finally {
            timer$recursing = false;
        }
    }
}
