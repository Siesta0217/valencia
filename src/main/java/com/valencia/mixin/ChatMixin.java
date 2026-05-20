package com.valencia.mixin;

import com.valencia.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
            msg(mc, "§cUsage: .nf bind <module> <key>");
            return;
        }

        String target  = parts[2].toLowerCase();
        String keyName = parts[3].toUpperCase();
        int keyCode    = resolveKey(keyName);
        if (keyCode == -1) {
            msg(mc, "§cUnknown key \"" + keyName + "\" — examples: G  Z  RIGHT_SHIFT  F5");
            return;
        }

        ModConfig cfg = ModConfig.get();
        switch (target) {
            case "nofall"    -> cfg.nofallKey    = keyCode;
            case "xray"      -> cfg.xrayKey      = keyCode;
            case "maceaura"  -> cfg.maceAuraKey  = keyCode;
            case "noslow"    -> cfg.noSlowKey     = keyCode;
            case "gui"       -> cfg.guiKey        = keyCode;
            case "bhop"      -> cfg.bhopKey       = keyCode;
            case "step"      -> cfg.stepKey       = keyCode;
            case "killaura"  -> cfg.killAuraKey   = keyCode;
            default -> {
                msg(mc, "§cUnknown module. Options: nofall / xray / maceaura / noslow / gui / bhop / step / killaura");
                return;
            }
        }
        cfg.save();
        msg(mc, "§a[Valencia] §e" + target + " §arebound to §e" + keyName);
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
