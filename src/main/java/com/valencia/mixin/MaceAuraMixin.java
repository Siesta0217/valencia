package com.valencia.mixin;

import com.valencia.KillAuraMod;
import com.valencia.MaceAuraMod;
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
public abstract class MaceAuraMixin {

    @Unique private boolean nofall$rotModified = false;

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void maceAura$before(CallbackInfo ci) {
        nofall$rotModified = false;
        if (!MaceAuraMod.isActive()) return;

        Entity target = MaceAuraMod.findTarget();
        MaceAuraMod.currentTarget = target;
        if (target == null) return;

        LocalPlayer self = (LocalPlayer) (Object) this;

        MaceAuraMod.savedYRot = self.getYRot();
        MaceAuraMod.savedXRot = self.getXRot();

        float[] rot = MaceAuraMod.calcRotations(self, target);
        if (MaceAuraMod.smoothRot) {
            rot = KillAuraMod.smoothRotation(
                MaceAuraMod.savedYRot, MaceAuraMod.savedXRot,
                rot[0], rot[1], Math.max(1f, MaceAuraMod.maxTurnDeg));
        }
        // Keep the silent-aim delta on the player's real mouse GCD grid, same
        // as KillAura — arbitrary float rotations fail the anti-cheat GCD check.
        if (MaceAuraMod.gcdSnap) {
            rot[0] = KillAuraMod.snapGcd(MaceAuraMod.savedYRot, rot[0], true);
            rot[1] = Mth.clamp(KillAuraMod.snapGcd(MaceAuraMod.savedXRot, rot[1], false), -90f, 90f);
        }
        self.setYRot(rot[0]);
        self.setXRot(rot[1]);

        nofall$rotModified = true;
        MaceAuraMod.pendingAttack = true;
    }

    @Inject(method = "sendPosition", at = @At("RETURN"))
    private void maceAura$after(CallbackInfo ci) {
        if (!MaceAuraMod.isActive()) return;

        LocalPlayer self = (LocalPlayer) (Object) this;

        if (nofall$rotModified) {
            self.setYRot(MaceAuraMod.savedYRot);
            self.setXRot(MaceAuraMod.savedXRot);
        }

        if (MaceAuraMod.pendingAttack && MaceAuraMod.currentTarget != null) {
            Minecraft mc = Minecraft.getInstance();
            double distSq  = KillAuraMod.reachDistSq(self, MaceAuraMod.currentTarget);
            double rangeSq = (double) MaceAuraMod.ATTACK_RANGE * MaceAuraMod.ATTACK_RANGE;
            boolean los = !MaceAuraMod.raycast || KillAuraMod.canSee(self, MaceAuraMod.currentTarget);
            if (mc.gameMode != null
                    && distSq <= rangeSq
                    && los
                    && self.getAttackStrengthScale(0f) >= 0.5f) {
                mc.gameMode.attack(self, MaceAuraMod.currentTarget);
                self.swing(InteractionHand.MAIN_HAND);
            }
            MaceAuraMod.pendingAttack = false;
            MaceAuraMod.currentTarget = null;
        }
    }
}
