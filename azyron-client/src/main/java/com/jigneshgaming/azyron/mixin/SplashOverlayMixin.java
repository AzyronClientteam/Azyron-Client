package com.jigneshgaming.azyron.mixin;

import com.jigneshgaming.azyron.AzyronClient;
import com.jigneshgaming.azyron.ui.AzyronLoadingRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders the custom AZYRON CLIENT loading screen ("Welcome to AZYRON CLIENT")
 * at @At("TAIL") so vanilla resource reload completion and Dynamic-FPS remain 100% compatible.
 */
@Mixin(SplashOverlay.class)
public abstract class SplashOverlayMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    private float progress;

    @Shadow
    private long reloadCompleteTime;

    @Unique
    private long azyron$fadeStartMs = -1L;

    @Inject(method = "render", at = @At("TAIL"))
    private void azyron$renderLoadingOverlay(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        AzyronClient inst = AzyronClient.getInstance();
        if (inst != null && inst.getConfig() != null && !inst.getConfig().isCustomLoadingScreenEnabled()) {
            return;
        }

        float fadeAlpha = 1.0f;
        if (this.reloadCompleteTime == -1L) {
            this.azyron$fadeStartMs = -1L;
        } else {
            if (this.azyron$fadeStartMs == -1L) {
                this.azyron$fadeStartMs = System.currentTimeMillis();
            }
            long fadeDuration = (inst != null && inst.getConfig() != null && inst.getConfig().isFastLoadingFade()) ? 450L : 850L;
            long elapsed = Math.max(0L, System.currentTimeMillis() - this.azyron$fadeStartMs);
            fadeAlpha = Math.max(0.0f, 1.0f - (elapsed / (float) fadeDuration));
        }

        AzyronLoadingRenderer.render(context, this.client, this.progress, fadeAlpha);
    }
}
