package com.jigneshgaming.azyron.profile;

import net.minecraft.client.MinecraftClient;

/**
 * Monitors JVM heap pressure and allocation rate (MB/s), relieving memory pressure safely
 * during screen/world transitions without inducing mid-combat GC stutters.
 */
public final class MemoryOptimizer {
    private static long lastSampleMs = 0L;
    private static long lastUsedBytes = 0L;
    private static int usedMb = 0;
    private static int maxMb = 1024;
    private static int usagePercent = 0;
    private static int allocationRateMbPerSec = 0;
    private static long lastCleanupMs = 0L;
    private static boolean hadWorldLastTick = false;

    private MemoryOptimizer() {
    }

    public static void onClientTick(MinecraftClient client, boolean aggressiveOptimization) {
        long now = System.currentTimeMillis();
        Runtime rt = Runtime.getRuntime();
        long max = rt.maxMemory();
        long used = rt.totalMemory() - rt.freeMemory();

        usedMb = (int) (used / (1024L * 1024L));
        maxMb = Math.max(1, (int) (max / (1024L * 1024L)));
        usagePercent = Math.min(100, (int) ((used * 100L) / Math.max(1L, max)));

        if (lastSampleMs == 0L) {
            lastSampleMs = now;
            lastUsedBytes = used;
        } else if (now - lastSampleMs >= 1000L) {
            long deltaBytes = used - lastUsedBytes;
            if (deltaBytes > 0) {
                long elapsed = Math.max(1L, now - lastSampleMs);
                allocationRateMbPerSec = (int) ((deltaBytes * 1000L) / (elapsed * 1024L * 1024L));
            } else {
                allocationRateMbPerSec = Math.max(0, allocationRateMbPerSec / 2);
            }
            lastSampleMs = now;
            lastUsedBytes = used;
        }

        boolean hasWorld = client != null && client.world != null;
        // Clean up discarded world references right after disconnecting from a world
        if (hadWorldLastTick && !hasWorld && (now - lastCleanupMs) > 15_000L) {
            triggerSafeCleanup(now);
        } else if (aggressiveOptimization && usagePercent >= 88 && client != null && client.currentScreen != null && (now - lastCleanupMs) > 45_000L) {
            // Only advise GC when paused in a GUI screen at >88% heap — never mid-combat!
            triggerSafeCleanup(now);
        }
        hadWorldLastTick = hasWorld;
    }

    public static void requestManualTrim() {
        requestSafeMemoryTrim(false);
    }

    public static void requestSafeMemoryTrim(boolean force) {
        long now = System.currentTimeMillis();
        if (force || (now - lastCleanupMs > 5_000L)) {
            triggerSafeCleanup(now);
        }
    }

    public static void onWorldTransition() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupMs > 12_000L) {
            triggerSafeCleanup(now);
        }
    }

    private static void triggerSafeCleanup(long now) {
        lastCleanupMs = now;
        System.gc();
    }

    public static int getUsedMb() { return usedMb; }
    public static int getMaxMb() { return maxMb; }
    public static int getUsagePercent() { return usagePercent; }
    public static int getAllocationRateMbPerSec() { return allocationRateMbPerSec; }
}
