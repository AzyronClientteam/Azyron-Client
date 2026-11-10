package com.jigneshgaming.azyron.mixin;

import com.jigneshgaming.azyron.AzyronClient;
import com.jigneshgaming.azyron.module.impl.ZoomModule;
import com.jigneshgaming.azyron.profile.MultiplayerPerformanceController;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Minecraft 1.20.1 smooth-zoom FOV hook.
 * In Minecraft 1.20.1, GameRenderer.getFov (method_3196) returns double (D) rather than float (F).
 */
@Mixin(GameRenderer.class)
public abstract class GameRenderer120Mixin {
    @Inject(
            method = "getFov",
            at = @At("RETURN"),
            cancellable = true,
            require = 1
    )
    private void azyron$applySmoothZoom120(
            Camera camera,
            float tickDelta,
            boolean changingFov,
            CallbackInfoReturnable<Double> cir
    ) {
        try {
            AzyronClient inst = AzyronClient.getInstance();
            if (inst != null && inst.getModuleManager() != null) {
                ZoomModule zoom = inst.getModuleManager().getZoomModule();
                Double baseObj = cir.getReturnValue();
                if (zoom != null && baseObj != null) {
                    float base = baseObj.floatValue();
                    float adjusted = zoom.applyFov(base);
                    if (adjusted != base) {
                        cir.setReturnValue((double) adjusted);
                    }
                }
            }
        } catch (Throwable t) {
            MultiplayerPerformanceController.logRateLimitedWarning("1.20.1 GameRenderer zoom warning", t);
        }
    }
}
