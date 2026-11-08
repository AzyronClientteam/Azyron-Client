package com.jigneshgaming.azyron.mixin;

import com.jigneshgaming.azyron.util.PlatformCompat;
import com.mojang.text2speech.Narrator;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.NarratorMode;
import net.minecraft.client.util.NarratorManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Provides a graceful fallback when native Flite TTS (libflite.so / libflite.so.1) is absent
 * on Android / PojavLauncher or minimal Linux systems, preventing JNA load errors, modal
 * TinyFileDialogs crashes, and InactiveNarratorLibraryException startup aborts.
 */
@Mixin(NarratorManager.class)
public abstract class NarratorManagerMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Shadow
    public abstract boolean isActive();

    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/text2speech/Narrator;getNarrator()Lcom/mojang/text2speech/Narrator;"
            ),
            require = 0
    )
    private Narrator azyron$createSafeNarrator() {
        return PlatformCompat.createSafeNarrator();
    }

    @Inject(method = "checkNarratorLibrary", at = @At("HEAD"), cancellable = true, require = 0)
    private void azyron$guardMissingNarratorLibrary(boolean narratorEnabled, CallbackInfo ci) {
        try {
            if (!this.isActive()) {
                if (narratorEnabled) {
                    PlatformCompat.logNarratorFallbackOnce(
                            "Narrator was enabled in options, but native TTS library is unavailable; resetting Narrator to OFF without crashing."
                    );
                    if (this.client != null && this.client.options != null && this.client.options.getNarrator() != null) {
                        this.client.options.getNarrator().setValue(NarratorMode.OFF);
                    }
                }
                // Cancel before TinyFileDialogs.tinyfd_messageBox and InactiveNarratorLibraryException
                ci.cancel();
            }
        } catch (Throwable ignored) {
            ci.cancel();
        }
    }
}
