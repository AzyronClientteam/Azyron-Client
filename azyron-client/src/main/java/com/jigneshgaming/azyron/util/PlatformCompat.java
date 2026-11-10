package com.jigneshgaming.azyron.util;

import com.jigneshgaming.azyron.AzyronClient;
import com.mojang.text2speech.Narrator;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.session.Session;

/**
 * Cross-platform runtime compatibility layer for Desktop (Windows/macOS/Linux) and
 * Android launchers (PojavLauncher, ZalithLauncher, Amethyst, FoldCraft) on Minecraft 1.21.11 & 1.20.1.
 */
public final class PlatformCompat {
    private static final String[] LINUX_FLITE_CANDIDATE_DIRS = {
            "/usr/lib",
            "/usr/lib64",
            "/lib",
            "/lib64",
            "/usr/local/lib",
            "/usr/lib/x86_64-linux-gnu",
            "/usr/lib/aarch64-linux-gnu",
            "/usr/lib/arm-linux-gnueabihf"
    };
    private static final String[] FLITE_FILENAMES = {
            "libflite.so.1",
            "libflite.so"
    };

    private static final AtomicBoolean LOGGED_NARRATOR_FALLBACK = new AtomicBoolean(false);
    private static final AtomicBoolean LOGGED_OFFLINE_REALMS_SKIP = new AtomicBoolean(false);

    private static volatile Boolean simulatedAndroidOverride = null;
    private static volatile Boolean simulatedFliteAvailableOverride = null;

    private PlatformCompat() {
    }

    /**
     * Detects whether the JVM is running on Android / PojavLauncher (or derivative mobile launchers).
     */
    public static boolean isAndroidOrPojav() {
        Boolean override = simulatedAndroidOverride;
        return override != null ? override : AndroidRuntime.DETECTED;
    }

    // Probe environment/native paths only once, not on every splash/HUD frame.
    private static final class AndroidRuntime {
        private static final boolean DETECTED = detectAndroidOrPojav();
    }

    private static boolean detectAndroidOrPojav() {
        try {
            if (hasNonBlankEnv("POJAV_RENDERER")
                    || hasNonBlankEnv("POJAV_ENVIRON")
                    || hasNonBlankEnv("POJAV_NATIVEDIR")
                    || hasNonBlankEnv("MOD_ANDROID_RUNTIME")
                    || hasNonBlankEnv("ZALITH_VERSION")
                    || hasNonBlankEnv("FCL_NATIVEDIR")) {
                return true;
            }
            if (hasNonBlankEnv("ANDROID_ROOT") && hasNonBlankEnv("ANDROID_DATA")) {
                return true;
            }
            String vendor = (System.getProperty("java.vendor", "") + " "
                    + System.getProperty("java.vm.vendor", "") + " "
                    + System.getProperty("java.specification.vendor", "")).toLowerCase(Locale.ROOT);
            if (vendor.contains("android") || vendor.contains("termux") || vendor.contains("pojav")) {
                return true;
            }
            String libPath = System.getProperty("java.library.path", "").toLowerCase(Locale.ROOT);
            if (libPath.contains("pojavlaunch") || libPath.contains("/data/user/0/") || libPath.contains("/data/data/")) {
                return true;
            }
            return Files.exists(Paths.get("/system/build.prop"));
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Checks whether GNU/Linux libflite TTS shared libraries exist before Mojang's NarratorLinux
     * attempts JNA dlopen("libflite.so.1"), avoiding crashes and native link error stacktraces.
     */
    public static boolean isFliteNativeLibraryPresent() {
        Boolean override = simulatedFliteAvailableOverride;
        if (override != null) {
            return override;
        }
        if (isAndroidOrPojav()) {
            return false;
        }
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (!os.contains("linux")) {
            // Windows (SAPI) and macOS (AVSpeechSynthesizer / NSSpeechSynthesizer) do not use Flite
            return true;
        }

        try {
            String searchPaths = System.getProperty("jna.library.path", "")
                    + File.pathSeparator + System.getProperty("java.library.path", "")
                    + File.pathSeparator + Optional.ofNullable(System.getenv("LD_LIBRARY_PATH")).orElse("");
            for (String entry : searchPaths.split(File.pathSeparator)) {
                if (containsFliteBinary(entry)) {
                    return true;
                }
            }
            for (String stdDir : LINUX_FLITE_CANDIDATE_DIRS) {
                if (containsFliteBinary(stdDir)) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean containsFliteBinary(String dirPath) {
        if (dirPath == null || dirPath.isBlank()) {
            return false;
        }
        Path dir = Paths.get(dirPath.trim());
        if (!Files.isDirectory(dir)) {
            return false;
        }
        for (String name : FLITE_FILENAMES) {
            if (Files.isRegularFile(dir.resolve(name))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Safely initializes Minecraft's Narrator, returning Narrator.EMPTY gracefully on Android /
     * missing-Flite systems or whenever native TTS initialization throws any Throwable/FatalException.
     */
    public static Narrator createSafeNarrator() {
        if (isAndroidOrPojav() || !isFliteNativeLibraryPresent()) {
            logNarratorFallbackOnce("Native Flite TTS library is unavailable on this platform; using silent Narrator.EMPTY fallback.");
            return Narrator.EMPTY;
        }
        try {
            Narrator narrator = Narrator.getNarrator();
            return narrator != null ? narrator : Narrator.EMPTY;
        } catch (Throwable t) {
            logNarratorFallbackOnce("Native TTS initialization failed (" + t.getClass().getSimpleName()
                    + ": " + t.getMessage() + "); falling back to Narrator.EMPTY.");
            return Narrator.EMPTY;
        }
    }

    public static void logNarratorFallbackOnce(String message) {
        if (LOGGED_NARRATOR_FALLBACK.compareAndSet(false, true)) {
            AzyronClient.LOGGER.info("[AZYRON] {}", message);
        }
    }

    /**
     * Determines whether the active Minecraft Session is an offline / local unauthenticated profile.
     * Does NOT modify or bypass Microsoft authentication; online MSA tokens return false so Realms
     * continues to authenticate normally.
     */
    public static boolean isOfflineOrLocalSession(Session session) {
        if (session == null) {
            return true;
        }
        try {
            return isOfflineOrLocalToken(session.getAccessToken(), session.getXuid());
        } catch (Throwable ignored) {
            return true;
        }
    }

    public static boolean isOfflineOrLocalToken(String accessToken, Optional<String> xuid) {
        if (accessToken == null) {
            return true;
        }
        String trimmed = accessToken.trim();
        if (trimmed.isEmpty()
                || "0".equals(trimmed)
                || "-".equals(trimmed)
                || "null".equalsIgnoreCase(trimmed)
                || "offline".equalsIgnoreCase(trimmed)
                || "local".equalsIgnoreCase(trimmed)
                || "fabric_loader".equalsIgnoreCase(trimmed)) {
            return true;
        }
        // Real Microsoft / Minecraft Services bearer tokens are long JWT/opaque tokens (>> 32 chars);
        // short placeholder tokens without an XUID belong to local/offline launch profiles.
        if (trimmed.length() < 16 && (xuid == null || xuid.isEmpty() || xuid.get().isBlank())) {
            return true;
        }
        return false;
    }

    public static void logOfflineRealmsSkipOnce(String username) {
        if (LOGGED_OFFLINE_REALMS_SKIP.compareAndSet(false, true)) {
            AzyronClient.LOGGER.info(
                    "[AZYRON] Local/offline account '{}' detected — skipping background Realms probe (online Microsoft account required for Realms).",
                    username != null ? username : "Player"
            );
        }
    }

    /**
     * Audits installed companion performance mods at startup and warns if an Android/PojavLauncher
     * user manually installed a desktop-only OpenGL mod known to corrupt mobile GL translation layers.
     */
    public static void auditInstalledModCompatibility() {
        try {
            FabricLoader loader = FabricLoader.getInstance();
            if (isAndroidOrPojav() && loader.isModLoaded("immediatelyfast")) {
                AzyronClient.LOGGER.warn(
                        "[AZYRON] Compatibility Notice: ImmediatelyFast is loaded on Android/PojavLauncher. "
                        + "Upstream ImmediatelyFast does not support mobile OpenGL translation layers (GL4ES/ANGLE/MobileGlues) "
                        + "and may cause mob texture glitches or native crashes; safe mobile config fallbacks have been applied."
                );
            }
        } catch (Throwable ignored) {
        }
    }

    public static void setSimulatedAndroidForTesting(Boolean simulatedAndroid) {
        simulatedAndroidOverride = simulatedAndroid;
        LOGGED_NARRATOR_FALLBACK.set(false);
    }

    public static void setSimulatedFliteAvailableForTesting(Boolean simulatedFlite) {
        simulatedFliteAvailableOverride = simulatedFlite;
        LOGGED_NARRATOR_FALLBACK.set(false);
    }

    private static boolean hasNonBlankEnv(String key) {
        String val = System.getenv(key);
        return val != null && !val.isBlank();
    }
}
