package com.jigneshgaming.azyron.mixin;

import com.jigneshgaming.azyron.profile.MultiplayerPerformanceController;
import java.util.Deque;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies a per-frame nanosecond time budget to ClientWorld.runQueuedChunkUpdates()
 * so heavy server-join chunk light bursts spread smoothly across frames instead of freezing.
 */
@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
    @Shadow
    @Final
    private Deque<Runnable> chunkUpdaters;

    @Inject(method = "runQueuedChunkUpdates", at = @At("HEAD"), cancellable = true, require = 0)
    private void azyron$budgetQueuedChunkLightUpdates(CallbackInfo ci) {
        MultiplayerPerformanceController.runBudgetedChunkLightUpdates(this.chunkUpdaters, MinecraftClient.getInstance());
        ci.cancel();
    }
}
