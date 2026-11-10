package com.jigneshgaming.azyron;

import com.jigneshgaming.azyron.config.AzyronConfig;
import com.jigneshgaming.azyron.profile.MemoryOptimizer;
import com.jigneshgaming.azyron.profile.PerformanceEngine;
import com.jigneshgaming.azyron.profile.PerformanceProfile;
import com.jigneshgaming.azyron.ui.AzyronLoadingRenderer;
import com.jigneshgaming.azyron.util.McVersionBridge;
import com.jigneshgaming.azyron.util.OptionsConfigSanitizer;
import com.jigneshgaming.azyron.util.PlatformCompat;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.nbt.NbtCompound;

/** Isolated JVM regression groups using the generated test doubles, NOT device tests.
 * Invoke separately with options | startup | mobile | config and both version properties.
 */
public final class AzyronAndroidRegressionSuite {
    private static int assertions;
    public static void main(String[] args) throws Exception {
        switch (args[0]) {
            case "options" -> options();
            case "startup" -> startup();
            case "mobile" -> mobile();
            case "config" -> config();
            default -> throw new IllegalArgumentException("Unknown test group");
        }
        System.out.println("PASS " + args[0] + " (" + assertions + " assertions, stubbed JVM, MC "
                + McVersionBridge.getActiveMcVersionString() + ")");
    }
    private static void check(boolean ok, String message) {
        assertions++;
        if (!ok) throw new AssertionError(message);
    }
    private static void options() {
        for (String raw : new String[]{null, "", " ", "\"\"", "null", "[]", "{}", "invalid"}) {
            check("true".equals(OptionsConfigSanitizer.normalizeBooleanOptionValue(raw, true)), "true fallback: " + raw);
            check("false".equals(OptionsConfigSanitizer.normalizeBooleanOptionValue(raw, false)), "false fallback: " + raw);
        }
        check("false".equals(OptionsConfigSanitizer.normalizeBooleanOptionValue(" OFF ", true)), "Explicit false preserved");
        check("true".equals(OptionsConfigSanitizer.normalizeBooleanOptionValue("\"on\"", false)), "Quoted launcher bool");
        for (String raw : new String[]{null, "", "\"\"", "true", "false", "null", "[]", "{}", "NaN", "Infinity", "1e999", "-0.1", "1.1", "0x1p0", "0.5f"}) {
            check("0.5".equals(OptionsConfigSanitizer.normalizeUnitDoubleOptionValue(raw, 0.5)), "Typed numeric fallback: " + raw);
        }
        String[] valid = {"0", "1", "0.25", "1e-1", "\"0.75\""};
        double[] values = {0.0, 1.0, 0.25, 0.1, 0.75};
        for (int i = 0; i < valid.length; i++) {
            String result = OptionsConfigSanitizer.normalizeUnitDoubleOptionValue(valid[i], 0.5);
            check(Double.parseDouble(result) == values[i], "Valid slider value preserved: " + valid[i]);
        }
        NbtCompound input = new NbtCompound();
        input.putString("textBackgroundOpacity", "");
        input.putString("backgroundForChatOnly", "");
        input.putString("enableVsync", "???");
        input.putString("fullscreen", "off");
        input.putString("key_key.attack", "key.mouse.left");
        input.putString("resourcePacks", "[\"vanilla\"]");
        input.putInt("chatScale", 2); // wrong NBT type
        OptionsConfigSanitizer.sanitizeOptionsCompound(input);
        check("0.5".equals(McVersionBridge.nbtGetString(input, "textBackgroundOpacity").orElse("")), "Opacity double default");
        check("true".equals(McVersionBridge.nbtGetString(input, "backgroundForChatOnly").orElse("")), "Chat background boolean default");
        check("true".equals(McVersionBridge.nbtGetString(input, "enableVsync").orElse("")), "Invalid later key repaired independently");
        check("1.0".equals(McVersionBridge.nbtGetString(input, "chatScale").orElse("")), "Wrong-type numeric fallback");
        check("false".equals(McVersionBridge.nbtGetString(input, "fullscreen").orElse("")), "Fullscreen off canonical");
        check(!input.contains("gamma"), "Missing values left to vanilla");
        check("key.mouse.left".equals(McVersionBridge.nbtGetString(input, "key_key.attack").orElse("")), "Unknown/key binding preserved");
        check("[\"vanilla\"]".equals(McVersionBridge.nbtGetString(input, "resourcePacks").orElse("")), "Resource pack list preserved");
        check(OptionsConfigSanitizer.sanitizeOptionsCompound(null) == null, "Null compound safe");
        OptionsConfigSanitizer.sanitizeOptionsCompound(input);
        check("0.5".equals(McVersionBridge.nbtGetString(input, "textBackgroundOpacity").orElse("")), "Idempotent repair");
    }
    private static void startup() throws Exception {
        PerformanceEngine engine = new PerformanceEngine(PerformanceProfile.POTATO);
        Path sodium = FabricLoader.getInstance().getConfigDir().resolve("sodium-options.json");
        engine.seedCompanionModConfigsIfMissing();
        check(Files.exists(sodium), "Initial seed works");
        String original = Files.readString(sodium);
        Files.delete(sodium);
        engine.seedCompanionModConfigsIfMissing();
        check(!Files.exists(sodium), "Repeated seed avoids filesystem work");
        new PerformanceEngine(PerformanceProfile.POTATO).seedCompanionModConfigsIfMissing();
        check(Files.exists(sodium), "Fresh instance still seeds");
        Files.writeString(sodium, "user supplied config");
        new PerformanceEngine(PerformanceProfile.POTATO).seedCompanionModConfigsIfMissing();
        check("user supplied config".equals(Files.readString(sodium)), "Existing user config untouched");
        Files.writeString(sodium, original);
    }
    private static void mobile() {
        try {
            MemoryOptimizer.setSimulatedRamForTesting(1280, 3072);
            check(MemoryOptimizer.detectRamTier() == MemoryOptimizer.RamTier.TIER_3GB, "3GB physical/1280MB heap simulated tier");
            MinecraftClient mc = MinecraftClient.getInstance();
            PlatformCompat.setSimulatedAndroidForTesting(false);
            DrawContext desktop = new DrawContext(1280, 720);
            AzyronLoadingRenderer.render(desktop, mc, 0.5f, 1.0f, false);
            PlatformCompat.setSimulatedAndroidForTesting(true);
            DrawContext mobile = new DrawContext(1280, 720);
            AzyronLoadingRenderer.render(mobile, mc, 0.5f, 1.0f, false);
            check(mobile.fillCallCount < desktop.fillCallCount, "Cold-boot mobile path submits fewer simulated quads");
            DrawContext warm = new DrawContext(1280, 720);
            AzyronLoadingRenderer.render(warm, mc, 0.5f, 1.0f, true);
            check(warm.fillCallCount == 3, "Warm mobile background/track/progress only");
            check(warm.drawnStrings.contains("AZYRON CLIENT"), "Mobile branding retained");
            check(PlatformCompat.createSafeNarrator() == com.mojang.text2speech.Narrator.EMPTY, "No Android flite loading");
        } finally {
            PlatformCompat.setSimulatedAndroidForTesting(null);
            MemoryOptimizer.setSimulatedRamForTesting(-1, -1);
        }
    }
    private static void config() throws Exception {
        Path dir = Files.createTempDirectory("azyron-config-regression-");
        Path file = dir.resolve("azyron-client.json");
        AzyronClient client = AzyronClient.bootstrapHeadlessForTests(file);
        String id = client.getModuleManager().getModules().get(0).getId();
        Files.writeString(file, "{\"activeProfile\":null,\"modules\":{\"" + id + "\":{\"settings\":null}}}");
        AzyronConfig config = new AzyronConfig(file);
        config.load();
        config.applyToManagers(client.getModuleManager(), client.getCosmeticsManager());
        config.captureFromManagers(client.getModuleManager(), client.getCosmeticsManager());
        config.save();
        check(Files.size(file) > 0, "Null settings can be captured and saved");
        Files.writeString(file, "{invalid");
        config.load();
        check(config.getActiveProfile() != null, "Malformed JSON restores defaults");
        try (var files = Files.list(dir)) {
            check(files.anyMatch(p -> p.getFileName().toString().endsWith(".bak")), "Malformed original backed up");
        }
    }
}
