package com.valencia.mixin;

import com.valencia.AutoTotemMod;
import com.valencia.BHopMod;
import com.valencia.ClickGuiScreen;
import com.valencia.CritMod;
import com.valencia.ElytraGotoMod;
import com.valencia.FastPlaceMod;
import com.valencia.KillAuraMod;
import com.valencia.MaceAuraMod;
import com.valencia.ModConfig;
import com.valencia.NameTagMod;
import com.valencia.NoFallMod;
import com.valencia.NoSlowMod;
import com.valencia.ScaffoldMod;
import com.valencia.SpearAuraMod;
import com.valencia.StepMod;
import com.valencia.TimerMod;
import com.valencia.VelocityMod;
import com.valencia.XRayMod;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class TickMixin {

    @Shadow private int rightClickDelay;

    @Unique private boolean nofall$prevN   = false;
    @Unique private boolean nofall$prevX   = false;
    @Unique private boolean nofall$prevM   = false;
    @Unique private boolean nofall$prevG   = false;
    @Unique private boolean nofall$prevGui = false;
    @Unique private boolean nofall$prevB   = false;
    @Unique private boolean nofall$prevH   = false;
    @Unique private boolean nofall$prevK   = false;
    @Unique private boolean nofall$prevVel = false;
    @Unique private boolean nofall$prevFP  = false;
    @Unique private boolean nofall$prevCr  = false;
    @Unique private boolean nofall$prevSc  = false;
    @Unique private boolean nofall$prevTm  = false;
    @Unique private boolean nofall$prevSp  = false;
    @Unique private boolean nofall$prevNt  = false;
    @Unique private boolean nofall$prevAt  = false;
    @Unique private boolean nofall$prevPan = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void nofall$onTick(CallbackInfo ci) {
        Minecraft mc = (Minecraft)(Object)this;

        long handle = GLFW.glfwGetCurrentContext();
        if (handle == 0L) return;

        ModConfig cfg = ModConfig.get();
        boolean nDown   = GLFW.glfwGetKey(handle, cfg.nofallKey)     == GLFW.GLFW_PRESS;
        boolean xDown   = GLFW.glfwGetKey(handle, cfg.xrayKey)       == GLFW.GLFW_PRESS;
        boolean mDown   = GLFW.glfwGetKey(handle, cfg.maceAuraKey)   == GLFW.GLFW_PRESS;
        boolean gDown   = GLFW.glfwGetKey(handle, cfg.noSlowKey)     == GLFW.GLFW_PRESS;
        boolean guiDown = GLFW.glfwGetKey(handle, cfg.guiKey)        == GLFW.GLFW_PRESS;
        boolean bDown   = GLFW.glfwGetKey(handle, cfg.bhopKey)       == GLFW.GLFW_PRESS;
        boolean hDown   = GLFW.glfwGetKey(handle, cfg.stepKey)       == GLFW.GLFW_PRESS;
        boolean kDown   = GLFW.glfwGetKey(handle, cfg.killAuraKey)   == GLFW.GLFW_PRESS;
        boolean velDown = GLFW.glfwGetKey(handle, cfg.velocityKey)   == GLFW.GLFW_PRESS;
        boolean fpDown  = GLFW.glfwGetKey(handle, cfg.fastPlaceKey)  == GLFW.GLFW_PRESS;
        boolean crDown  = GLFW.glfwGetKey(handle, cfg.critKey)       == GLFW.GLFW_PRESS;
        boolean scDown  = GLFW.glfwGetKey(handle, cfg.scaffoldKey)   == GLFW.GLFW_PRESS;
        boolean tmDown  = GLFW.glfwGetKey(handle, cfg.timerKey)      == GLFW.GLFW_PRESS;
        boolean spDown  = GLFW.glfwGetKey(handle, cfg.spearAuraKey)  == GLFW.GLFW_PRESS;
        boolean ntDown  = GLFW.glfwGetKey(handle, cfg.nameTagKey)    == GLFW.GLFW_PRESS;
        boolean atDown  = GLFW.glfwGetKey(handle, cfg.autoTotemKey)  == GLFW.GLFW_PRESS;
        boolean panDown = GLFW.glfwGetKey(handle, cfg.panicKey)      == GLFW.GLFW_PRESS;

        if (mc.screen == null) {
            if (nDown   && !nofall$prevN)   { NoFallMod.toggleManual();  saveEnabled(); msg(mc, "§7[NoFall] "     + state(NoFallMod.isEnabled()));     }
            if (xDown   && !nofall$prevX)   { XRayMod.toggle();          saveEnabled(); msg(mc, "§7[XRay] "       + state(XRayMod.isEnabled()));       }
            if (mDown   && !nofall$prevM)   { MaceAuraMod.toggle();      saveEnabled(); msg(mc, "§7[MaceAura] "   + state(MaceAuraMod.isEnabled()));   }
            if (gDown   && !nofall$prevG)   { NoSlowMod.toggle();        saveEnabled(); msg(mc, "§7[NoSlow] "     + state(NoSlowMod.isEnabled()));     }
            if (bDown   && !nofall$prevB)   { BHopMod.toggle();          saveEnabled(); msg(mc, "§7[BHop] "       + state(BHopMod.isEnabled()));       }
            if (hDown   && !nofall$prevH)   { StepMod.toggle();          saveEnabled(); msg(mc, "§7[Step] "       + state(StepMod.isEnabled()));       }
            if (kDown   && !nofall$prevK)   { KillAuraMod.toggle();      saveEnabled(); msg(mc, "§7[KillAura] "   + state(KillAuraMod.isEnabled()));   }
            if (velDown && !nofall$prevVel) { VelocityMod.toggle();      saveEnabled(); msg(mc, "§7[Velocity] "   + state(VelocityMod.isEnabled()));   }
            if (fpDown  && !nofall$prevFP)  { FastPlaceMod.toggle();     saveEnabled(); msg(mc, "§7[FastPlace] "  + state(FastPlaceMod.isEnabled()));  }
            if (crDown  && !nofall$prevCr)  { CritMod.toggle();          saveEnabled(); msg(mc, "§7[CritHit] "    + state(CritMod.isEnabled()));       }
            if (scDown  && !nofall$prevSc)  { ScaffoldMod.toggle();      saveEnabled(); msg(mc, "§7[Scaffold] "   + state(ScaffoldMod.isEnabled()));   }
            if (tmDown  && !nofall$prevTm)  { TimerMod.toggle();         saveEnabled(); msg(mc, "§7[Timer] "      + state(TimerMod.isEnabled()));      }
            if (spDown  && !nofall$prevSp)  { SpearAuraMod.toggle();     saveEnabled(); msg(mc, "§7[SpearAura] "  + state(SpearAuraMod.isEnabled())); }
            if (ntDown  && !nofall$prevNt)  { NameTagMod.toggle();       saveEnabled(); msg(mc, "§7[NameTag] "    + state(NameTagMod.isEnabled())); }
            if (atDown  && !nofall$prevAt)  { AutoTotemMod.toggle();     saveEnabled(); msg(mc, "§7[AutoTotem] "  + state(AutoTotemMod.isEnabled())); }
            if (panDown && !nofall$prevPan) { nofall$panic(mc); }
            if (guiDown && !nofall$prevGui) mc.setScreen(new ClickGuiScreen());
        } else if (mc.screen instanceof ClickGuiScreen && guiDown && !nofall$prevGui) {
            mc.setScreen(null);
        }

        if (FastPlaceMod.isActive()) rightClickDelay = 0;
        if (ScaffoldMod.consumeRightClickReset()) rightClickDelay = 0;

        ElytraGotoMod.tick();
        com.valencia.AutoFishMod.tick();
        com.valencia.NoCrashMod.tick();
        AutoTotemMod.tick();

        nofall$prevN   = nDown;
        nofall$prevX   = xDown;
        nofall$prevM   = mDown;
        nofall$prevG   = gDown;
        nofall$prevGui = guiDown;
        nofall$prevB   = bDown;
        nofall$prevH   = hDown;
        nofall$prevK   = kDown;
        nofall$prevVel = velDown;
        nofall$prevFP  = fpDown;
        nofall$prevCr  = crDown;
        nofall$prevSc  = scDown;
        nofall$prevTm  = tmDown;
        nofall$prevSp  = spDown;
        nofall$prevNt  = ntDown;
        nofall$prevAt  = atDown;
        nofall$prevPan = panDown;
    }

    /** Panic: kill every gameplay-affecting module in one keypress (go legit). */
    @Unique
    private static void nofall$panic(Minecraft mc) {
        if (NoFallMod.isEnabled())      NoFallMod.toggleManual();
        if (KillAuraMod.isEnabled())    KillAuraMod.toggle();
        if (MaceAuraMod.isEnabled())    MaceAuraMod.toggle();
        if (SpearAuraMod.isEnabled())   SpearAuraMod.toggle();
        if (CritMod.isEnabled())        CritMod.toggle();
        if (ScaffoldMod.isEnabled())    ScaffoldMod.toggle();
        if (TimerMod.isEnabled())       TimerMod.toggle();
        if (BHopMod.isEnabled())        BHopMod.toggle();
        if (StepMod.isEnabled())        StepMod.toggle();
        if (VelocityMod.isEnabled())    VelocityMod.toggle();
        if (FastPlaceMod.isEnabled())   FastPlaceMod.toggle();
        if (NoSlowMod.isEnabled())      NoSlowMod.toggle();
        if (AutoTotemMod.isEnabled())   AutoTotemMod.toggle();
        if (ElytraGotoMod.isEnabled())  ElytraGotoMod.toggle();
        if (com.valencia.AutoFishMod.isEnabled()) com.valencia.AutoFishMod.toggle();
        if (com.valencia.NoCrashMod.isEnabled())  com.valencia.NoCrashMod.toggle();
        saveEnabled();
        msg(mc, "§c[Panic] all modules OFF");
    }

    private static void saveEnabled() {
        ModConfig cfg = ModConfig.get();
        cfg.nofallEnabled     = NoFallMod.isEnabled();
        cfg.xrayEnabled       = XRayMod.isEnabled();
        cfg.maceAuraEnabled   = MaceAuraMod.isEnabled();
        cfg.noSlowEnabled     = NoSlowMod.isEnabled();
        cfg.bhopEnabled       = BHopMod.isEnabled();
        cfg.stepEnabled       = StepMod.isEnabled();
        cfg.killAuraEnabled   = KillAuraMod.isEnabled();
        cfg.velocityEnabled   = VelocityMod.isEnabled();
        cfg.fastPlaceEnabled  = FastPlaceMod.isEnabled();
        cfg.critEnabled       = CritMod.isEnabled();
        cfg.scaffoldEnabled   = ScaffoldMod.isEnabled();
        cfg.timerEnabled      = TimerMod.isEnabled();
        cfg.spearAuraEnabled  = SpearAuraMod.isEnabled();
        cfg.nameTagEnabled    = NameTagMod.isEnabled();
        cfg.autoTotemEnabled  = AutoTotemMod.isEnabled();
        cfg.save();
    }

    private static String state(boolean on) {
        return on ? "§aON" : "§cOFF";
    }

    private static void msg(Minecraft mc, String text) {
        if (mc.player != null)
            mc.player.displayClientMessage(Component.literal(text), true);
    }
}
