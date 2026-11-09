package com.jigneshgaming.azyron.profile;

import com.jigneshgaming.azyron.AzyronClient;
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
     * for the detected 3GB–8GB RAM tier so low-memory PCs and PojavLauncher phones never OOM
     * or stutter on chunk loading, while preserving any tighter user settings.
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
                opts.applyGraphicsMode(GraphicsMode.FAST);
                opts.getCloudRenderMode().setValue(CloudRenderMode.OFF);
                opts.getParticles().setValue(ParticlesMode.MINIMAL);
                opts.getEntityShadows().setValue(false);
                opts.getAo().setValue(false);
                opts.getVignette().setValue(false);
                changed = true;
            } else if (tier == MemoryOptimizer.RamTier.TIER_4GB) {
                opts.applyGraphicsMode(GraphicsMode.FAST);
                opts.getEntityShadows().setValue(false);
                opts.getVignette().setValue(false);
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
                opts.applyGraphicsMode(profile.getGraphicsMode());
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
                opts.getVignette().setValue(profile.isVignette());
                opts.getEnableVsync().setValue(profile.isVsync());
                opts.getMaxFps().setValue(profile.getMaxFps());
                if (!IntegratedServerOptimizer.shouldForceSyncChunkWrites(opts.syncChunkWrites)) {
                    opts.syncChunkWrites = false;
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

    /**
     * Seeds RAM-tier-calibrated default config files for Sodium, Lithium, EntityCulling, MoreCulling,
     * Dynamic FPS, and ImmediatelyFast if the user does not already have custom configs.
     */
    public void seedCompanionModConfigsIfMissing() {
        this.companionConfigsSeeded = true;
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir();
            Files.createDirectories(configDir);

            MemoryOptimizer.RamTier tier = MemoryOptimizer.detectRamTier();
            int chunkThreads = MemoryOptimizer.computeOptimalChunkBuilderThreads();
            int cpuAheadLimit = (tier == MemoryOptimizer.RamTier.TIER_3GB || tier == MemoryOptimizer.RamTier.TIER_4GB) ? 2 : 3;
            int tracingDist = (tier == MemoryOptimizer.RamTier.TIER_3GB) ? 64 : 96;

            writeIfAbsent(configDir.resolve("sodium-options.json"),
                    "{\n"
                    + "  \"quality\": {\n"
                    + "    \"weather_quality\": \"FAST\",\n"
                    + "    \"leaves_quality\": \"FAST\",\n"
                    + "    \"enable_vignette\": false\n"
                    + "  },\n"
                    + "  \"performance\": {\n"
                    + "    \"chunk_builder_threads\": " + chunkThreads + ",\n"
                    + "    \"chunk_build_defer_mode\": \"NEARBY\",\n"
                    + "    \"cpu_render_ahead_limit\": " + cpuAheadLimit + ",\n"
                    + "    \"animate_only_visible_textures\": true,\n"
                    + "    \"use_entity_culling\": true,\n"
                    + "    \"use_fog_occlusion\": true,\n"
                    + "    \"use_block_face_culling\": true,\n"
                    + "    \"use_no_error_g_l_context\": false\n"
                    + "  }\n"
                    + "}\n"
            );

            writeIfAbsent(configDir.resolve("entityculling.json"),
                    "{\n"
                    + "  \"configVersion\": 9,\n"
                    + "  \"renderNametagsThroughWalls\": true,\n"
                    + "  \"tracingDistance\": " + tracingDist + ",\n"
                    + "  \"debugMode\": false,\n"
                    + "  \"sleepDelay\": 10,\n"
                    + "  \"hitboxLimit\": 50,\n"
                    + "  \"tickCulling\": true,\n"
                    + "  \"blockEntityFrustumCulling\": true,\n"
                    + "  \"safeMode\": false,\n"
                    + "  \"solidLeaves\": false\n"
                    + "}\n"
            );

            writeIfAbsent(configDir.resolve("moreculling.toml"),
                    "version = 1\n"
                    + "enableSodiumMenu = true\n"
                    + "cloudCulling = true\n"
                    + "signTextCulling = true\n"
                    + "rainCulling = true\n"
                    + "beaconBeamCulling = true\n"
                    + "entityModelCulling = false\n"
                    + "endGatewayCulling = true\n"
                    + "useBlockStateCulling = true\n"
                    + "useCustomItemFrameRenderer = true\n"
                    + "itemFrameMapCulling = true\n"
                    + "useItemFrameLOD = true\n"
                    + "itemFrameLODRange = 16\n"
                    + "useItemFrame3FaceCulling = true\n"
                    + "itemFrame3FaceCullingRange = 12.0\n"
                    + "leavesCullingMode = \"FAST\"\n"
                    + "leavesCullingAmount = 2\n"
                    + "includeMangroveRoots = true\n"
            );

            writeIfAbsent(configDir.resolve("lithium.properties"),
                    "# AZYRON CLIENT - Lithium 1.21.11 Optimization Preset\n"
                    + "mixin.ai=true\n"
                    + "mixin.alloc=true\n"
                    + "mixin.block=true\n"
                    + "mixin.chunk=true\n"
                    + "mixin.entity=true\n"
                    + "mixin.gen=true\n"
                    + "mixin.math=true\n"
                    + "mixin.shapes=true\n"
                    + "mixin.world=true\n"
            );

            // Seed valid ImmediatelyFast 1.14+ (MC 1.21.11) config ONLY if user manually installed ImmediatelyFast,
            // disabling enhanced_batching and FBO state skipping on Android/PojavLauncher to avoid mobile GL glitches.
            if (FabricLoader.getInstance().isModLoaded("immediatelyfast")) {
                boolean mobile = com.jigneshgaming.azyron.util.PlatformCompat.isAndroidOrPojav();
                writeIfAbsent(configDir.resolve("immediatelyfast.json"),
                        "{\n"
                        + "  \"enhanced_batching\": " + (!mobile) + ",\n"
                        + "  \"font_atlas_resizing\": false,\n"
                        + "  \"map_atlas_generation\": true,\n"
                        + "  \"map_atlas_size\": 1024,\n"
                        + "  \"skip_text_translucency_sorting\": true,\n"
                        + "  \"fast_text_lookup\": true,\n"
                        + "  \"avoid_redundant_framebuffer_switching\": " + (!mobile) + ",\n"
                        + "  \"batch_animated_item_updates\": " + (!mobile) + ",\n"
                        + "  \"fast_gui_layer_lookup\": true\n"
                        + "}\n"
                );
            }

            writeIfAbsent(configDir.resolve("dynamic_fps.json"),
                    "{\n"
                    + "  \"idle\": { \"timeout\": 180, \"condition\": \"none\" },\n"
                    + "  \"battery_tracker\": { \"enabled\": false },\n"
                    + "  \"states\": {\n"
                    + "    \"unfocused\": { \"frame_rate_target\": 15, \"volume_multipliers\": { \"master\": 0.25 } },\n"
                    + "    \"invisible\": { \"frame_rate_target\": 1, \"volume_multipliers\": { \"master\": 0.0 } },\n"
                    + "    \"abandoned\": { \"frame_rate_target\": 10 }\n"
                    + "  }\n"
                    + "}\n"
            );
        } catch (IOException ignored) {
        }
    }

    private static void writeIfAbsent(Path path, String content) throws IOException {
        if (!Files.exists(path)) {
            Files.writeString(path, content, StandardCharsets.UTF_8);
        }
    }
}
