package com.jigneshgaming.azyron.mixin;

import com.jigneshgaming.azyron.AzyronClient;
import com.jigneshgaming.azyron.module.ModuleManager;
import com.jigneshgaming.azyron.module.impl.CrosshairModule;
import com.jigneshgaming.azyron.module.impl.PotionHudModule;
import com.jigneshgaming.azyron.util.UiRenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(InGameHud.class)
public abstract class InGameHudMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true, require = 0)
    private void azyron$renderCustomCrosshair(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        AzyronClient inst = AzyronClient.getInstance();
        if (inst == null || inst.getModuleManager() == null || this.client.options.hudHidden) {
            return;
        }
        CrosshairModule crosshair = inst.getModuleManager().getCrosshairModule();
        if (crosshair != null && crosshair.isEnabled()) {
            crosshair.renderCrosshair(context, this.client);
            ci.cancel();
        }
    }

    @Inject(method = "renderStatusEffectOverlay", at = @At("HEAD"), cancellable = true, require = 0)
    private void azyron$hideVanillaEffectsIfConfigured(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        AzyronClient inst = AzyronClient.getInstance();
        if (inst != null && inst.getModuleManager() != null) {
            PotionHudModule potionHud = inst.getModuleManager().getPotionHudModule();
            if (potionHud != null && potionHud.shouldHideVanillaOverlay()) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "render", at = @At("TAIL"), require = 0)
    private void azyron$renderClientHud(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (!UiRenderUtils.isCoreTextShaderReady(this.client)) {
            return;
        }
        AzyronClient inst = AzyronClient.getInstance();
        if (inst != null) {
            ModuleManager mm = inst.getModuleManager();
            if (mm != null) {
                mm.renderHudOverlay(context, this.client);
            }
        }
    }
}
