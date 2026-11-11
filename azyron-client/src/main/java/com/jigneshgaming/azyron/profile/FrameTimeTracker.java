package com.jigneshgaming.azyron.profile;

/**
 * Zero-allocation, O(1) incremental ring buffer tracking frame-time deltas,
 * 1% low FPS (98th-percentile worst frame time without per-frame array sorting),
 * and live micro-stutter spikes.
 */
public final class FrameTimeTracker {
    private static final int SAMPLE_CAPACITY = 240;
    private static final float[] FRAME_MS = new float[SAMPLE_CAPACITY];

    private static int writeIndex = 0;
    private static int count = 0;
    private static float rollingSumMs = 0.0f;
    private static long lastFrameNs = 0L;
    private static float latestFrameMs = 4.0f;
    private static volatile boolean frameSpikeActive = false;

    private static long lastLowCalcMs = 0L;
    private static float cachedWorst98thMs = 4.5f;

    private FrameTimeTracker() {
    }

    /**
     * Alias invoked at GameRenderer frame start / HUD render to record frame pacing with O(1) cost.
     */
    public static void recordFrameStart() {
        onFrameRendered();
    }

    public static void onFrameRendered() {
        long now = System.nanoTime();
        if (lastFrameNs != 0L) {
            float deltaMs = (now - lastFrameNs) / 1_000_000.0f;
            if (deltaMs > 0.05f && deltaMs < 1000.0f) {
                latestFrameMs = deltaMs;
                if (count == SAMPLE_CAPACITY) {
                    rollingSumMs -= FRAME_MS[writeIndex];
                } else {
                    count++;
                }
                FRAME_MS[writeIndex] = deltaMs;
                rollingSumMs = Math.max(0.0f, rollingSumMs + deltaMs);
                writeIndex = (writeIndex + 1) % SAMPLE_CAPACITY;

                float avg = rollingSumMs / count;
                frameSpikeActive = deltaMs > 18.0f || (count >= 12 && deltaMs > (avg * 1.85f) && deltaMs > 11.0f);
            }
        }
        lastFrameNs = now;
    }

    public static float getLatestFrameMs() {
        return latestFrameMs;
    }

    public static float getAverageFrameMs() {
        if (count <= 0) {
            return 4.0f;
        }
        return rollingSumMs / count;
    }

    /**
     * Computes 1% low FPS using a zero-allocation linear top-K scan refreshed at <=8Hz
     * instead of copying and sorting 240 floats on every rendered frame.
     */
    public static int getOnePercentLowFps(int fallbackCurrentFps) {
        int safeFps = Math.max(1, fallbackCurrentFps);
        if (count < 10) {
            return Math.max(1, (int) Math.round(safeFps * 0.82));
        }

        long nowMs = System.currentTimeMillis();
        if (nowMs - lastLowCalcMs >= 125L || cachedWorst98thMs <= 0.1f) {
            lastLowCalcMs = nowMs;
            cachedWorst98thMs = find98thPercentileFrameMs(count);
        }

        if (cachedWorst98thMs <= 0.1f) {
            return safeFps;
        }
        return Math.max(1, Math.min(safeFps, Math.round(1000.0f / cachedWorst98thMs)));
    }

    /**
     * Finds the ~98th percentile slowest frame duration (5th largest out of up to 240 samples,
     * or 2nd largest for smaller windows) using 5 stack floats — zero heap or scratch array traffic.
     */
    private static float find98thPercentileFrameMs(int n) {
        int rank = Math.max(1, Math.min(5, n - (int) Math.floor(n * 0.98)));
        float m1 = 0f, m2 = 0f, m3 = 0f, m4 = 0f, m5 = 0f;
        for (int i = 0; i < n; i++) {
            float v = FRAME_MS[i];
            if (v > m1) { m5 = m4; m4 = m3; m3 = m2; m2 = m1; m1 = v; }
            else if (v > m2) { m5 = m4; m4 = m3; m3 = m2; m2 = v; }
            else if (v > m3) { m5 = m4; m4 = m3; m3 = v; }
            else if (v > m4) { m5 = m4; m4 = v; }
            else if (v > m5) { m5 = v; }
        }
        return switch (rank) {
            case 1 -> m1;
            case 2 -> m2 > 0f ? m2 : m1;
            case 3 -> m3 > 0f ? m3 : m1;
            case 4 -> m4 > 0f ? m4 : m1;
            default -> m5 > 0f ? m5 : m1;
        };
    }

    public static boolean isExperiencingFrameSpike() {
        return frameSpikeActive;
    }

    public static String getStabilityRating() {
        if (frameSpikeActive) {
            return "GUARDING";
        }
        return latestFrameMs <= 8.5f ? "ULTRA SMOOTH" : "STABLE";
    }

    public static void resetForTesting() {
        writeIndex = 0;
        count = 0;
        rollingSumMs = 0.0f;
        lastFrameNs = 0L;
        latestFrameMs = 4.0f;
        frameSpikeActive = false;
        lastLowCalcMs = 0L;
        cachedWorst98thMs = 4.5f;
    }
}
