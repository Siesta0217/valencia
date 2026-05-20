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

    @Unique private boolean nofall$killModified = false;
    @Unique private int     nofall$atkTimer     = 0;

    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void killAura$before(CallbackInfo ci) {
        nofall$killModified = false;
        if (!KillAuraMod.isActive()) {
            nofall$atkTimer = 0;
            return;
        }

        Entity target = KillAuraMod.findTarget();
        KillAuraMod.currentTarget = target;
        if (target == null) return;

        LocalPlayer self = (LocalPlayer)(Object)this;
        KillAuraMod.savedYRot = self.getYRot();
        KillAuraMod.savedXRot = self.getXRot();
        float[] rot = KillAuraMod.calcRotations(self, target);
        self.setYRot(rot[0]);
        self.setXRot(rot[1]);
        nofall$killModified = true;
        KillAuraMod.pendingAttack = true;
    }

    @Inject(method = "sendPosition", at = @At("RETURN"))
    private void killAura$after(CallbackInfo ci) {
        if (!KillAuraMod.isActive()) return;

        LocalPlayer self = (LocalPlayer)(Object)this;
        if (nofall$killModified) {
            self.setYRot(KillAuraMod.savedYRot);
            self.setXRot(KillAuraMod.savedXRot);
        }

        // Tick-based attack timer (independent of weapon cooldown)
        nofall$atkTimer--;

        if (KillAuraMod.pendingAttack && KillAuraMod.currentTarget != null) {
            Minecraft mc = Minecraft.getInstance();
            double dist = self.distanceTo(KillAuraMod.currentTarget);
            if (mc.gameMode != null
                    && dist <= KillAuraMod.ATTACK_RANGE
                    && nofall$atkTimer <= 0) {
                mc.gameMode.attack(self, KillAuraMod.currentTarget);
                self.swing(InteractionHand.MAIN_HAND);
                nofall$atkTimer = KillAuraMod.attackDelay;
            }
            KillAuraMod.pendingAttack = false;
            KillAuraMod.currentTarget = null;
        }
    }
}
