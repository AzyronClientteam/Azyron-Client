package com.jigneshgaming.azyron.mixin;

import com.jigneshgaming.azyron.profile.MultiplayerPerformanceController;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Culls ultra-distant decorative/ambient entities in crowded multiplayer hubs and under
 * 3GB–8GB RAM / server-join load while preserving all nearby entities, players, hostiles, and projectiles.
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcher120Mixin {
    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true, require = 1)
    private <E extends Entity> void azyron$cullDistantCrowdEntities(
            E entity,
            Frustum frustum,
            double x,
            double y,
            double z,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (!MultiplayerPerformanceController.shouldRenderEntity(entity, x, y, z, MinecraftClient.getInstance())) {
            cir.setReturnValue(false);
        }
    }
}
