package com.jigneshgaming.azyron.mixin;

import com.jigneshgaming.azyron.AzyronClient;
import com.jigneshgaming.azyron.profile.MemoryOptimizer;
import com.jigneshgaming.azyron.ui.AzyronMainMenuScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {
    @org.spongepowered.asm.mixin.Unique
    private static boolean azyron$redirectingTitle = false;

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true, require = 0)
    private void azyron$interceptTitleScreen(Screen screen, CallbackInfo ci) {
        if (azyron$redirectingTitle) {
            return;
        }
        try {
            if (screen instanceof TitleScreen && !(screen instanceof AzyronMainMenuScreen)) {
                MinecraftClient mc = (MinecraftClient) (Object) this;
                if (mc.world != null) {
                    MemoryOptimizer.onWorldTransition();
                }
                if (AzyronMainMenuScreen.bypassRedirectOnce) {
                    AzyronMainMenuScreen.bypassRedirectOnce = false;
                    return;
                }
                AzyronClient inst = AzyronClient.getInstance();
                if (inst != null && inst.getConfig() != null && inst.getConfig().isCustomMainMenuEnabled()) {
                    azyron$redirectingTitle = true;
                    try {
                        mc.setScreen(new AzyronMainMenuScreen());
                        ci.cancel();
                    } finally {
                        azyron$redirectingTitle = false;
                    }
                }
            }
        } catch (Throwable t) {
            azyron$redirectingTitle = false;
            AzyronClient.LOGGER.warn("[AZYRON] Fallback to vanilla TitleScreen due to error: {}", t.getMessage());
        }
    }

    @Inject(method = "getWindowTitle", at = @At("RETURN"), cancellable = true, require = 0)
    private void azyron$brandWindowTitle(CallbackInfoReturnable<String> cir) {
        try {
            String original = cir.getReturnValue();
            if (original != null && !original.startsWith("AZYRON CLIENT")) {
                cir.setReturnValue("AZYRON CLIENT 1.21.11  |  " + original);
            }
        } catch (Throwable ignored) {
        }
    }
}
