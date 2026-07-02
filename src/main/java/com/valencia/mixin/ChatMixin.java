package com.valencia.mixin;

import com.valencia.ElytraGotoMod;
import com.valencia.ModConfig;
import com.valencia.WaypointsMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.network.chat.Component;
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
        if (message.startsWith(".nf wp")) {
            ci.cancel();
            handleWaypoint(message);
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
            case "spearaura" -> cfg.spearAuraKey  = keyCode;
            case "nametag"   -> cfg.nameTagKey    = keyCode;
            default -> {
                msg(mc, "§cUnknown module. Options: nofall / xray / maceaura / spearaura / nametag / noslow / gui / bhop / step / killaura");
                return;
            }
        }
        cfg.save();
        msg(mc, "§a[Valencia] §e" + target + " §arebound to §e" + keyName);
    }

    private static void handleGoto(String message) {
        Minecraft mc = Minecraft.getInstance();
        String[] parts = message.split("\\s+");
        // .nf goto stop — glides down to the ground first if mid-flight, then
        // releases (the server has no cancel-fall-flying packet, so abandoning
        // control mid-glide just leaves the player stuck gliding/stalling).
        if (parts.length == 3 && parts[2].equalsIgnoreCase("stop")) {
            ElytraGotoMod.requestStop();
            return;
        }

        // .nf goto <name> — single non-numeric arg is a waypoint lookup.
        if (parts.length == 3 && !isNumeric(parts[2])) {
            ModConfig.Wp wp = WaypointsMod.all().get(parts[2]);
            if (wp == null) {
                msg(mc, "§c[Goto] 沒有叫 §e" + parts[2] + " §c的 waypoint — .nf wp list 看清單");
                return;
            }
            double[] pos = WaypointsMod.resolve(wp);
            if (pos == null) {
                msg(mc, "§c[Goto] 該 waypoint 在終界/另一側,無法跨維度換算");
                return;
            }
            ElytraGotoMod.setTarget(pos[0], pos[1], pos[2]);
            msg(mc, String.format("§a[Goto] §fwaypoint §e%s §7→ %.0f, %.0f, %.0f (%.0fm)",
                parts[2], pos[0], pos[1], pos[2], ElytraGotoMod.horizontalDistance()));
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

    /** `.nf wp add <name>` / `del <name>` / `list` */
    private static void handleWaypoint(String message) {
        Minecraft mc = Minecraft.getInstance();
        String[] parts = message.trim().split("\\s+");

        if (parts.length == 3 && parts[2].equalsIgnoreCase("list")) {
            var wps = WaypointsMod.all();
            if (wps.isEmpty()) { msg(mc, "§7[WP] 沒有 waypoint — .nf wp add <name> 存一個"); return; }
            msg(mc, "§b[WP] §f" + wps.size() + " waypoints:");
            for (var e : wps.entrySet()) {
                double[] pos = WaypointsMod.resolve(e.getValue());
                String where;
                if (pos == null) {
                    where = "§8(另一維度,無法換算)";
                } else if (mc.player != null) {
                    double dx = pos[0] - mc.player.getX(), dz = pos[2] - mc.player.getZ();
                    where = String.format("§7%.0f, %.0f, %.0f §8(%.0fm)",
                        pos[0], pos[1], pos[2], Math.sqrt(dx * dx + dz * dz));
                } else {
                    where = String.format("§7%.0f, %.0f, %.0f", e.getValue().x, e.getValue().y, e.getValue().z);
                }
                msg(mc, "§7 - §e" + e.getKey() + " " + where);
            }
            return;
        }

        if (parts.length == 4 && parts[2].equalsIgnoreCase("add")) {
            String name = parts[3];
            if (!name.matches("[A-Za-z0-9_\\-]+") || isNumeric(name)
                    || name.equalsIgnoreCase("stop") || name.equalsIgnoreCase("list")) {
                msg(mc, "§c[WP] 名稱只能是英數/底線/連字號,且不能是純數字或保留字");
                return;
            }
            if (mc.player == null || mc.level == null) return;
            WaypointsMod.add(name, mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                mc.level.dimension().toString());
            msg(mc, String.format("§a[WP] §e%s §a已存 §7(%.0f, %.0f, %.0f) — .nf goto %s 直飛",
                name, mc.player.getX(), mc.player.getY(), mc.player.getZ(), name));
            return;
        }

        if (parts.length == 4 && (parts[2].equalsIgnoreCase("del") || parts[2].equalsIgnoreCase("remove"))) {
            msg(mc, WaypointsMod.remove(parts[3])
                ? "§a[WP] §e" + parts[3] + " §a已刪除"
                : "§c[WP] 沒有叫 §e" + parts[3] + " §c的 waypoint");
            return;
        }

        msg(mc, "§cUsage: .nf wp add <name> | del <name> | list");
    }

    private static boolean isNumeric(String s) {
        try { Double.parseDouble(s); return true; }
        catch (NumberFormatException e) { return false; }
    }

    private static void msg(Minecraft mc, String text) {
        if (mc.player != null)
            mc.player.displayClientMessage(Component.literal(text), false);
    }

    private static int resolveKey(String name) {
        return ModConfig.keyCode(name);
    }
}
