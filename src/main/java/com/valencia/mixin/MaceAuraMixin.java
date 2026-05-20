package com.valencia.mixin;

import com.valencia.MaceAuraMod;
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
            double dist = self.distanceTo(MaceAuraMod.currentTarget);
            if (mc.gameMode != null
                    && dist <= MaceAuraMod.ATTACK_RANGE
                    && self.getAttackStrengthScale(0f) >= 0.5f) {
                mc.gameMode.attack(self, MaceAuraMod.currentTarget);
                self.swing(InteractionHand.MAIN_HAND);
            }
            MaceAuraMod.pendingAttack = false;
            MaceAuraMod.currentTarget = null;
        }
    }
}
