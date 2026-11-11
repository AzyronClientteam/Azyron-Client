package com.jigneshgaming.azyron.mixin;

import com.jigneshgaming.azyron.profile.MultiplayerPerformanceController;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Minecraft 1.20.1 multiplayer chunk-receive telemetry hook.
 * Tracks ClientPlayNetworkHandler.loadChunk (method_38539) bursts during server joins and BungeeCord
 * world switches so ClientWorld light-update budgeting and MemoryOptimizer stay coordinated on 1.20.1.
 */
@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandler120Mixin {
    @Inject(
            method = "loadChunk",
            at = @At("HEAD"),
            require = 1
    )
    private void azyron$onLoadChunk120(CallbackInfo ci) {
        MultiplayerPerformanceController.recordChunkLoaded120();
    }
}
