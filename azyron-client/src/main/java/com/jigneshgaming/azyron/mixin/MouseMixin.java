package com.jigneshgaming.azyron.mixin;

import com.jigneshgaming.azyron.AzyronClient;
import com.jigneshgaming.azyron.module.impl.ZoomModule;
import com.jigneshgaming.azyron.util.CpsTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.Mouse;
import net.minecraft.client.input.MouseInput;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Surrogate;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft 1.21.11 Mouse input hook for AZYRON CLIENT CPS tracking and scroll-to-zoom.
 * Targets Mouse.onMouseButton(long, MouseInput, int) [method_1601(JLnet/minecraft/class_11910;I)V]
 * with @Surrogate overloads and require = 0 so startup never crashes even if wrapped by another mod.
 */
@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(
            method = "onMouseButton(JLnet/minecraft/client/input/MouseInput;I)V",
            at = @At("HEAD"),
            require = 0
    )
    private void azyron$trackCpsClicks(long window, MouseInput input, int action, CallbackInfo ci) {
        try {
            CpsTracker.markMixinHookActive();
            if (action == GLFW.GLFW_PRESS && this.client != null && this.client.currentScreen == null && input != null) {
                int btn = input.button();
                if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    CpsTracker.recordLeftClick();
                } else if (btn == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    CpsTracker.recordRightClick();
                }
            }
        } catch (Throwable ignored) {
            // Never allow click telemetry to disrupt input handling
        }
    }

    @Surrogate
    private void azyron$trackCpsClicks(long window, int button, int action, int mods, CallbackInfo ci) {
        try {
            CpsTracker.markMixinHookActive();
            if (action == GLFW.GLFW_PRESS && this.client != null && this.client.currentScreen == null) {
                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    CpsTracker.recordLeftClick();
                } else if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                    CpsTracker.recordRightClick();
                }
            }
        } catch (Throwable ignored) {
        }
    }

    @Surrogate
    private void azyron$trackCpsClicks(CallbackInfo ci) {
        // Safe zero-argument surrogate if a companion mod alters method parameters
    }

    @Inject(
            method = "onMouseScroll(JDD)V",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void azyron$handleScrollZoom(long window, double horizontal, double vertical, CallbackInfo ci) {
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
        } catch (Throwable ignored) {
        }
    }

    @Surrogate
    private void azyron$handleScrollZoom(CallbackInfo ci) {
    }
}
