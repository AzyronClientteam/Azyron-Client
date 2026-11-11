package com.jigneshgaming.azyron.profile;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import net.minecraft.client.MinecraftClient;

/**
 * Hardware-aware JVM memory & 3GB–8GB RAM tier optimizer for AZYRON CLIENT.
 * Detects available JVM heap and physical device RAM to calibrate chunk, particle,
 * and garbage-collection thresholds without causing cold-boot stalls or mid-combat GC stutters.
 */
public final class MemoryOptimizer {
    /**
     * Hardware memory tiers calibrated specifically for 3GB to 8GB+ RAM devices
     * across desktop launchers and PojavLauncher.
     */
    public enum RamTier {
        TIER_3GB("3GB RAM (Ultra-Light)", 6, 5, 0, 0, 0.50, 1, 40, 75, 85),
        TIER_4GB("4GB RAM (Low-Memory)", 7, 6, 1, 1, 0.65, 2, 75, 78, 87),
        TIER_6GB("5–6GB RAM (Balanced)", 9, 7, 2, 2, 0.80, 3, 130, 82, 89),
        TIER_8GB("7–8GB+ RAM (High-Headroom)", 10, 8, 2, 2, 0.90, 4, 180, 85, 91);

        private final String label;
        private final int maxSafeViewDistance;
        private final int maxSafeSimDistance;
        private final int maxSafeBiomeBlend;
        private final int maxSafeMipmap;
        private final double maxSafeEntityScale;
        private final int recommendedChunkThreads;
        private final int dynamicParticleCap;
        private final int highPressurePercent;
        private final int criticalPressurePercent;

        RamTier(
                String label,
                int maxSafeViewDistance,
                int maxSafeSimDistance,
                int maxSafeBiomeBlend,
                int maxSafeMipmap,
                double maxSafeEntityScale,
                int recommendedChunkThreads,
                int dynamicParticleCap,
                int highPressurePercent,
                int criticalPressurePercent
        ) {
            this.label = label;
            this.maxSafeViewDistance = maxSafeViewDistance;
            this.maxSafeSimDistance = maxSafeSimDistance;
            this.maxSafeBiomeBlend = maxSafeBiomeBlend;
            this.maxSafeMipmap = maxSafeMipmap;
            this.maxSafeEntityScale = maxSafeEntityScale;
            this.recommendedChunkThreads = recommendedChunkThreads;
            this.dynamicParticleCap = dynamicParticleCap;
            this.highPressurePercent = highPressurePercent;
            this.criticalPressurePercent = criticalPressurePercent;
        }

        public String getLabel() { return label; }
        public int getMaxSafeViewDistance() { return maxSafeViewDistance; }
        public int getMaxSafeSimDistance() { return maxSafeSimDistance; }
        public int getMaxSafeBiomeBlend() { return maxSafeBiomeBlend; }
        public int getMaxSafeMipmap() { return maxSafeMipmap; }
        public double getMaxSafeEntityScale() { return maxSafeEntityScale; }
        public int getRecommendedChunkThreads() { return recommendedChunkThreads; }
        public int getDynamicParticleCap() { return dynamicParticleCap; }
        public int getHighPressurePercent() { return highPressurePercent; }
        public int getCriticalPressurePercent() { return criticalPressurePercent; }
    }

    private static final long BOOT_TIME_MS = System.currentTimeMillis();
    private static final long STARTUP_GRACE_MS = 20_000L;
    private static final long MIN_SAMPLE_INTERVAL_MS = 250L;
    private static final int PHYSICAL_SYSTEM_RAM_MB = detectPhysicalSystemRamMb();

    private static int testOverrideMaxHeapMb = -1;
    private static int testOverridePhysicalRamMb = -1;

    private static long lastQuickReadMs = 0L;
    private static long lastSampleMs = 0L;
    private static long lastUsedBytes = 0L;
    private static int usedMb = 0;
    private static int maxMb = Math.max(256, (int) (Runtime.getRuntime().maxMemory() / (1024L * 1024L)));
    private static int usagePercent = 0;
    private static int allocationRateMbPerSec = 0;
    private static long lastCleanupMs = 0L;
    private static boolean hadWorldLastTick = false;
    private static Object lastWorldDimensionKey = null;

    private MemoryOptimizer() {
    }

    /**
     * Reads physical device RAM in MB on Linux / Android (PojavLauncher) via /proc/meminfo,
     * returning -1 when unavailable so JVM heap sizing takes precedence cleanly.
     */
    private static int detectPhysicalSystemRamMb() {
        try {
            Path memInfo = Paths.get("/proc/meminfo");
            if (Files.isReadable(memInfo)) {
                try (BufferedReader br = Files.newBufferedReader(memInfo, StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        if (line.startsWith("MemTotal:")) {
                            String digits = line.replaceAll("[^0-9]", "");
                            if (!digits.isEmpty()) {
                                long kb = Long.parseLong(digits);
                                return (int) Math.max(512L, kb / 1024L);
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return -1;
    }

    public static RamTier detectRamTier() {
        int heapMb = testOverrideMaxHeapMb > 0
                ? testOverrideMaxHeapMb
                : Math.max(256, (int) (Runtime.getRuntime().maxMemory() / (1024L * 1024L)));
        int physMb = testOverridePhysicalRamMb > 0
                ? testOverridePhysicalRamMb
                : PHYSICAL_SYSTEM_RAM_MB;

        // Classify based on the tighter of allocated JVM -Xmx heap and total device RAM (3GB..8GB+)
        if (heapMb <= 1664 || (physMb > 0 && physMb <= 3456)) {
            return RamTier.TIER_3GB;
        }
        if (heapMb <= 2432 || (physMb > 0 && physMb <= 4608)) {
            return RamTier.TIER_4GB;
        }
        if (heapMb <= 3584 || (physMb > 0 && physMb <= 6656)) {
            return RamTier.TIER_6GB;
        }
        return RamTier.TIER_8GB;
    }

    /**
     * Selects the optimal out-of-the-box PerformanceProfile for the detected 3GB–8GB RAM tier.
     */
    public static PerformanceProfile getRecommendedProfileForHardware() {
        return switch (detectRamTier()) {
            case TIER_3GB -> PerformanceProfile.POTATO;
            case TIER_4GB -> PerformanceProfile.SMOOTH_FPS;
            case TIER_6GB, TIER_8GB -> PerformanceProfile.BALANCED;
        };
    }

    public static boolean isHighMemoryPressure() {
        return usagePercent >= detectRamTier().getHighPressurePercent();
    }

    public static boolean isCriticalMemoryPressure() {
        return usagePercent >= detectRamTier().getCriticalPressurePercent();
    }

    /**
     * Computes optimal chunk mesh builder thread count by balancing RAM tier off-heap buffer budgets
     * with available CPU cores so singleplayer IntegratedServer chunk generation threads are never starved.
     */
    public static int computeOptimalChunkBuilderThreads() {
        RamTier tier = detectRamTier();
        int cores = Math.max(2, Runtime.getRuntime().availableProcessors());
        int halfCores = Math.max(1, cores / 2);
        return Math.max(1, Math.min(tier.getRecommendedChunkThreads(), halfCores));
    }

    /**
     * Recommends safe JVM -Xmx heap allocation (in MB) for 3GB–8GB+ devices, preserving enough
     * physical RAM headroom for Android/OS kernel, OpenGL/ANGLE drivers, and off-heap DirectByteBuffers.
     */
    public static int getRecommendedJvmHeapMb(int physicalDeviceRamMb) {
        if (physicalDeviceRamMb <= 3456) {
            return 1408; // 3GB device: leave ~1.6GB for OS + OpenGL driver + DirectByteBuffers
        }
        if (physicalDeviceRamMb <= 4608) {
            return 2048; // 4GB device: 2.0GB heap
        }
        if (physicalDeviceRamMb <= 6656) {
            return 3072; // 6GB device: 3.0GB heap
        }
        return 4096;     // 8GB+ device: 4.0GB heap avoids G1GC humongous region stalls
    }

    public static void setSimulatedRamForTesting(int simulatedHeapMb, int simulatedPhysicalMb) {
        testOverrideMaxHeapMb = simulatedHeapMb;
        testOverridePhysicalRamMb = simulatedPhysicalMb;
        if (simulatedHeapMb > 0) {
            maxMb = simulatedHeapMb;
        } else {
            maxMb = Math.max(256, (int) (Runtime.getRuntime().maxMemory() / (1024L * 1024L)));
        }
    }

    public static void onClientTick(MinecraftClient client, boolean aggressiveOptimization) {
        long now = System.currentTimeMillis();

        // Throttle Runtime memory queries to <=4Hz even if called from high-FPS UI screens
        if (lastQuickReadMs == 0L || (now - lastQuickReadMs) >= MIN_SAMPLE_INTERVAL_MS) {
            lastQuickReadMs = now;
            Runtime rt = Runtime.getRuntime();
            long rawMax = rt.maxMemory();
            long effectiveMax = testOverrideMaxHeapMb > 0 ? (testOverrideMaxHeapMb * 1024L * 1024L) : rawMax;
            long used = Math.max(0L, rt.totalMemory() - rt.freeMemory());

            usedMb = (int) (used / (1024L * 1024L));
            maxMb = Math.max(1, (int) (effectiveMax / (1024L * 1024L)));
            usagePercent = Math.min(100, (int) ((used * 100L) / Math.max(1L, effectiveMax)));

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
        }

        boolean hasWorld = client != null && client.world != null;
        Object currentDimKey = hasWorld ? client.world.getRegistryKey() : null;
        boolean dimensionChanged = hasWorld && hadWorldLastTick
                && lastWorldDimensionKey != null && currentDimKey != null
                && !lastWorldDimensionKey.equals(currentDimKey);

        if ((hadWorldLastTick != hasWorld) || dimensionChanged) {
            // Reset allocation-rate baseline across server/world transitions WITHOUT calling System.gc(),
            // avoiding multi-second Stop-The-World Full GC freezes during server join & chunk bursts.
            onWorldTransition();
        }
        hadWorldLastTick = hasWorld;
        lastWorldDimensionKey = currentDimKey;
    }

    public static void requestManualTrim() {
        requestSafeMemoryTrim(false);
    }

    /**
     * Explicit user-initiated memory trim from the AZYRON Client Options tab only.
     * Never triggered automatically during gameplay, server joining, or world transitions.
     */
    public static void requestSafeMemoryTrim(boolean force) {
        if (!force && MultiplayerPerformanceController.isInJoinWarmup()) {
            return;
        }
        long now = System.currentTimeMillis();
        if (force || (now - lastCleanupMs > 10_000L)) {
            lastCleanupMs = now;
            System.gc();
            lastQuickReadMs = 0L;
            onClientTick(null, false);
        }
    }

    /**
     * Resets telemetry sampling state on world/server switches without invoking System.gc().
     */
    public static void onWorldTransition() {
        lastSampleMs = 0L;
        lastQuickReadMs = 0L;
        allocationRateMbPerSec = 0;
    }

    public static int getUsedMb() { return usedMb; }
    public static int getMaxMb() { return maxMb; }
    public static int getUsagePercent() { return usagePercent; }
    public static int getAllocationRateMbPerSec() { return allocationRateMbPerSec; }
    public static int getPhysicalSystemRamMb() {
        return testOverridePhysicalRamMb > 0 ? testOverridePhysicalRamMb : PHYSICAL_SYSTEM_RAM_MB;
    }
}
