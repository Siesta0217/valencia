package com.valencia.mixin;

import com.valencia.KillAuraMod;
import com.valencia.SpearAuraMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SpearAura attack pipeline — mirrors MaceAuraMixin's HEAD/RETURN around
 * `sendPosition` so server rotation lands on the target without disturbing
 * the visible camera. Adds spear-specific bits:
 *
 *  - JAB:    one discrete attack per tick when cooldown ≥ 50% and target
 *            is inside the spear's [MIN_REACH, MAX_REACH] sweet spot.
 *  - CHARGE: holds `keyAttack` for `chargeReleaseTicks` then releases. We
 *            keep silent-aim refreshed every tick so the swing lands on
 *            target even if it strafes during the charge.
 *  - AUTO:   picks per-tick based on `SpearAuraMod.effectiveMode()`.
 *
 *  - autoStepBack: when the best target is inside MIN_REACH, hold the
 *            back key so the player drifts out of the dead zone.
 */
@Mixin(LocalPlayer.class)
public abstract class SpearAuraMixin {

    @Unique private boolean spear$rotModified = false;

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void spearAura$before(CallbackInfo ci) {
        spear$rotModified = false;
        if (!SpearAuraMod.isActive()) {
            SpearAuraMod.stopCharge();
            SpearAuraMod.stopStepBack();
            return;
        }

        Entity target = SpearAuraMod.findTarget();
        SpearAuraMod.currentTarget = target;
        if (target == null) {
            SpearAuraMod.stopCharge();
            SpearAuraMod.stopStepBack();
            return;
        }

        LocalPlayer self = (LocalPlayer) (Object) this;

        // Silent aim — capture, override, mixin RETURN restores.
        SpearAuraMod.savedYRot = self.getYRot();
        SpearAuraMod.savedXRot = self.getXRot();
        float[] rot = SpearAuraMod.calcRotations(self, target);
        if (SpearAuraMod.smoothRot) {
            rot = KillAuraMod.smoothRotation(
                SpearAuraMod.savedYRot, SpearAuraMod.savedXRot,
                rot[0], rot[1], Math.max(1f, SpearAuraMod.maxTurnDeg));
        }
        self.setYRot(rot[0]);
        self.setXRot(rot[1]);
        spear$rotModified = true;
        SpearAuraMod.pendingAttack = true;
    }

    @Inject(method = "sendPosition", at = @At("RETURN"))
    private void spearAura$after(CallbackInfo ci) {
        if (!SpearAuraMod.isActive()) return;

        LocalPlayer self = (LocalPlayer) (Object) this;
        if (spear$rotModified) {
            self.setYRot(SpearAuraMod.savedYRot);
            self.setXRot(SpearAuraMod.savedXRot);
        }

        if (!SpearAuraMod.pendingAttack || SpearAuraMod.currentTarget == null) return;
        SpearAuraMod.pendingAttack = false;

        Entity target = SpearAuraMod.currentTarget;
        SpearAuraMod.currentTarget = null;
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null) return;

        double dist = Math.sqrt(KillAuraMod.reachDistSq(self, target));

        // Auto step-back: if target is too close, hold S until we drift out.
        if (SpearAuraMod.autoStepBack && dist < SpearAuraMod.MIN_REACH) {
            SpearAuraMod.startStepBack();
        } else {
            SpearAuraMod.stopStepBack();
        }

        // Dead zone — can't damage closer than MIN_REACH. Don't waste swings.
        if (dist < SpearAuraMod.MIN_REACH) {
            SpearAuraMod.stopCharge();
            return;
        }
        if (dist > SpearAuraMod.MAX_REACH) {
            SpearAuraMod.stopCharge();
            return;
        }

        // Line-of-sight gate — don't swing through walls when raycast is on.
        if (SpearAuraMod.raycast && !KillAuraMod.canSee(self, target)) {
            SpearAuraMod.stopCharge();
            return;
        }

        int eff = SpearAuraMod.effectiveMode();
        if (eff == SpearAuraMod.MODE_JAB) {
            SpearAuraMod.stopCharge();
            if (self.getAttackStrengthScale(0f) >= 0.5f) {
                mc.gameMode.attack(self, target);
                self.swing(InteractionHand.MAIN_HAND);
            }
        } else { // CHARGE
            SpearAuraMod.startCharge();
            SpearAuraMod.tickCharge();
            if (SpearAuraMod.chargeTicks >= SpearAuraMod.chargeReleaseTicks) {
                // Release — let vanilla resolve damage based on charge stage
                // and the silent-aimed rotation we just sent.
                SpearAuraMod.stopCharge();
            }
        }
    }
}
