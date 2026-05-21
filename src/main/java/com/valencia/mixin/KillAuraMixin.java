package com.valencia.mixin;

import com.valencia.KillAuraMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class KillAuraMixin {

    @Unique private boolean nofall$killModified   = false;
    @Unique private int     nofall$nextAttackTick = 0;

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void killAura$before(CallbackInfo ci) {
        nofall$killModified = false;
        if (!KillAuraMod.isActive()) return;

        Entity target = KillAuraMod.findTarget();
        KillAuraMod.currentTarget = target;
        if (target == null) return;

        LocalPlayer self = (LocalPlayer)(Object)this;
        KillAuraMod.savedYRot = self.getYRot();
        KillAuraMod.savedXRot = self.getXRot();

        float[] tgtRot = KillAuraMod.calcRotations(self, target);
        float newYaw, newPitch;
        if (KillAuraMod.smoothRot) {
            float[] sm = KillAuraMod.smoothRotation(
                KillAuraMod.savedYRot, KillAuraMod.savedXRot,
                tgtRot[0], tgtRot[1],
                Math.max(1f, KillAuraMod.maxTurnDeg)
            );
            newYaw   = sm[0];
            newPitch = sm[1];
        } else {
            newYaw   = tgtRot[0];
            newPitch = tgtRot[1];
        }
        self.setYRot(newYaw);
        self.setXRot(newPitch);
        nofall$killModified  = true;
        KillAuraMod.pendingAttack = true;
    }

    @Inject(method = "sendPosition", at = @At("RETURN"))
    private void killAura$after(CallbackInfo ci) {
        if (!KillAuraMod.isActive()) return;

        LocalPlayer self = (LocalPlayer)(Object)this;
        // Body Lock: skip restore so the player's view physically follows the
        // target (snap-aim / visible rotation). Default behavior restores
        // savedYRot/XRot to keep the local view unchanged (silent aim).
        if (nofall$killModified && !KillAuraMod.bodyLock) {
            self.setYRot(KillAuraMod.savedYRot);
            self.setXRot(KillAuraMod.savedXRot);
        }

        if (KillAuraMod.pendingAttack && KillAuraMod.currentTarget != null) {
            Minecraft mc = Minecraft.getInstance();

            // Hitbox-edge distance — matches server reach check, fixes ghost
            // swings on airborne / tall mobs where center-distance > 3.0 but
            // hitbox-edge distance is well within reach.
            double distSq  = KillAuraMod.reachDistSq(self, KillAuraMod.currentTarget);
            double rangeSq = (double) KillAuraMod.ATTACK_RANGE * KillAuraMod.ATTACK_RANGE;

            boolean inRange       = distSq <= rangeSq;
            boolean tickReady     = self.tickCount >= nofall$nextAttackTick;
            boolean cooldownReady = !KillAuraMod.waitCooldown
                                 || self.getAttackStrengthScale(0.5f) >= 1.0f;
            boolean lineOfSight   = !KillAuraMod.raycast
                                 || KillAuraMod.canSee(self, KillAuraMod.currentTarget);

            if (mc.gameMode != null && inRange && tickReady && cooldownReady && lineOfSight) {
                mc.gameMode.attack(self, KillAuraMod.currentTarget);
                self.swing(InteractionHand.MAIN_HAND);
                nofall$nextAttackTick = self.tickCount + Math.max(1, KillAuraMod.attackDelay);
            }
            KillAuraMod.pendingAttack = false;
            KillAuraMod.currentTarget = null;
        }
    }
}
