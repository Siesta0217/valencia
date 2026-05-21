package com.valencia.mixin;

import com.valencia.VelocityMod;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class VelocityMixin {

    // Sample player velocity before vanilla knockback runs, then read it after
    // and scale the *delta* (what knockback actually added). Lets us cleanly
    // apply horizontal/vertical percentages without re-implementing knockback.
    @Unique private Vec3 nofall$preKb = null;

    @Inject(method = "knockback", at = @At("HEAD"))
    private void velocity$kbHead(double strength, double x, double z, CallbackInfo ci) {
        if (!((Object)this instanceof LocalPlayer)) return;
        if (!VelocityMod.isActive()) return;
        nofall$preKb = ((LivingEntity)(Object)this).getDeltaMovement();
    }

    @Inject(method = "knockback", at = @At("RETURN"))
    private void velocity$kbReturn(double strength, double x, double z, CallbackInfo ci) {
        if (nofall$preKb == null) return;
        if (!((Object)this instanceof LocalPlayer)) { nofall$preKb = null; return; }
        if (!VelocityMod.isActive())                { nofall$preKb = null; return; }

        LivingEntity self = (LivingEntity)(Object)this;
        Vec3 post = self.getDeltaMovement();
        Vec3 pre  = nofall$preKb;
        nofall$preKb = null;

        float h = VelocityMod.horizontal / 100f;
        float v = VelocityMod.vertical   / 100f;

        // delta = what knockback contributed
        double dx = post.x - pre.x;
        double dy = post.y - pre.y;
        double dz = post.z - pre.z;

        self.setDeltaMovement(
            pre.x + dx * h,
            pre.y + dy * v,
            pre.z + dz * h
        );
    }
}
