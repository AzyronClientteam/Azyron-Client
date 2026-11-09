package com.jigneshgaming.azyron.mixin;

import com.jigneshgaming.azyron.profile.MultiplayerPerformanceController;
import net.minecraft.client.network.ChunkBatchSizeCalculator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Paces ServerboundChunkBatchReceivedPacket desiredChunksPerTick using protocol-standard
 * flow control so multiplayer servers do not flood 25–64 chunks/tick onto 3GB–8GB RAM devices.
 */
@Mixin(ChunkBatchSizeCalculator.class)
public abstract class ChunkBatchSizeCalculatorMixin {
    @Inject(method = "getDesiredChunksPerTick", at = @At("RETURN"), cancellable = true, require = 0)
    private void azyron$paceMultiplayerChunkBatches(CallbackInfoReturnable<Float> cir) {
        float vanilla = cir.getReturnValueF();
        float paced = MultiplayerPerformanceController.paceDesiredChunksPerTick(vanilla);
        if (paced != vanilla) {
            cir.setReturnValue(paced);
        }
    }
}
