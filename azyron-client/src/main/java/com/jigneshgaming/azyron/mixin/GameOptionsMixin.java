package com.jigneshgaming.azyron.mixin;

import com.jigneshgaming.azyron.profile.IntegratedServerOptimizer;
import com.jigneshgaming.azyron.util.OptionsConfigSanitizer;
import net.minecraft.client.option.GameOptions;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sanitizes options.txt boolean values (accepting "fullscreen:off" -> "false" and normalizing
 * invalid values safely) and configures non-blocking chunk writes after options load.
 */
@Mixin(GameOptions.class)
public abstract class GameOptionsMixin {
    @Shadow
    public boolean syncChunkWrites;

    @Inject(method = "update", at = @At("RETURN"), require = 0)
    private void azyron$sanitizeLoadedOptionsCompound(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
        try {
            NbtCompound updated = cir.getReturnValue();
            if (updated != null) {
                OptionsConfigSanitizer.sanitizeOptionsCompound(updated);
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "load", at = @At("TAIL"), require = 0)
    private void azyron$optimizePostLoadOptions(CallbackInfo ci) {
        try {
            if (!IntegratedServerOptimizer.shouldForceSyncChunkWrites(this.syncChunkWrites)) {
                this.syncChunkWrites = false;
            }
        } catch (Throwable ignored) {
        }
    }
}
