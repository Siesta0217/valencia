package com.valencia.mixin;

import com.valencia.KillAuraMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
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
    @Unique private float   nofall$lastTargetYaw  = 0f;

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

        // Quantize to the player's real mouse grid so the rotation deltas the
        // server sees are valid multiples of their sensitivity GCD (silent aim
        // that sends arbitrary floats fails the anti-cheat GCD check).
        if (KillAuraMod.gcdSnap) {
            newYaw   = KillAuraMod.snapGcd(KillAuraMod.savedYRot, newYaw, true);
            newPitch = Mth.clamp(KillAuraMod.snapGcd(KillAuraMod.savedXRot, newPitch, false), -90f, 90f);
        }
        nofall$lastTargetYaw = newYaw;

        self.setYRot(newYaw);
        self.setXRot(newPitch);

        // Visible Body: physically rotate body+head (3rd-person visible) but
        // leave the camera (yRot) alone so first-person view doesn't move.
        // yRot is restored in RETURN; yBodyRot/yHeadRot persist.
        if (KillAuraMod.visibleBody) {
            self.yBodyRot  = newYaw;
            self.yBodyRotO = newYaw;
            self.yHeadRot  = newYaw;
            self.yHeadRotO = newYaw;
        }

        nofall$killModified  = true;
        KillAuraMod.pendingAttack = true;
    }

    @Inject(method = "sendPosition", at = @At("RETURN"))
    private void killAura$after(CallbackInfo ci) {
        if (!KillAuraMod.isActive()) return;

        LocalPlayer self = (LocalPlayer)(Object)this;

        // Body Lock OFF (default): restore view rotation — silent aim
        // Body Lock ON: keep modified rotation — view physically snaps
        if (nofall$killModified && !KillAuraMod.bodyLock) {
            self.setYRot(KillAuraMod.savedYRot);
            self.setXRot(KillAuraMod.savedXRot);
        }

        // Visible Body: re-apply body/head rotation after the view restore so
        // body stays facing target even when camera snaps back to user's input.
        if (nofall$killModified && KillAuraMod.visibleBody) {
            self.yBodyRot  = nofall$lastTargetYaw;
            self.yBodyRotO = nofall$lastTargetYaw;
            self.yHeadRot  = nofall$lastTargetYaw;
            self.yHeadRotO = nofall$lastTargetYaw;
        }

        if (KillAuraMod.pendingAttack && KillAuraMod.currentTarget != null) {
            Minecraft mc = Minecraft.getInstance();

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
                int jitter = KillAuraMod.cpsJitter > 0
                    ? java.util.concurrent.ThreadLocalRandom.current().nextInt(KillAuraMod.cpsJitter + 1) : 0;
                nofall$nextAttackTick = self.tickCount + Math.max(1, KillAuraMod.attackDelay + jitter);
            }
            KillAuraMod.pendingAttack = false;
            KillAuraMod.currentTarget = null;
        }
    }
}
