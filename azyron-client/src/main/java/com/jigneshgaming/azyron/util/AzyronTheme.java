package com.jigneshgaming.azyron.util;

/**
 * Color palette and interpolation utilities for AZYRON CLIENT.
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
     * Animated dual-tone accent cycling smoothly between Electric Purple and Electric Blue.
     */
    public static int getAnimatedAccent(float phaseOffset) {
        double now = (System.currentTimeMillis() % 4000L) / 4000.0;
        float wave = (float) (0.5 + 0.5 * Math.sin((now + phaseOffset) * Math.PI * 2.0));
        return lerpColor(ACCENT_PURPLE_BRIGHT, ACCENT_ELECTRIC_BLUE, wave);
    }

    public static int resolvePresetColor(String presetName) {
        if (presetName == null) {
            return ACCENT_ELECTRIC_BLUE;
        }
        return switch (presetName.toUpperCase()) {
            case "PURPLE" -> ACCENT_PURPLE_BRIGHT;
            case "ELECTRIC BLUE", "CYAN" -> ACCENT_ELECTRIC_BLUE;
            case "CHROMA", "DYNAMIC" -> getAnimatedAccent(0.0f);
            case "EMERALD", "GREEN" -> STATUS_GOOD;
            case "CRIMSON", "RED" -> STATUS_DANGER;
            case "WHITE" -> TEXT_PRIMARY;
            default -> ACCENT_ELECTRIC_BLUE;
        };
    }

    public static float clamp01(float v) {
        return Math.max(0.0f, Math.min(1.0f, v));
    }
}
