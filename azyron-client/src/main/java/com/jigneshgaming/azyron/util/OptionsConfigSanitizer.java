package com.jigneshgaming.azyron.util;

import com.jigneshgaming.azyron.AzyronClient;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.nbt.NbtCompound;

/**
 * Normalizes Minecraft 1.21.11 options.txt entries before SimpleOption Codec parsing.
 * Specifically resolves PojavLauncher / external launcher "fullscreen:off" entries
 * (accepting "off" as "false" and "on" as "true") and safely normalizes malformed values
 * so GameOptions.load() never logs Codec.BOOL errors or aborts on malformed JSON tokens.
 */
public final class OptionsConfigSanitizer {
    /**
     * Boolean SimpleOption keys in Minecraft 1.21.11 GameOptions whose Codec.BOOL parser
     * rejects legacy/launcher strings such as "off" or "on".
     * Format: { optionKey, defaultFallback ("false" or "true") }
     */
    private static final String[][] BOOLEAN_OPTION_DEFAULTS = {
            {"fullscreen", "false"},
            {"enableVsync", "true"},
            {"entityShadows", "true"},
            {"ao", "true"},
            {"vignette", "true"},
            {"autoJump", "false"},
            {"toggleCrouch", "false"},
            {"toggleSprint", "false"},
            {"touchscreen", "false"},
            {"rawMouseInput", "true"},
            {"narratorHotkey", "true"},
            {"directionalAudio", "false"},
            {"hideLightningFlashes", "false"},
            {"darkMojangStudiosBackground", "false"},
            {"realmsNotifications", "true"},
            {"allowServerListing", "true"},
            {"onlyShowSecureChat", "false"},
            {"reducedDebugInfo", "false"}
    };

    private OptionsConfigSanitizer() {
    }

    /**
     * Normalizes a raw boolean configuration string into canonical "true" or "false".
     * Accepts "off"/"0"/"no"/"disabled" -> "false", "on"/"1"/"yes"/"enabled" -> "true",
     * and falls back safely for any unrecognized or corrupted value.
     */
    public static String normalizeBooleanOptionValue(String raw, boolean fallback) {
        return normalizeBooleanOptionValue(null, raw, fallback);
    }

    public static String normalizeBooleanOptionValue(String optionKey, String raw, boolean fallback) {
        if (raw == null) {
            return Boolean.toString(fallback);
        }
        String cleaned = stripOptionalQuotes(raw.trim()).trim().toLowerCase(Locale.ROOT);
        return switch (cleaned) {
            case "true", "on", "1", "yes", "enabled", "enable" -> "true";
            case "false", "off", "0", "no", "disabled", "disable", "" -> "false";
            default -> {
                if (optionKey != null) {
                    AzyronClient.LOGGER.warn(
                            "[AZYRON] Normalized invalid boolean value '{}' for option '{}' -> '{}'",
                            raw, optionKey, fallback
                    );
                }
                yield Boolean.toString(fallback);
            }
        };
    }

    public static boolean parseNormalizedBoolean(String raw, boolean fallback) {
        return "true".equals(normalizeBooleanOptionValue(null, raw, fallback));
    }

    /**
     * Sanitizes an options.txt NbtCompound in-place right after GameOptions.update(NbtCompound)
     * and right before GameOptions.Visitor parses SimpleOption codecs.
     */
    public static NbtCompound sanitizeOptionsCompound(NbtCompound nbt) {
        if (nbt == null) {
            return null;
        }
        try {
            for (String[] entry : BOOLEAN_OPTION_DEFAULTS) {
                String key = entry[0];
                boolean fallback = "true".equals(entry[1]);
                if (!nbt.contains(key)) {
                    continue;
                }
                Optional<String> rawOpt = nbt.getString(key);
                if (rawOpt == null || rawOpt.isEmpty()) {
                    nbt.putString(key, Boolean.toString(fallback));
                    continue;
                }
                String raw = rawOpt.get();
                if ("true".equals(raw) || "false".equals(raw)) {
                    continue;
                }
                String normalized = normalizeBooleanOptionValue(key, raw, fallback);
                nbt.putString(key, normalized);
                if ("fullscreen".equals(key)) {
                    AzyronClient.LOGGER.info(
                            "[AZYRON] Normalized options.txt fullscreen='{}' -> '{}'",
                            raw, normalized
                    );
                }
            }
        } catch (Throwable t) {
            AzyronClient.LOGGER.warn("[AZYRON] Non-fatal warning while sanitizing options compound: {}", t.getMessage());
        }
        return nbt;
    }

    private static String stripOptionalQuotes(String s) {
        if (s.length() >= 2) {
            char first = s.charAt(0);
            char last = s.charAt(s.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return s.substring(1, s.length() - 1);
            }
        }
        return s;
    }
}
