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

/**
 * NOTE: this hooks {@code LivingEntity.knockback}, which only runs locally
 * when the client itself computes knockback — i.e. single-player / client-side
 * mob hits. On a real multiplayer server, knockback arrives as an explicit
 * velocity packet and {@code knockback} is never called client-side, so this
 * mixin is effectively a no-op there. {@code VelocityPacketMixin} (added in
 * v1.7.5) is the path that actually handles multiplayer velocity. This one is
 * kept for the SP / client-side case; don't delete it expecting MP to break.
 */
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
