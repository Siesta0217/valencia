package com.valencia.mixin;

import com.valencia.ElytraGotoMod;
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
        if (message.startsWith(".nf goto")) {
            ci.cancel();
            handleGoto(message);
            return;
        }
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

    private static void handleGoto(String message) {
        Minecraft mc = Minecraft.getInstance();
        String[] parts = message.split("\\s+");
        // .nf goto stop
        if (parts.length == 3 && parts[2].equalsIgnoreCase("stop")) {
            ElytraGotoMod.stop();
            msg(mc, "§7[Goto] §cstopped");
            msg(mc, "§8tip: 想走路請先脫掉鞘翅 — vanilla MC 機制下，穿著鞘翅按 SPACE 在空中會自動展翅");
            return;
        }
        // .nf goto X Y Z   (Y optional — defaults to 64)
        if (parts.length == 4 || parts.length == 5) {
            try {
                double x = Double.parseDouble(parts[2]);
                double y = parts.length == 5 ? Double.parseDouble(parts[3]) : 64;
                double z = Double.parseDouble(parts[parts.length - 1]);
                ElytraGotoMod.setTarget(x, y, z);
                double dist = ElytraGotoMod.horizontalDistance();
                msg(mc, String.format("§a[Goto] §ftarget §e%.0f,%.0f,%.0f §7(%.0fm, ~%.0fs)",
                    x, y, z, dist, dist / 33.0));
                msg(mc, "§7Equip elytra + put fireworks in main/off hand, then jump off something.");
                return;
            } catch (NumberFormatException ignored) {}
        }
        msg(mc, "§cUsage: .nf goto <x> [y] <z>  |  .nf goto stop");
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
