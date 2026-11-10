package com.jigneshgaming.azyron.profile;

import com.jigneshgaming.azyron.AzyronClient;
import com.jigneshgaming.azyron.util.McVersionBridge;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.entity.Entity;
import net.minecraft.particle.ParticlesMode;

/**
 * Coordinates AZYRON CLIENT performance profiles, automatic 3GB–8GB RAM hardware calibration,
 * live frame-spike & heap-pressure dynamic throttling, unfocused background power saving,
 * and companion mod optimization presets.
 */
public final class PerformanceEngine {
    private static final int UNFOCUSED_FPS_CAP = 15;
    public static final int LOW_RAM_FPS_TARGET = 60;
    public static final int REQUESTED_LOW_RAM_SIMULATION_DISTANCE = 4;
    // Conservative vanilla-compatible floor; four needs version-specific game validation.
    // Do not serialize an out-of-range four or bypass the option codec/tick semantics.
    public static final int MIN_SAFE_SIMULATION_DISTANCE = 5;

    private PerformanceProfile activeProfile = PerformanceProfile.BALANCED;
    private int particlesSpawnedThisTick = 0;
    private int culledParticlesCount = 0;
    private String activeBannerMessage = "";
    private long bannerExpireTimeMs = 0L;
    private boolean companionConfigsSeeded = false;
    private boolean initialRamCalibrationApplied = false;

    private boolean windowFocused = true;
    private int savedFocusedMaxFps = -1;
    private Boolean dynamicFpsModInstalled = null;

    public PerformanceEngine(PerformanceProfile initialProfile) {
        this.activeProfile = initialProfile != null ? initialProfile : PerformanceProfile.BALANCED;
    }

    public PerformanceProfile getActiveProfile() {
        return activeProfile;
    }

    public boolean isWindowFocused() {
        return windowFocused;
    }

    public void onClientTick(MinecraftClient client) {
        particlesSpawnedThisTick = 0;
        boolean autoMem = AzyronClient.getInstance() == null
                || AzyronClient.getInstance().getConfig() == null
                || AzyronClient.getInstance().getConfig().isAutoMemoryOptimization();
        MemoryOptimizer.onClientTick(client, autoMem || activeProfile != PerformanceProfile.BALANCED);
        MultiplayerPerformanceController.onClientTick(client);

        if (!initialRamCalibrationApplied && client != null && client.options != null) {
            initialRamCalibrationApplied = true;
            if (autoMem) {
                applyAutoRamCalibration(client);
            }
        }
    }

    /**
     * Automatically clamps excessive vanilla render/simulation/biome/entity settings to safe bounds
     * for the detected 3GB–8GB RAM tier to reduce memory pressure and chunk-load work,
     * while preserving tighter user settings. This cannot guarantee FPS or prevent every OOM.
     */
    public void applyAutoRamCalibration(MinecraftClient client) {
        if (client == null || client.options == null) {
            return;
        }
        MemoryOptimizer.RamTier tier = MemoryOptimizer.detectRamTier();
        GameOptions opts = client.options;
        boolean changed = false;

        try {
            if (opts.syncChunkWrites && !IntegratedServerOptimizer.shouldForceSyncChunkWrites(opts.syncChunkWrites)) {
                opts.syncChunkWrites = false;
                changed = true;
            }

            int curView = opts.getViewDistance().getValue();
            if (curView > tier.getMaxSafeViewDistance()) {
                opts.getViewDistance().setValue(tier.getMaxSafeViewDistance());
                changed = true;
            }

            int curSim = opts.getSimulationDistance().getValue();
            if (curSim > tier.getMaxSafeSimDistance()) {
                opts.getSimulationDistance().setValue(tier.getMaxSafeSimDistance());
                changed = true;
            }

            int curBlend = opts.getBiomeBlendRadius().getValue();
            if (curBlend > tier.getMaxSafeBiomeBlend()) {
                opts.getBiomeBlendRadius().setValue(tier.getMaxSafeBiomeBlend());
                changed = true;
            }

            if (client.world == null) {
                int curMip = opts.getMipmapLevels().getValue();
                if (curMip > tier.getMaxSafeMipmap()) {
                    opts.getMipmapLevels().setValue(tier.getMaxSafeMipmap());
                    changed = true;
                }
            }

            double curEntScale = opts.getEntityDistanceScaling().getValue();
            if (curEntScale > tier.getMaxSafeEntityScale()) {
                opts.getEntityDistanceScaling().setValue(tier.getMaxSafeEntityScale());
                changed = true;
            }

            if (tier == MemoryOptimizer.RamTier.TIER_3GB) {
                McVersionBridge.setGraphicsMode(opts, GraphicsMode.FAST);
                opts.getCloudRenderMode().setValue(CloudRenderMode.OFF);
                opts.getParticles().setValue(ParticlesMode.MINIMAL);
                opts.getEntityShadows().setValue(false);
                opts.getAo().setValue(false);
                McVersionBridge.setVignette(opts, false);
                int currentFps = opts.getMaxFps().getValue();
                if (currentFps > LOW_RAM_FPS_TARGET) opts.getMaxFps().setValue(LOW_RAM_FPS_TARGET);
                changed = true;
            } else if (tier == MemoryOptimizer.RamTier.TIER_4GB) {
                McVersionBridge.setGraphicsMode(opts, GraphicsMode.FAST);
                opts.getEntityShadows().setValue(false);
                McVersionBridge.setVignette(opts, false);
                changed = true;
            }

            if (changed) {
                AzyronClient.LOGGER.info("[AZYRON] Auto-calibrated settings for {} (Max Heap: {} MB)",
                        tier.getLabel(), MemoryOptimizer.getMaxMb());
            }
        } catch (Throwable t) {
            AzyronClient.LOGGER.debug("[AZYRON] Non-fatal warning in auto RAM calibration: {}", t.getMessage());
        }
    }

    /**
     * Automatically throttles background FPS and GPU load when the Minecraft window loses focus
     * (unless Dynamic FPS is already handling window focus states).
     */
    public void onWindowFocusChanged(MinecraftClient client, boolean focused) {
        if (this.windowFocused == focused) {
            return;
        }
        this.windowFocused = focused;

        if (client == null || client.options == null || isDynamicFpsLoaded()) {
            return;
        }

        try {
            if (!focused) {
                int currentMax = client.options.getMaxFps().getValue();
                if (currentMax > UNFOCUSED_FPS_CAP) {
                    this.savedFocusedMaxFps = currentMax;
                    client.options.getMaxFps().setValue(UNFOCUSED_FPS_CAP);
                }
            } else if (this.savedFocusedMaxFps > 0) {
                client.options.getMaxFps().setValue(this.savedFocusedMaxFps);
                this.savedFocusedMaxFps = -1;
            }
        } catch (Throwable ignored) {
        }
    }

    private boolean isDynamicFpsLoaded() {
        if (dynamicFpsModInstalled == null) {
            try {
                FabricLoader loader = FabricLoader.getInstance();
                dynamicFpsModInstalled = loader.isModLoaded("dynamic_fps") || loader.isModLoaded("dynamicfps");
            } catch (Throwable ignored) {
                dynamicFpsModInstalled = false;
            }
        }
        return dynamicFpsModInstalled;
    }

    /**
     * Applies the chosen PerformanceProfile to Minecraft 1.21.11 GameOptions and persists config.
     */
    public void applyProfile(PerformanceProfile profile, MinecraftClient client, boolean showNotification) {
        if (profile == null) {
            return;
        }
        this.activeProfile = profile;
        this.initialRamCalibrationApplied = true;
        this.savedFocusedMaxFps = -1;

        if (client != null && client.options != null) {
            GameOptions opts = client.options;
            try {
                McVersionBridge.setGraphicsMode(opts, profile.getGraphicsMode());
                opts.getParticles().setValue(profile.getParticlesMode());
                opts.getCloudRenderMode().setValue(profile.getCloudRenderMode());
                opts.getViewDistance().setValue(profile.getViewDistance());
                opts.getSimulationDistance().setValue(profile.getSimulationDistance());
                opts.getBiomeBlendRadius().setValue(profile.getBiomeBlendRadius());
                // Avoid triggering an asynchronous full texture/shader reload mid-world on PojavLauncher
                if (client.world == null) {
                    opts.getMipmapLevels().setValue(profile.getMipmapLevels());
                }
                opts.getEntityDistanceScaling().setValue(profile.getEntityDistanceScaling());
                opts.getEntityShadows().setValue(profile.isEntityShadows());
                opts.getAo().setValue(profile.isAmbientOcclusion());
                McVersionBridge.setVignette(opts, profile.isVignette());
                opts.getEnableVsync().setValue(profile.isVsync());
                opts.getMaxFps().setValue(profile.getMaxFps());
                if (!IntegratedServerOptimizer.shouldForceSyncChunkWrites(opts.syncChunkWrites)) {
                    opts.syncChunkWrites = false;
                }
                // Profile switching previously bypassed the first-tick RAM clamp. Apply
                // the same safety bounds on BOTH versions before saving any preset.
                AzyronClient inst = AzyronClient.getInstance();
                if (inst == null || inst.getConfig() == null || inst.getConfig().isAutoMemoryOptimization()) {
                    applyAutoRamCalibration(client);
                }
                opts.write();
            } catch (Throwable t) {
                AzyronClient.LOGGER.warn("[AZYRON] Non-fatal warning applying GameOptions preset: {}", t.getMessage());
            }
        }

        if (!this.companionConfigsSeeded) {
            seedCompanionModConfigsIfMissing();
        }

        if (AzyronClient.getInstance() != null && AzyronClient.getInstance().getConfig() != null) {
            AzyronClient.getInstance().getConfig().setActiveProfile(profile);
            AzyronClient.getInstance().getConfig().save();
        }

        if (showNotification) {
            this.activeBannerMessage = "Profile Activated: " + profile.getDisplayName();
            this.bannerExpireTimeMs = System.currentTimeMillis() + 3000L;
        }
    }

    /**
     * Fast-path O(1) particle gate called by ParticleManagerMixin.
     * Dynamically tightens particle budgets during frame spikes, high RAM pressure, or unfocused windows.
     */
    public boolean shouldAllowParticle(double x, double y, double z) {
        if (!windowFocused) {
            culledParticlesCount++;
            return false;
        }

        int budget = activeProfile.getMaxParticlesPerTick();
        double maxDist = activeProfile.getMaxParticleDistanceBlocks();

        // Dynamically tighten particle load during server-join warmup, live frame spikes, or 3GB–8GB RAM pressure
        if (MultiplayerPerformanceController.isInJoinWarmup()
                || FrameTimeTracker.isExperiencingFrameSpike()
                || MemoryOptimizer.isCriticalMemoryPressure()) {
            budget = Math.max(12, Math.min(16, budget / 4));
            maxDist = Math.min(maxDist, 14.0);
        } else if (activeProfile != PerformanceProfile.POTATO && MemoryOptimizer.isHighMemoryPressure()) {
            budget = Math.min(budget, MemoryOptimizer.detectRamTier().getDynamicParticleCap());
            maxDist = Math.min(maxDist, 24.0);
        }

        if (particlesSpawnedThisTick >= budget) {
            culledParticlesCount++;
            return false;
        }

        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.player != null) {
            Entity p = client.player;
            double dx = x - p.getX();
            double dy = y - p.getY();
            double dz = z - p.getZ();
            double distSq = dx * dx + dy * dy + dz * dz;
            if (distSq > maxDist * maxDist) {
                culledParticlesCount++;
                return false;
            }
            // In Potato Mode, probabilistically thin mid-range ambient particles beyond 8 blocks
            if (activeProfile == PerformanceProfile.POTATO && distSq > 64.0 && ThreadLocalRandom.current().nextBoolean()) {
                culledParticlesCount++;
                return false;
            }
        }

        particlesSpawnedThisTick++;
        return true;
    }

    public int getCulledParticlesCount() {
        return culledParticlesCount;
    }

    public String getActiveBannerMessage() {
        if (activeBannerMessage.isEmpty()) {
            return "";
        }
        if (System.currentTimeMillis() > bannerExpireTimeMs) {
            activeBannerMessage = "";
            return "";
        }
        return activeBannerMessage;
    }

    /** Seed only reviewed version-specific schemas for installed mods.
     * Other optimizers retain their own defaults: no guessed cross-version keys.
     */
    public void seedCompanionModConfigsIfMissing() {
        if (this.companionConfigsSeeded) return;
        try {
            FabricLoader loader = FabricLoader.getInstance();
            Path configDir = loader.getConfigDir();
            MemoryOptimizer.RamTier tier = MemoryOptimizer.detectRamTier();
            int cpuAheadLimit = tier == MemoryOptimizer.RamTier.TIER_3GB
                    || tier == MemoryOptimizer.RamTier.TIER_4GB ? 2 : 3;
            McVersionBridge.SupportedMcVersion version = McVersionBridge.getRuntimeMcVersion();
            if (loader.isModLoaded("sodium")) {
                Files.createDirectories(configDir);
                writeIfAbsent(configDir.resolve("sodium-options.json"),
                        buildSodiumOptionsJson(version, MemoryOptimizer.computeOptimalChunkBuilderThreads(), cpuAheadLimit));
            }
            if (loader.isModLoaded("dynamic_fps") || loader.isModLoaded("dynamicfps")) {
                Files.createDirectories(configDir);
                writeIfAbsent(configDir.resolve("dynamic_fps.json"), buildDynamicFpsOptionsJson(version));
            }
            this.companionConfigsSeeded = true;
        } catch (IOException e) {
            AzyronClient.LOGGER.debug("[AZYRON] Companion config seeding deferred: {}", e.getMessage());
        }
    }

    public static String buildSodiumOptionsJson(
            McVersionBridge.SupportedMcVersion mcVer,
            int chunkThreads,
            int cpuAheadLimit
    ) {
        if (mcVer == McVersionBridge.SupportedMcVersion.MC_1_20_1) {
            // Sodium 0.5.x (Minecraft 1.20.1) schema uses always_defer_chunk_updates_v2
            return "{\n"
                    + "  \"quality\": {\n"
                    + "    \"weather_quality\": \"FAST\",\n"
                    + "    \"leaves_quality\": \"FAST\",\n"
                    + "    \"enable_vignette\": false\n"
                    + "  },\n"
                    + "  \"performance\": {\n"
                    + "    \"chunk_builder_threads\": " + chunkThreads + ",\n"
                    + "    \"always_defer_chunk_updates_v2\": true,\n"
                    + "    \"animate_only_visible_textures\": true,\n"
                    + "    \"use_entity_culling\": true,\n"
                    + "    \"use_fog_occlusion\": true,\n"
                    + "    \"use_block_face_culling\": true,\n"
                    + "    \"use_no_error_g_l_context\": false\n"
                    + "  },\n"
                    + "  \"advanced\": {\n"
                    + "    \"cpu_render_ahead_limit\": " + cpuAheadLimit + "\n"
                    + "  }\n"
                    + "}\n";
        }
        // Sodium 0.8.x (Minecraft 1.21.11) schema uses chunk_build_defer_mode
        return "{\n"
                + "  \"quality\": {\n"
                + "    \"hidden_fluid_culling\": true\n"
                + "  },\n"
                + "  \"performance\": {\n"
                + "    \"chunk_builder_threads\": " + chunkThreads + ",\n"
                + "    \"chunk_build_defer_mode\": \"ALWAYS\",\n"
                + "    \"animate_only_visible_textures\": true,\n"
                + "    \"use_entity_culling\": true,\n"
                + "    \"use_fog_occlusion\": true,\n"
                + "    \"use_block_face_culling\": true,\n"
                + "    \"use_no_error_g_l_context\": false\n"
                + "  },\n"
                + "  \"advanced\": {\n"
                + "    \"cpu_render_ahead_limit\": " + cpuAheadLimit + "\n"
                + "  }\n"
                + "}\n";
    }

    /** Dynamic FPS 3.2.1 requires every state codec field; 3.11.6 uses volume maps. */
    public static String buildDynamicFpsOptionsJson(McVersionBridge.SupportedMcVersion version) {
        boolean legacy = version == McVersionBridge.SupportedMcVersion.MC_1_20_1;
        StringBuilder json = new StringBuilder(legacy ? "{\"states\":{" :
                "{\"enabled\":true,\"uncap_menu_frame_rate\":false,\"download_natives\":false,"
                + "\"battery_tracker\":{\"enabled\":false},\"states\":{");
        String[] names = {"hovered", "unfocused", "invisible"};
        int[] rates = {60, 15, 1};
        double[] volumes = {1.0, 0.25, 0.0};
        for (int i = 0; i < names.length; i++) {
            if (i > 0) json.append(',');
            json.append('"').append(names[i]).append("\":{\"frame_rate_target\":").append(rates[i]);
            if (legacy) json.append(",\"volume_multiplier\":").append(volumes[i]);
            else json.append(",\"enable_vsync\":false,\"volume_multipliers\":{\"master\":")
                    .append(volumes[i]).append('}');
            json.append(",\"graphics_state\":\"default\",\"show_toasts\":")
                    .append(i == 0).append(",\"run_garbage_collector\":false}");
        }
        return json.append("}}\n").toString();
    }

    private static void writeIfAbsent(Path path, String content) throws IOException {
        if (!Files.exists(path)) {
            Files.writeString(path, content, StandardCharsets.UTF_8);
        }
    }
}
