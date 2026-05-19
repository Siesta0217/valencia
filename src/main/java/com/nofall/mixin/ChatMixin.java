package com.nofall.mixin;

import com.nofall.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

// 攔截聊天訊息中的 .nf bind 指令（require=0 表示找不到 method 也不 crash）
@Mixin(LocalPlayer.class)
public class ChatMixin {

    @Inject(method = "chat(Ljava/lang/String;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void nofall$chatBind(String message, CallbackInfo ci) {
        handleBind(message, ci);
    }

    // 備用：部分版本方法名不同
    @Inject(method = "sendCommand(Ljava/lang/String;)V",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void nofall$cmdBind(String message, CallbackInfo ci) {
        handleBind("." + message, ci); // command 沒有前綴，補 . 再比對
    }

    private static void handleBind(String message, CallbackInfo ci) {
        if (!message.startsWith(".nf bind ")) return;
        ci.cancel();

        Minecraft mc = Minecraft.getInstance();
        String[] parts = message.split(" ");
        if (parts.length != 4) {
            msg(mc, "§c用法: .nf bind <nofall|xray|maceaura|noslow> <鍵名>");
            return;
        }

        String target = parts[2].toLowerCase();
        String keyName = parts[3].toUpperCase();
        int keyCode = resolveKey(keyName);
        if (keyCode == -1) {
            msg(mc, "§c找不到按鍵 \"" + keyName + "\"，例: G  F5  GRAVE");
            return;
        }

        ModConfig cfg = ModConfig.get();
        switch (target) {
            case "nofall"   -> cfg.nofallKey   = keyCode;
            case "xray"     -> cfg.xrayKey     = keyCode;
            case "maceaura" -> cfg.maceAuraKey = keyCode;
            case "noslow"   -> cfg.fastFoodKey = keyCode;
            default -> { msg(mc, "§c目標: nofall / xray / maceaura / noslow"); return; }
        }
        cfg.save();
        msg(mc, "§a[NoFall] §e" + target + " §a→ §e" + keyName);
    }

    private static void msg(Minecraft mc, String text) {
        if (mc.player != null)
            mc.player.displayClientMessage(Component.literal(text), false);
    }

    private static int resolveKey(String name) {
        try { return GLFW.class.getField("GLFW_KEY_" + name).getInt(null); }
        catch (Exception ignored) { return -1; }
    }
}
