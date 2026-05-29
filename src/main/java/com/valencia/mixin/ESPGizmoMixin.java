package com.valencia.mixin;

import com.valencia.ESPMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.debug.DebugRenderer;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * World-space ESP rendered through vanilla's own Gizmos pipeline.
 *
 * Previous revisions did this in 2D on the HUD by projecting each entity
 * AABB via {@code projectPointToScreen} then drawing line segments with
 * {@code GuiGraphics.fill}. That works visually but always lags vanilla
 * entity rendering by a partial-tick of jitter, because:
 *   - the HUD pass runs AFTER the world pass with a different camera
 *     snapshot,
 *   - {@code projectPointToScreen} internally uses partialTick = 0,
 *   - entity positions use the current partialTick.
 * Each of those is sub-pixel by itself but stacks into visible swim.
 *
 * The vanilla F3+B hitbox path emits {@link Gizmos#cuboid} during
 * {@link DebugRenderer#emitGizmos}, which {@link net.minecraft.client.renderer.LevelRenderer}
 * wraps in a {@code Gizmos.withCollector(...)} scope so the cuboid lands
 * in the same collector that F3+B uses. The collector is then drawn in
 * the same world pass with the same matrices vanilla used for entities,
 * giving us frame-perfect smoothness for free.
 *
 * Only active when ESP's selected style is HITBOX. Other styles
 * (Corners / Outline / Filled) stay HUD-based in {@link com.valencia.ESPRenderer}.
 */
@Mixin(DebugRenderer.class)
public abstract class ESPGizmoMixin {

    @Inject(method = "emitGizmos", at = @At("TAIL"))
    private void valencia$espGizmos(Frustum frustum,
                                     double camX, double camY, double camZ,
                                     float partialTick,
                                     CallbackInfo ci) {
        if (!ESPMod.isEnabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        int color = 0xFF000000
            | ((ESPMod.red   & 0xFF) << 16)
            | ((ESPMod.green & 0xFF) << 8)
            |  (ESPMod.blue  & 0xFF);
        GizmoStyle style = GizmoStyle.stroke(color);
        double maxDistSq = (double) ESPMod.maxDistance * ESPMod.maxDistance;

        for (Entity e : mc.level.entitiesForRendering()) {
            if (!ESPMod.targets(e)) continue;
            if (!e.isAlive()) continue;

            AABB bb = e.getBoundingBox();
            if (!frustum.isVisible(bb)) continue;

            double dx = e.getX() - camX;
            double dy = e.getY() - camY;
            double dz = e.getZ() - camZ;
            if (dx * dx + dy * dy + dz * dz > maxDistSq) continue;

            // Render-interpolated AABB: shift current bbox by (renderPos - currentPos).
            Vec3 entityPos = e.position();
            Vec3 renderPos = e.getPosition(partialTick);
            AABB renderBox = bb.move(renderPos.subtract(entityPos));

            // setAlwaysOnTop = render with no depth test, so the box shows
            // through walls (standard ESP). Without it the gizmo is depth-
            // tested and gets occluded by terrain.
            Gizmos.cuboid(renderBox, style).setAlwaysOnTop();
        }
    }
}
