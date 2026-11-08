package com.jigneshgaming.azyron.mixin;

import com.jigneshgaming.azyron.util.PlatformCompat;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.realms.RealmsAvailability;
import net.minecraft.client.session.Session;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gracefully handles Realms availability checks for offline / local sessions in Minecraft 1.21.11.
 * Does NOT modify or bypass Microsoft/Realms authentication: online Microsoft sessions proceed
 * through vanilla RealmsClient untouched, while unauthenticated local profiles immediately
 * resolve to AUTHENTICATION_ERROR without firing failing HTTP 401 calls or error stacktraces.
 */
@Mixin(RealmsAvailability.class)
public abstract class RealmsAvailabilityMixin {
    @Inject(method = "checkInternal", at = @At("HEAD"), cancellable = true, require = 0)
    private static void azyron$handleOfflineLocalSessionGracefully(
            CallbackInfoReturnable<CompletableFuture<RealmsAvailability.Info>> cir
    ) {
        try {
            MinecraftClient mc = MinecraftClient.getInstance();
            Session session = mc != null ? mc.getSession() : null;
            if (PlatformCompat.isOfflineOrLocalSession(session)) {
                PlatformCompat.logOfflineRealmsSkipOnce(session != null ? session.getUsername() : "OfflinePlayer");
                cir.setReturnValue(CompletableFuture.completedFuture(
                        new RealmsAvailability.Info(RealmsAvailability.Type.AUTHENTICATION_ERROR)
                ));
            }
        } catch (Throwable ignored) {
        }
    }
}
