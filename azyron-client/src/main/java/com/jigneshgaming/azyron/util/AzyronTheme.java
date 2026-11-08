package com.jigneshgaming.azyron.util;

/**
 * Color palette and zero-allocation LUT interpolation utilities for AZYRON CLIENT.
 * Uses explicit ARGB colors with full alpha channels required by Minecraft 1.21.11 DrawContext.
 */
public final class AzyronTheme {
    // Deep Obsidian Dark Surfaces
    public static final int BG_DEEPEST = 0xFF070810;
    public static final int BG_DARK = 0xFF0B0D1A;
    public static final int BG_SIDEBAR = 0xF00B0E1D;
    public static final int BG_PANEL = 0xEB101324;
    public static final int BG_CARD = 0xDF151930;
    public static final int BG_CARD_HOVER = 0xEB1D2342;
    public static final int BG_CARD_ACTIVE = 0xF221284E;
    public static final int BG_HUD_DEFAULT = 0xA80B0D1A;
    public static final int BG_INPUT = 0xE60D1020;

    // Signature Accents: Electric Purple & Electric Blue
    public static final int ACCENT_PURPLE = 0xFF9333EA;
    public static final int ACCENT_PURPLE_BRIGHT = 0xFFA855F7;
    public static final int ACCENT_ELECTRIC_BLUE = 0xFF00D4FF;
    public static final int ACCENT_ROYAL_BLUE = 0xFF3B82F6;
    public static final int ACCENT_MAGENTA = 0xFFD946EF;

    // Semantic Status Colors
    public static final int STATUS_GOOD = 0xFF22C55E;
    public static final int STATUS_WARN = 0xFFF59E0B;
    public static final int STATUS_DANGER = 0xFFEF4444;
    public static final int DISCORD_BLURPLE = 0xFF5865F2;

    // Crisp Typography
    public static final int TEXT_PRIMARY = 0xFFF8FAFC;
    public static final int TEXT_SECONDARY = 0xFFCBD5E1;
    public static final int TEXT_MUTED = 0xFF94A3B8;
    public static final int BORDER_SUBTLE = 0x55475569;
    public static final int BORDER_GLOW = 0xAA8B5CF6;

    // Precomputed 256-step sine-wave lookup table eliminating per-frame Math.sin() across HUD widgets
    private static final int LUT_SIZE = 256;
    private static final int[] CHROMA_LUT = new int[LUT_SIZE];

    static {
        for (int i = 0; i < LUT_SIZE; i++) {
            double phase = (double) i / LUT_SIZE;
            float wave = (float) (0.5 + 0.5 * Math.sin(phase * Math.PI * 2.0));
            CHROMA_LUT[i] = lerpColor(ACCENT_PURPLE_BRIGHT, ACCENT_ELECTRIC_BLUE, wave);
        }
    }

    private AzyronTheme() {
    }

    public static int withAlpha(int rgb, int alpha) {
        int clamped = Math.max(0, Math.min(255, alpha));
        return (clamped << 24) | (rgb & 0x00FFFFFF);
    }

    public static int withAlphaFloat(int rgb, float alpha) {
        return withAlpha(rgb, Math.round(clamp01(alpha) * 255.0f));
    }

    public static int lerpColor(int c1, int c2, float t) {
        float f = clamp01(t);
        int a1 = (c1 >>> 24) & 0xFF;
        int r1 = (c1 >>> 16) & 0xFF;
        int g1 = (c1 >>> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        int a2 = (c2 >>> 24) & 0xFF;
        int r2 = (c2 >>> 16) & 0xFF;
        int g2 = (c2 >>> 8) & 0xFF;
        int b2 = c2 & 0xFF;

        int a = Math.round(a1 + (a2 - a1) * f);
        int r = Math.round(r1 + (r2 - r1) * f);
        int g = Math.round(g1 + (g2 - g1) * f);
        int b = Math.round(b1 + (b2 - b1) * f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Animated dual-tone accent cycling smoothly between Electric Purple and Electric Blue
     * via O(1) precomputed LUT lookup (zero per-frame Math.sin calls).
     */
    public static int getAnimatedAccent(float phaseOffset) {
        int baseIdx = (int) (((System.currentTimeMillis() & 0xFFF) * LUT_SIZE) >>> 12);
        int offsetIdx = Math.round(phaseOffset * LUT_SIZE);
        return CHROMA_LUT[(baseIdx + offsetIdx) & (LUT_SIZE - 1)];
    }

    /**
     * Zero-allocation preset color resolver (avoids String.toUpperCase() heap allocations on every HUD frame).
     */
    public static int resolvePresetColor(String presetName) {
        if (presetName == null || presetName.isEmpty()) {
            return ACCENT_ELECTRIC_BLUE;
        }
        if ("Chroma".equalsIgnoreCase(presetName) || "Dynamic".equalsIgnoreCase(presetName)) {
            return getAnimatedAccent(0.0f);
        }
        if ("Electric Blue".equalsIgnoreCase(presetName) || "Cyan".equalsIgnoreCase(presetName)) {
            return ACCENT_ELECTRIC_BLUE;
        }
        if ("Purple".equalsIgnoreCase(presetName)) {
            return ACCENT_PURPLE_BRIGHT;
        }
        if ("Emerald".equalsIgnoreCase(presetName) || "Green".equalsIgnoreCase(presetName)) {
            return STATUS_GOOD;
        }
        if ("Crimson".equalsIgnoreCase(presetName) || "Red".equalsIgnoreCase(presetName)) {
            return STATUS_DANGER;
        }
        if ("White".equalsIgnoreCase(presetName)) {
            return TEXT_PRIMARY;
        }
        return ACCENT_ELECTRIC_BLUE;
    }

    public static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }
}
