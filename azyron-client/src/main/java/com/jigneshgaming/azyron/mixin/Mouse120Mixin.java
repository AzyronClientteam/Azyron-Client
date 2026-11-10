package com.jigneshgaming.azyron.mixin;

import com.jigneshgaming.azyron.AzyronClient;
import com.jigneshgaming.azyron.module.impl.ZoomModule;
import com.jigneshgaming.azyron.profile.MultiplayerPerformanceController;
import com.jigneshgaming.azyron.util.CpsTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft 1.20.1 Mouse input hook for AZYRON CLIENT CPS tracking and scroll-to-zoom.
 * Targets 1.20.1 Intermediary descriptors:
 *  - onMouseButton: method_1601(JIII)V
 *  - onMouseScroll: method_1598(JDD)V
 */
@Mixin(Mouse.class)
public abstract class Mouse120Mixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(
            method = "method_1601(JIII)V",
            at = @At("HEAD"),
            remap = false,
            require = 0
    )
    private void azyron$trackCpsClicks120(long window, int button, int action, int mods, CallbackInfo ci) {
        try {
            CpsTracker.markMixinHookActive();
            if (action == GLFW.GLFW_PRESS && this.client != null && this.client.currentScreen == null) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    CpsTracker.recordLeftClick();
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    CpsTracker.recordRightClick();
                }
            }
        } catch (Throwable t) {
            MultiplayerPerformanceController.logRateLimitedWarning("1.20.1 Mouse CPS hook warning", t);
        }
    }

    @Inject(
            method = "method_1598(JDD)V",
            at = @At("HEAD"),
            cancellable = true,
            remap = false,
            require = 0
    )
    private void azyron$handleScrollZoom120(long window, double horizontal, double vertical, CallbackInfo ci) {
        try {
            if (this.client != null && this.client.currentScreen == null) {
                AzyronClient inst = AzyronClient.getInstance();
                if (inst != null && inst.getModuleManager() != null) {
                    ZoomModule zoom = inst.getModuleManager().getZoomModule();
                    if (zoom != null && zoom.onScroll(vertical)) {
                        ci.cancel();
                    }
                }
            }
        } catch (Throwable t) {
            MultiplayerPerformanceController.logRateLimitedWarning("1.20.1 Mouse scroll zoom warning", t);
        }
    }
}
