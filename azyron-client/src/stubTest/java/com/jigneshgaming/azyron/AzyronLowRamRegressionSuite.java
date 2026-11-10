package com.jigneshgaming.azyron;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jigneshgaming.azyron.profile.*;
import com.jigneshgaming.azyron.util.McVersionBridge;
import java.nio.file.Files;
import java.nio.file.Path;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.integrated.IntegratedServer;

/** Deterministic GENERATED-STUB JVM checks. NOT a Fabric launch or FPS benchmark.
 * Run in separate JVMs with -Dazyron.mcVersion=1.20.1 and =1.21.11.
 */
public final class AzyronLowRamRegressionSuite {
    private static int assertions;
    private static void check(boolean value, String message) {
        assertions++;
        if (!value) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        Path config = Files.createTempDirectory("azyron-lowram-").resolve("client.json");
        MemoryOptimizer.setSimulatedRamForTesting(1408, 3072);
        try {
            AzyronClient client = AzyronClient.bootstrapHeadlessForTests(config);
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.world = null;
            client.getConfig().setAutoMemoryOptimization(true);
            for (PerformanceProfile profile : PerformanceProfile.values()) {
                mc.options.getMaxFps().setValue(260);
                client.getPerformanceEngine().applyProfile(profile, mc, false);
                check(mc.options.getViewDistance().getValue() <= 6, profile + " cannot undo render cap");
                check(mc.options.getSimulationDistance().getValue() == 5, profile + " uses safe simulation minimum");
                check(mc.options.getMaxFps().getValue() <= 60, profile + " cannot undo FPS target");
                check(mc.options.getBiomeBlendRadius().getValue() == 0, profile + " no biome blend cost");
                check(mc.options.getEntityDistanceScaling().getValue() <= .5, profile + " entity render budget");
                check(mc.options.getMipmapLevels().getValue() == 0, profile + " low-RAM mips");
            }
            mc.options.getMaxFps().setValue(30);
            new PerformanceEngine(PerformanceProfile.POTATO).onClientTick(mc);
            check(mc.options.getMaxFps().getValue() == 30, "Initial calibration preserves lower user FPS cap");
            client.getConfig().setAutoMemoryOptimization(false);
            client.getPerformanceEngine().applyProfile(PerformanceProfile.BALANCED, mc, false);
            check(mc.options.getViewDistance().getValue() == 10, "Explicit auto-memory opt-out works");
            check(mc.options.getMaxFps().getValue() == 260, "Opt-out leaves requested preset FPS intact");
            client.getConfig().setAutoMemoryOptimization(true);
            mc.options.getViewDistance().setValue(16);
            mc.options.getSimulationDistance().setValue(12);
            IntegratedServer server = new IntegratedServer(mc);
            IntegratedServerOptimizer.resetState();
            IntegratedServerOptimizer.onIntegratedServerTick(server, mc);
            check(server.getPlayerManager().getViewDistance() <= 6, "Singleplayer early view cap");
            check(server.getPlayerManager().getSimulationDistance() == 5, "Singleplayer early simulation cap");
            check(IntegratedServerOptimizer.computeDesiredSimStepDown(80, true, true, 5, 0) == 0,
                    "Pressure cannot reduce simulation below safe floor");
            check(IntegratedServerOptimizer.computeDesiredSimStepDown(80, true, true, 6, 0) == 1,
                    "Adaptive reduction respects floor");
            check(IntegratedServerOptimizer.shouldForceSyncChunkWrites(true), "Preserve synchronous save preference");
            check(!IntegratedServerOptimizer.shouldForceSyncChunkWrites(false), "Preserve existing async preference");
            System.setProperty("azyron.allowAsyncChunkWrites", "true");
            check(!IntegratedServerOptimizer.shouldForceSyncChunkWrites(true), "Async requires explicit opt-in");
            System.setProperty("azyron.forceSyncChunkWrites", "true");
            check(IntegratedServerOptimizer.shouldForceSyncChunkWrites(true), "Durability override wins");
            for (McVersionBridge.SupportedMcVersion version : McVersionBridge.SupportedMcVersion.values()) {
                boolean legacy = version == McVersionBridge.SupportedMcVersion.MC_1_20_1;
                JsonObject sodium = JsonParser.parseString(PerformanceEngine.buildSodiumOptionsJson(version, 1, 2)).getAsJsonObject();
                JsonObject perf = sodium.getAsJsonObject("performance");
                check(perf.get("chunk_builder_threads").getAsInt() == 1, "Bounded chunk rebuild worker");
                check(!perf.has("cpu_render_ahead_limit"), "Render ahead not in performance");
                check(sodium.getAsJsonObject("advanced").get("cpu_render_ahead_limit").getAsInt() == 2, "Render ahead in advanced");
                check(!perf.get("use_no_error_g_l_context").getAsBoolean(), "No GL no-error assumption on Pojav");
                check(legacy ? perf.get("always_defer_chunk_updates_v2").getAsBoolean()
                        : "ALWAYS".equals(perf.get("chunk_build_defer_mode").getAsString()), "Exact Sodium defer schema");
                JsonObject dynamic = JsonParser.parseString(PerformanceEngine.buildDynamicFpsOptionsJson(version)).getAsJsonObject();
                for (String state : new String[]{"hovered", "unfocused", "invisible"}) {
                    JsonObject value = dynamic.getAsJsonObject("states").getAsJsonObject(state);
                    check(value.has(legacy ? "volume_multiplier" : "volume_multipliers"), "Versioned Dynamic FPS volume schema");
                    check(!value.get("run_garbage_collector").getAsBoolean(), "No forced GC on focus change");
                    check("default".equals(value.get("graphics_state").getAsString()), "No background chunk reloads");
                    check(value.has("show_toasts"), "Required legacy codec field present");
                }
            }
            System.out.println("PASS low-RAM: " + assertions + " assertions, stubbed JVM, MC "
                    + McVersionBridge.getActiveMcVersionString() + "; NOT a device benchmark");
        } finally {
            MemoryOptimizer.setSimulatedRamForTesting(-1, -1);
            System.clearProperty("azyron.allowAsyncChunkWrites");
            System.clearProperty("azyron.forceSyncChunkWrites");
        }
    }
}
