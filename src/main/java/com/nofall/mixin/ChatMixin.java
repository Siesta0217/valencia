package com.nofall.mixin;

import com.nofall.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// 攔截 ChatScreen.handleChatInput — 在封包送出前截取，適用於 1.21.11
@Mixin(ChatScreen.class)
public class ChatMixin {

    @Inject(method = "handleChatInput",
            at = @At("HEAD"), cancellable = true, require = 0)
    private void nofall$interceptChat(String message, boolean addToHistory, CallbackInfo ci) {
        if (!message.startsWith(".nf bind ")) return;
        ci.cancel();

        Minecraft mc = Minecraft.getInstance();
        String[] parts = message.split(" ");
        if (parts.length != 4) {
            msg(mc, "§c用法: .nf bind <nofall|xray|maceaura|noslow|gui> <鍵名>");
            return;
        }

        String target = parts[2].toLowerCase();
        String keyName = parts[3].toUpperCase();
        int keyCode = resolveKey(keyName);
        if (keyCode == -1) {
            msg(mc, "§c找不到按鍵 \"" + keyName + "\"，例: G  Z  RSHIFT  F5");
            return;
        }

        ModConfig cfg = ModConfig.get();
        switch (target) {
            case "nofall"   -> cfg.nofallKey   = keyCode;
            case "xray"     -> cfg.xrayKey     = keyCode;
            case "maceaura" -> cfg.maceAuraKey = keyCode;
            case "noslow"   -> cfg.fastFoodKey = keyCode;
            case "gui"      -> cfg.guiKey      = keyCode;
            default -> { msg(mc, "§c目標: nofall / xray / maceaura / noslow / gui"); return; }
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
