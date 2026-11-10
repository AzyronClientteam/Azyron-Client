package com.jigneshgaming.azyron.util;

import com.jigneshgaming.azyron.AzyronClient;
import java.util.Locale;
import net.minecraft.nbt.NbtCompound;

/** Typed, in-memory options.txt repair before vanilla SimpleOption codec parsing.
 * Missing/unknown entries are left to vanilla defaults; the original file is not rewritten here.
 * Text background opacity is a DOUBLE, not the backgroundForChatOnly BOOLEAN.
 */
public final class OptionsConfigSanitizer {
    private static final String[][] BOOLEAN_OPTION_DEFAULTS = {
            {"fullscreen", "false"}, {"enableVsync", "true"},
            {"entityShadows", "true"}, {"ao", "true"}, {"vignette", "true"},
            {"autoJump", "false"}, {"toggleCrouch", "false"}, {"toggleSprint", "false"},
            {"touchscreen", "false"}, {"rawMouseInput", "true"}, {"narratorHotkey", "true"},
            {"directionalAudio", "false"}, {"hideLightningFlashes", "false"},
            {"darkMojangStudiosBackground", "false"}, {"realmsNotifications", "true"},
            {"allowServerListing", "true"}, {"onlyShowSecureChat", "false"},
            {"reducedDebugInfo", "false"}, {"backgroundForChatOnly", "true"}
    };
    // Only common unit-interval slider keys with known schemas on both supported versions.
    private static final String[][] UNIT_DOUBLE_DEFAULTS = {
            {"textBackgroundOpacity", "0.5"}, {"chatOpacity", "1.0"},
            {"chatScale", "1.0"}, {"chatLineSpacing", "0.0"},
            {"mouseSensitivity", "0.5"}, {"gamma", "0.5"}
    };

    private OptionsConfigSanitizer() { }

    public static String normalizeBooleanOptionValue(String raw, boolean fallback) {
        return normalizeBooleanOptionValue(null, raw, fallback);
    }

    public static String normalizeBooleanOptionValue(String optionKey, String raw, boolean fallback) {
        if (raw == null) return Boolean.toString(fallback);
        String cleaned = stripOptionalQuotes(raw.trim()).trim().toLowerCase(Locale.ROOT);
        return switch (cleaned) {
            case "true", "on", "1", "yes", "enabled", "enable" -> "true";
            case "false", "off", "0", "no", "disabled", "disable" -> "false";
            // Blank is missing data, NOT an explicit false. Preserve the option's default.
            default -> Boolean.toString(fallback);
        };
    }

    public static boolean parseNormalizedBoolean(String raw, boolean fallback) {
        return "true".equals(normalizeBooleanOptionValue(raw, fallback));
    }

    /** Reject JSON booleans/containers, NaN/Infinity and out-of-range slider values. */
    public static String normalizeUnitDoubleOptionValue(String raw, double fallback) {
        if (!Double.isFinite(fallback) || fallback < 0.0 || fallback > 1.0) {
            throw new IllegalArgumentException("Fallback must be a finite unit-interval number");
        }
        if (raw == null) return Double.toString(fallback);
        String cleaned = stripOptionalQuotes(raw.trim()).trim();
        // parseDouble accepts hexadecimal and suffixes that JSON codecs reject: prohibit them.
        if (cleaned.isEmpty() || cleaned.length() > 64
                || !cleaned.matches("-?(?:0|[1-9][0-9]*)(?:\\.[0-9]+)?(?:[eE][+-]?[0-9]+)?")) {
            return Double.toString(fallback);
        }
        try {
            double value = Double.parseDouble(cleaned);
            return Double.toString(Double.isFinite(value) && value >= 0.0 && value <= 1.0 ? value : fallback);
        } catch (NumberFormatException invalid) {
            return Double.toString(fallback);
        }
    }

    public static NbtCompound sanitizeOptionsCompound(NbtCompound nbt) {
        if (nbt == null) return null;
        for (String[] entry : BOOLEAN_OPTION_DEFAULTS) {
            sanitizeEntry(nbt, entry[0], entry[1], true);
        }
        for (String[] entry : UNIT_DOUBLE_DEFAULTS) {
            sanitizeEntry(nbt, entry[0], entry[1], false);
        }
        return nbt;
    }

    private static void sanitizeEntry(NbtCompound nbt, String key, String fallback, boolean bool) {
        // Never manufacture absent options: vanilla retains its initialized value.
        if (!nbt.contains(key)) return;
        try {
            String raw = McVersionBridge.nbtGetString(nbt, key).orElse(null);
            String normalized = bool ? normalizeBooleanOptionValue(raw, Boolean.parseBoolean(fallback))
                    : normalizeUnitDoubleOptionValue(raw, Double.parseDouble(fallback));
            if (!normalized.equals(raw)) nbt.putString(key, normalized);
        } catch (RuntimeException invalid) {
            // A malformed entry must not prevent independent later entries from being repaired.
            nbt.putString(key, fallback);
            AzyronClient.LOGGER.warn("[AZYRON] Restored invalid option '{}' to its typed default", key);
        }
    }

    private static String stripOptionalQuotes(String s) {
        if (s.length() >= 2) {
            char first = s.charAt(0), last = s.charAt(s.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return s.substring(1, s.length() - 1);
            }
        }
        return s;
    }
}
