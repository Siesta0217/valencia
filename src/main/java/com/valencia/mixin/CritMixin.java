package com.valencia.mixin;

import com.valencia.CritMod;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Packet-crit ("hop crit"). Injected at {@code MultiPlayerGameMode.attack} HEAD,
 * which runs BEFORE the ServerboundInteractPacket is sent — so the mini-hop
 * position packets reach the server first, making it see fallDistance > 0 and
 * onGround = false when it resolves the attack → critical hit.
 *
 * The old version injected into {@code Player.attack}, which vanilla calls
 * AFTER sending the attack packet, so the spoof always arrived too late and
 * never crit. (That was the "CritHit does nothing" bug.)
 */
@Mixin(MultiPlayerGameMode.class)
public abstract class CritMixin {

    @Inject(method = "attack", at = @At("HEAD"))
    private void crit$onAttack(Player attacker, Entity target, CallbackInfo ci) {
        if (!CritMod.isActive()) return;
        if (!(attacker instanceof LocalPlayer p)) return;
        if (!(target instanceof LivingEntity)) return;

        // canCriticalAttack needs: fallDistance > 0, !onGround, !onClimbable,
        // !inWater, !restricted, !passenger, !sprinting, full charge. We only
        // spoof the onGround/fallDistance pair, so bail on anything else we
        // can't fake cleanly.
        if (!p.onGround()) return;                          // already airborne — vanilla crits normally
        if (p.getAttackStrengthScale(0.5f) <= 0.9f) return; // cooldown not full
        if (p.isInWater() || p.onClimbable()) return;
        if (p.isPassenger() || p.isSprinting()) return;     // sprinting blocks crits in vanilla
        if (p.getVehicle() != null) return;
        if (p.connection == null) return;

        // Mini-hop: up then back down, onGround=false. The down packet gives the
        // server a positive fallDistance. Real client position never changes, so
        // it's visually invisible. Sent here = before the attack packet.
        double x = p.getX(), y = p.getY(), z = p.getZ();
        boolean hc = p.horizontalCollision;
        try {
            p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y + 0.0625, z, false, hc));
            p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y + 1.0E-5,  z, false, hc));
            p.connection.send(new ServerboundMovePlayerPacket.Pos(x, y,            z, false, hc));
        } catch (Throwable t) { com.valencia.Log.once("crit mini-hop packet", t); }
    }
}
