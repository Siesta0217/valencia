package com.valencia.mixin;

import com.valencia.HitboxMod;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Inflates {@code Entity.getBoundingBox()} returned AABB by
 * {@link HitboxMod#expand} so melee hit detection picks up edge swings.
 * Skips the local player (would mess up our own collision) and optionally
 * skips non-player entities when {@code playersOnly} is on.
 */
@Mixin(Entity.class)
public abstract class HitboxMixin {

    @Inject(
        method = "getBoundingBox()Lnet/minecraft/world/phys/AABB;",
        at = @At("RETURN"),
        cancellable = true
    )
    private void valencia$expandHitbox(CallbackInfoReturnable<AABB> cir) {
        if (!HitboxMod.isEnabled()) return;
        Entity self = (Entity) (Object) this;

        // Never modify our own box — would corrupt local collision / camera.
        Minecraft mc = Minecraft.getInstance();
        if (self == mc.player) return;

        if (HitboxMod.playersOnly && !(self instanceof Player)) return;

        AABB orig = cir.getReturnValue();
        if (orig == null) return;
        cir.setReturnValue(orig.inflate(HitboxMod.expand));
    }
}
