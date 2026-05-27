package com.valencia.mixin;

import com.valencia.VelocityMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Scale the velocity that arrives via ClientboundSetEntityMotionPacket.
 *
 * In multiplayer the server runs LivingEntity.knockback() on its side and
 * then sends the resulting velocity to the client as a packet — the
 * client never calls knockback() itself. The old VelocityMixin (hooked on
 * LivingEntity.knockback) therefore never ran for actual PvP knockback,
 * which is why setting horizontal / vertical to 0 still let knockback
 * through.
 *
 * Fix: redirect the lerpMotion(Vec3) call inside handleSetEntityMotion.
 * If the affected entity is the local player and Velocity is active,
 * scale the motion before applying.
 */
@Mixin(ClientPacketListener.class)
public abstract class VelocityPacketMixin {

    @Redirect(
        method = "handleSetEntityMotion",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;lerpMotion(Lnet/minecraft/world/phys/Vec3;)V"
        )
    )
    private void velocity$scaleMotion(Entity self, Vec3 motion) {
        if (VelocityMod.isActive() && self == Minecraft.getInstance().player) {
            float h = VelocityMod.horizontal / 100f;
            float v = VelocityMod.vertical / 100f;
            self.lerpMotion(new Vec3(motion.x * h, motion.y * v, motion.z * h));
        } else {
            self.lerpMotion(motion);
        }
    }
}
