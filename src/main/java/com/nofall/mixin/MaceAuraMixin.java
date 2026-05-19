package com.nofall.mixin;

import com.nofall.MaceAuraMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class MaceAuraMixin {

    // HEAD: 封包發出前，偷偷把視角轉向目標
    @Inject(method = "sendPosition", at = @At("HEAD"))
    private void maceAura$before(CallbackInfo ci) {
        if (!MaceAuraMod.isActive()) return;

        Entity target = MaceAuraMod.findTarget();
        MaceAuraMod.currentTarget = target;
        if (target == null) return;

        LocalPlayer self = (LocalPlayer) (Object) this;

        // 儲存真實視角
        MaceAuraMod.savedYRot = self.getYRot();
        MaceAuraMod.savedXRot = self.getXRot();

        // 設定偽造視角（伺服器收到的方向）
        float[] rot = MaceAuraMod.calcRotations(self, target);
        self.setYRot(rot[0]);
        self.setXRot(rot[1]);

        MaceAuraMod.pendingAttack = true;
    }

    // RETURN: 封包發出後，立刻恢復視角 + 送出攻擊
    @Inject(method = "sendPosition", at = @At("RETURN"))
    private void maceAura$after(CallbackInfo ci) {
        if (!MaceAuraMod.isActive()) return;

        LocalPlayer self = (LocalPlayer) (Object) this;

        // 恢復客戶端真實視角（玩家完全看不出視角動了）
        self.setYRot(MaceAuraMod.savedYRot);
        self.setXRot(MaceAuraMod.savedXRot);

        // 攻擊冷卻 ≥ 90% 才出手
        if (MaceAuraMod.pendingAttack && MaceAuraMod.currentTarget != null) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.gameMode != null && self.getAttackStrengthScale(0f) >= 0.9f) {
                mc.gameMode.attack(self, MaceAuraMod.currentTarget);
                self.swing(InteractionHand.MAIN_HAND);
            }
            MaceAuraMod.pendingAttack = false;
            MaceAuraMod.currentTarget = null;
        }
    }
}
