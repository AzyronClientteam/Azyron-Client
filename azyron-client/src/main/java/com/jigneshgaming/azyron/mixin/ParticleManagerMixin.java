package com.jigneshgaming.azyron.mixin;

import com.jigneshgaming.azyron.AzyronClient;
import com.jigneshgaming.azyron.profile.PerformanceEngine;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * High-speed particle budget & distance culler for Potato Mode and Smooth FPS Mode.
 */
@Mixin(ParticleManager.class)
public abstract class ParticleManagerMixin {
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;", at = @At("HEAD"), cancellable = true, require = 0)
    private void azyron$throttleParticles(
            ParticleEffect parameters,
            double x, double y, double z,
            double velocityX, double velocityY, double velocityZ,
            CallbackInfoReturnable<Particle> cir
    ) {
        AzyronClient inst = AzyronClient.getInstance();
        if (inst != null) {
            PerformanceEngine engine = inst.getPerformanceEngine();
            if (engine != null && !engine.shouldAllowParticle(x, y, z)) {
                cir.setReturnValue(null);
            }
        }
    }
}
