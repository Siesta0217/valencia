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
    @Unique
    private int nofall$airborneTicks;

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void nofall$beforeSendPosition(CallbackInfo ci) {
        nofall$active = false;
        if (!NoFallMod.isEnabled()) return;

        LivingEntity self = (LivingEntity) (Object) this;

        // Skip while elytra-flying — spoofing onGround mid-flight desyncs the
        // server's fall-flying state and breaks deploy/landing. (Fall-flying is
        // exempt from vanilla's flight-kick check too, so nothing to reset here.)
        if (self.isFallFlying()) { nofall$airborneTicks = 0; return; }

        // Track consecutive airborne ticks for the flight-kick reset below.
        if (self.onGround()) nofall$airborneTicks = 0;
        else nofall$airborneTicks++;

        boolean spoof;
        if (NoFallMod.mode == 1) {
            // Smart: spoof only when a fall would actually deal damage, so the
            // server doesn't see an airborne onGround=true every tick (the
            // pattern modern anti-cheats flag). Walking / small hops stay honest.
            boolean fallHurts = ((Entity) (Object) this).fallDistance > 2.0;
            // Anti-kick: a vanilla server (allow-flight=false) kicks after ~80
            // airborne ticks. Force one onGround=true every 60 to reset that
            // counter so a sustained hover/flight isn't booted as "Flying".
            boolean antiKick = NoFallMod.noFlightKick && nofall$airborneTicks >= 60;
            if (antiKick) nofall$airborneTicks = 0;
            spoof = fallHurts || antiKick;
        } else {
            spoof = true;   // Always: every tick (also immune to the flight kick)
        }
        if (!spoof) return;

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
