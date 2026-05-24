package com.valencia.mixin;

import com.valencia.CritMod;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class CritMixin {

    @Inject(method = "attack", at = @At("HEAD"))
    private void crit$onAttack(Entity target, CallbackInfo ci) {
        if (!CritMod.isActive()) return;
        if (!((Object) this instanceof LocalPlayer p)) return;
        if (!(target instanceof LivingEntity)) return;

        // Player.canCriticalAttack() requires: fallDistance > 0, !onGround,
        // !onClimbable, !isInWater, !isMobilityRestricted, !isPassenger,
        // !isSprinting, target instanceof LivingEntity, and attackStrengthScale > 0.9.
        // We only need to fix the onGround/fallDistance pair — bail on conditions
        // we can't fake without making it obvious.
        if (!p.onGround()) return;                             // already airborne, vanilla handles it
        if (p.getAttackStrengthScale(0.5f) <= 0.9f) return;    // cooldown not full
        if (p.isInWater() || p.onClimbable()) return;
        if (p.isPassenger() || p.isSprinting()) return;
        if (p.getVehicle() != null) return;

        // NCP-style mini-hop: two position packets that tell the server
        // we briefly left the ground. Real client position never changes,
        // so visually it's invisible. Server-side fallDistance becomes > 0
        // on the down packet, satisfying canCriticalAttack.
        double x = p.getX(), y = p.getY(), z = p.getZ();
        boolean hc = p.horizontalCollision;
        try {
            p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y + 0.0625, z, false, hc));
            p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y + 1.0E-5,  z, false, hc));
            p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y,            z, false, hc));
        } catch (Throwable ignored) {}
    }
}
