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

        // Optional dimension-frame prefix: ow / nether (aliases: o / n).
        // Lets the user say "these coords are in the overworld frame" so we
        // auto-convert (÷8 or ×8) when standing in the other dimension. Y is
        // never scaled — vanilla portals keep Y unchanged across dimensions.
        int argStart = 2;
        String frame = null;
        if (parts.length >= 4) {
            String tag = parts[2].toLowerCase();
            if (tag.equals("ow") || tag.equals("o") || tag.equals("overworld")) {
                frame = "ow"; argStart = 3;
            } else if (tag.equals("nether") || tag.equals("n")) {
                frame = "nether"; argStart = 3;
            }
        }

        int argCount = parts.length - argStart;
        // X Y Z, or X Z (Y defaults to 64)
        if (argCount == 2 || argCount == 3) {
            try {
                double x = Double.parseDouble(parts[argStart]);
                double y = argCount == 3 ? Double.parseDouble(parts[argStart + 1]) : 64;
                double z = Double.parseDouble(parts[parts.length - 1]);

                boolean inNether = mc.level != null
                    && mc.level.dimension().toString().contains("the_nether");
                String note = null;
                if ("ow".equals(frame) && inNether) {
                    x /= 8.0; z /= 8.0;
                    note = "OW→Nether ÷8";
                } else if ("nether".equals(frame) && !inNether) {
                    x *= 8.0; z *= 8.0;
                    note = "Nether→OW ×8";
                }

                ElytraGotoMod.setTarget(x, y, z);
                double dist = ElytraGotoMod.horizontalDistance();
                String header = note != null
                    ? String.format("§a[Goto] §ftarget §e%.0f,%.0f,%.0f §7(%s, %.0fm, ~%.0fs)",
                        x, y, z, note, dist, dist / 33.0)
                    : String.format("§a[Goto] §ftarget §e%.0f,%.0f,%.0f §7(%.0fm, ~%.0fs)",
                        x, y, z, dist, dist / 33.0);
                msg(mc, header);
                msg(mc, "§7Equip elytra + put fireworks in main/off hand, then jump off something.");
                return;
            } catch (NumberFormatException ignored) {}
        }
        msg(mc, "§cUsage: .nf goto [ow|nether] <x> [y] <z>  |  .nf goto stop");
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
