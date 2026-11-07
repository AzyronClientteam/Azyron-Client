package com.jigneshgaming.azyron.profile;

import java.util.Arrays;

/**
 * Zero-allocation ring buffer tracking frame-time deltas, 1% low FPS, and micro-stutter spikes.
 */
public final class FrameTimeTracker {
    private static final int SAMPLE_CAPACITY = 240;
    private static final float[] FRAME_MS = new float[SAMPLE_CAPACITY];
    private static final float[] SORT_SCRATCH = new float[SAMPLE_CAPACITY];

    private static int writeIndex = 0;
    private static int count = 0;
    private static long lastFrameNs = 0L;
    private static float latestFrameMs = 4.0f;

    private FrameTimeTracker() {
    }

    public static void onFrameRendered() {
        long now = System.nanoTime();
        if (lastFrameNs != 0L) {
            float deltaMs = (now - lastFrameNs) / 1_000_000.0f;
            if (deltaMs > 0.05f && deltaMs < 1000.0f) {
                latestFrameMs = deltaMs;
                FRAME_MS[writeIndex] = deltaMs;
                writeIndex = (writeIndex + 1) % SAMPLE_CAPACITY;
                if (count < SAMPLE_CAPACITY) {
                    count++;
                }
            }
        }
        lastFrameNs = now;
    }

    public static float getLatestFrameMs() {
        return latestFrameMs;
    }

    public static float getAverageFrameMs() {
        if (count == 0) {
            return 4.0f;
        }
        float sum = 0.0f;
        for (int i = 0; i < count; i++) {
            sum += FRAME_MS[i];
        }
        return sum / count;
    }

    /**
     * Computes 1% low FPS (derived from the 99th percentile slowest frame times).
     */
    public static int getOnePercentLowFps(int fallbackCurrentFps) {
        if (count < 10) {
            return Math.max(1, (int) Math.round(fallbackCurrentFps * 0.82));
        }
        int n = count;
        System.arraycopy(FRAME_MS, 0, SORT_SCRATCH, 0, n);
        Arrays.sort(SORT_SCRATCH, 0, n);
        int idx = Math.min(n - 1, Math.max(0, (int) Math.floor(n * 0.98)));
        float worstMs = SORT_SCRATCH[idx];
        if (worstMs <= 0.1f) {
            return fallbackCurrentFps;
        }
        return Math.max(1, Math.min(fallbackCurrentFps, Math.round(1000.0f / worstMs)));
    }

    public static boolean isExperiencingFrameSpike() {
        return latestFrameMs > 16.5f || latestFrameMs > (getAverageFrameMs() * 1.75f);
    }

    public static String getStabilityRating() {
        if (isExperiencingFrameSpike()) {
            return "GUARDING";
        }
        return latestFrameMs <= 8.5f ? "ULTRA SMOOTH" : "STABLE";
    }
}
