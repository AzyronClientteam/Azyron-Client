package com.jigneshgaming.azyron.mixin;

import com.jigneshgaming.azyron.AzyronClient;
import com.jigneshgaming.azyron.module.impl.ZoomModule;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void azyron$applySmoothZoom(Camera camera, float tickDelta, boolean changingFov, CallbackInfoReturnable<Float> cir) {
        AzyronClient inst = AzyronClient.getInstance();
        if (inst != null && inst.getModuleManager() != null) {
            ZoomModule zoom = inst.getModuleManager().getZoomModule();
            if (zoom != null) {
                float base = cir.getReturnValueF();
                float adjusted = zoom.applyFov(base);
                if (adjusted != base) {
                    cir.setReturnValue(adjusted);
                }
            }
        }
    }
}
