package com.valencia.mixin;

import com.valencia.VelocityMod;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;

/**
 * Explosion knockback (TNT, crystals, creepers) does NOT arrive through
 * ClientboundSetEntityMotionPacket — the server sends it inside
 * ClientboundExplodePacket as {@code playerKnockback}, so VelocityPacketMixin
 * never sees it. Redirect the packet's getter inside handleExplosion and scale
 * the vector before vanilla applies it. With horizontal/vertical at 0 this
 * fully cancels crystal/TNT knockback, matching the other two paths.
 */
@Mixin(ClientPacketListener.class)
public abstract class VelocityExplosionMixin {

    @Redirect(
        method = "handleExplosion",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/protocol/game/ClientboundExplodePacket;playerKnockback()Ljava/util/Optional;"
        ),
        require = 0
    )
    private Optional<Vec3> velocity$scaleExplosion(ClientboundExplodePacket packet) {
        Optional<Vec3> kb = packet.playerKnockback();
        if (!VelocityMod.isActive() || kb.isEmpty()) return kb;
        float h = VelocityMod.horizontal / 100f;
        float v = VelocityMod.vertical / 100f;
        Vec3 k = kb.get();
        return Optional.of(new Vec3(k.x * h, k.y * v, k.z * h));
    }
}
