package com.jigneshgaming.azyron.profile;

import com.jigneshgaming.azyron.AzyronClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.integrated.IntegratedServer;

/**
 * Optimizes singleplayer IntegratedServer tick pacing, chunk generation CPU contention,
 * and asynchronous RegionFile chunk I/O on 3GB–8GB RAM desktops and Android/PojavLauncher devices.
 */
public final class IntegratedServerOptimizer {
    private static final int MSPT_RING_SIZE = 40; // 2-second rolling window at 20 TPS
    private static final double OVERLOAD_MSPT_THRESHOLD = 46.0;
    private static final double RECOVERY_MSPT_THRESHOLD = 32.0;
    private static final long ADJUST_COOLDOWN_MS = 5_000L;

    private static final long[] TICK_DURATIONS_NS = new long[MSPT_RING_SIZE];
    private static int ringIndex = 0;
    private static int sampleCount = 0;
    private static long rollingSumNs = 0L;

    private static int activeSimStepDown = 0;
    private static long lastAdjustTimeMs = 0L;

    private IntegratedServerOptimizer() {
    }

    /**
     * Records a completed IntegratedServer tick duration in O(1) with zero heap allocations.
     */
    public static void recordServerTickNs(long durationNs) {
        if (durationNs < 0L) {
            return;
        }
        if (sampleCount == MSPT_RING_SIZE) {
            rollingSumNs -= TICK_DURATIONS_NS[ringIndex];
        } else {
            sampleCount++;
        }
        TICK_DURATIONS_NS[ringIndex] = durationNs;
        rollingSumNs += durationNs;
        ringIndex = (ringIndex + 1) % MSPT_RING_SIZE;
    }

    public static double getAverageMspt() {
        if (sampleCount <= 0) {
            return 0.0;
        }
        return (rollingSumNs / (double) sampleCount) / 1_000_000.0;
    }

    public static int getActiveSimStepDown() {
        return activeSimStepDown;
    }

    public static void resetState() {
        for (int i = 0; i < MSPT_RING_SIZE; i++) {
            TICK_DURATIONS_NS[i] = 0L;
        }
        ringIndex = 0;
        sampleCount = 0;
        rollingSumNs = 0L;
        activeSimStepDown = 0;
        lastAdjustTimeMs = 0L;
    }

    /**
     * Prevents blocking synchronous RegionFile disk flushes on every generated/saved chunk
     * in singleplayer, allowing Minecraft's dedicated I/O worker pool to batch writes asynchronously.
     */
    public static boolean shouldForceSyncChunkWrites(boolean vanillaSyncWrites) {
        if (Boolean.getBoolean("azyron.forceSyncChunkWrites")) {
            return vanillaSyncWrites;
        }
        AzyronClient inst = AzyronClient.getInstance();
        if (inst != null && inst.getConfig() != null && !inst.getConfig().isAutoMemoryOptimization()) {
            return vanillaSyncWrites;
        }
        return false;
    }

    /**
     * Dynamically paces IntegratedServer simulation & view distances during heavy terrain generation
     * or high 3GB–8GB RAM pressure so chunk generation workers are not starved by entity/block ticks.
     */
    public static void onIntegratedServerTick(IntegratedServer server, MinecraftClient client) {
        if (server == null || server.isPaused()) {
            return;
        }
        PlayerManager playerManager = server.getPlayerManager();
        if (playerManager == null || client == null || client.options == null) {
            return;
        }

        AzyronClient inst = AzyronClient.getInstance();
        boolean autoOpt = inst == null || inst.getConfig() == null || inst.getConfig().isAutoMemoryOptimization();
        if (!autoOpt) {
            return;
        }

        MemoryOptimizer.RamTier tier = MemoryOptimizer.detectRamTier();

        // 1. Guard Integrated Server view distance against excessive quadratic chunk-gen queues on 3GB/4GB hardware
        int safeMaxView = tier.getMaxSafeViewDistance() + 2;
        if (tier == MemoryOptimizer.RamTier.TIER_3GB || tier == MemoryOptimizer.RamTier.TIER_4GB) {
            int currentServerView = playerManager.getViewDistance();
            if (currentServerView > safeMaxView) {
                playerManager.setViewDistance(safeMaxView);
            }
        }

        // 2. Adaptive Simulation Distance Pacing during MSPT spikes (>46ms) or critical/high RAM pressure
        long now = System.currentTimeMillis();
        if (now - lastAdjustTimeMs < ADJUST_COOLDOWN_MS || sampleCount < 10) {
            return;
        }

        int configuredSim = Math.max(2, client.options.getSimulationDistance().getValue());
        int targetStepDown = computeDesiredSimStepDown(
                getAverageMspt(),
                MemoryOptimizer.isHighMemoryPressure(),
                MemoryOptimizer.isCriticalMemoryPressure(),
                configuredSim,
                activeSimStepDown
        );

        if (targetStepDown != activeSimStepDown) {
            activeSimStepDown = targetStepDown;
            lastAdjustTimeMs = now;
            int effectiveSim = Math.max(2, configuredSim - activeSimStepDown);
            if (playerManager.getSimulationDistance() != effectiveSim) {
                playerManager.setSimulationDistance(effectiveSim);
                AzyronClient.LOGGER.info(
                        "[AZYRON] IntegratedServer pacing adjusted simulation distance to {} chunks (MSPT: {} ms, RAM: {}%)",
                        effectiveSim,
                        Math.round(getAverageMspt() * 10.0) / 10.0,
                        MemoryOptimizer.getUsagePercent()
                );
            }
        }
    }

    /**
     * Pure calculation helper (also directly unit-tested) for hysteresis-stabilized simulation distance pacing.
     */
    public static int computeDesiredSimStepDown(
            double avgMspt,
            boolean highRamPressure,
            boolean criticalRamPressure,
            int configuredSimDistance,
            int currentStepDown
    ) {
        int maxAllowedReduction = Math.max(0, configuredSimDistance - 2);
        if (maxAllowedReduction == 0) {
            return 0;
        }
        if (criticalRamPressure || avgMspt >= 58.0) {
            return Math.min(maxAllowedReduction, 2);
        }
        if (highRamPressure || avgMspt >= OVERLOAD_MSPT_THRESHOLD) {
            return Math.min(maxAllowedReduction, Math.max(1, currentStepDown));
        }
        if (avgMspt <= RECOVERY_MSPT_THRESHOLD && !highRamPressure && currentStepDown > 0) {
            return currentStepDown - 1;
        }
        return Math.min(maxAllowedReduction, currentStepDown);
    }
}
