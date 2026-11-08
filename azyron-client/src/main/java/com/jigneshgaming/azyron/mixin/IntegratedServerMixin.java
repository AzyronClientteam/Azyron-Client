package com.jigneshgaming.azyron.mixin;

import com.jigneshgaming.azyron.profile.IntegratedServerOptimizer;
import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.integrated.IntegratedServer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Optimizes singleplayer IntegratedServer chunk generation I/O and adaptive simulation distance
 * pacing under heavy terrain generation or constrained 3GB–8GB RAM headroom.
 */
@Mixin(IntegratedServer.class)
public abstract class IntegratedServerMixin {
    @Shadow
    @Final
    private MinecraftClient client;

    @Unique
    private long azyron$tickStartNs = 0L;

    @Inject(method = "syncChunkWrites", at = @At("HEAD"), cancellable = true, require = 0)
    private void azyron$useAsyncChunkWrites(CallbackInfoReturnable<Boolean> cir) {
        try {
            boolean vanilla = this.client != null && this.client.options != null && this.client.options.syncChunkWrites;
            if (!IntegratedServerOptimizer.shouldForceSyncChunkWrites(vanilla)) {
                cir.setReturnValue(false);
            }
        } catch (Throwable ignored) {
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), require = 0)
    private void azyron$onTickStart(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        this.azyron$tickStartNs = System.nanoTime();
    }

    @Inject(method = "tick", at = @At("TAIL"), require = 0)
    private void azyron$onTickEnd(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        try {
            if (this.azyron$tickStartNs > 0L) {
                IntegratedServerOptimizer.recordServerTickNs(System.nanoTime() - this.azyron$tickStartNs);
            }
            IntegratedServerOptimizer.onIntegratedServerTick((IntegratedServer) (Object) this, this.client);
        } catch (Throwable ignored) {
        }
    }
}
