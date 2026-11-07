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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mouse.class)
public abstract class MouseMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Inject(method = "onMouseButton", at = @At("HEAD"))
    private void azyron$trackCpsClicks(long window, MouseInput input, int action, CallbackInfo ci) {
        if (action == GLFW.GLFW_PRESS && this.client != null && this.client.currentScreen == null && input != null) {
            int btn = input.button();
            if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                CpsTracker.recordLeftClick();
            } else if (btn == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
                CpsTracker.recordRightClick();
            }
        }
    }

    @Inject(method = "onMouseScroll", at = @At("HEAD"), cancellable = true)
    private void azyron$handleScrollZoom(long window, double horizontal, double vertical, CallbackInfo ci) {
        if (this.client != null && this.client.currentScreen == null) {
            AzyronClient inst = AzyronClient.getInstance();
            if (inst != null && inst.getModuleManager() != null) {
                ZoomModule zoom = inst.getModuleManager().getZoomModule();
                if (zoom != null && zoom.onScroll(vertical)) {
                    ci.cancel();
                }
            }
        }
    }
}
