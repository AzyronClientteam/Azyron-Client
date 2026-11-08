package com.jigneshgaming.azyron.profile;

import com.jigneshgaming.azyron.AzyronClient;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ThreadLocalRandom;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import net.minecraft.entity.Entity;

/**
 * Coordinates AZYRON CLIENT performance profiles, live GameOptions tuning,
 * adaptive particle/frame-spike throttling, and companion mod optimization presets.
 */
public final class PerformanceEngine {
    private PerformanceProfile activeProfile = PerformanceProfile.BALANCED;
    private int particlesSpawnedThisTick = 0;
    private int culledParticlesCount = 0;
    private String activeBannerMessage = "";
    private long bannerExpireTimeMs = 0L;

    public PerformanceEngine(PerformanceProfile initialProfile) {
        this.activeProfile = initialProfile != null ? initialProfile : PerformanceProfile.BALANCED;
    }

    public PerformanceProfile getActiveProfile() {
        return activeProfile;
    }

    public void onClientTick(MinecraftClient client) {
        particlesSpawnedThisTick = 0;
        MemoryOptimizer.onClientTick(client, activeProfile != PerformanceProfile.BALANCED);
    }

    /**
     * Applies the chosen PerformanceProfile to Minecraft 1.21.11 GameOptions and persists config.
     */
    public void applyProfile(PerformanceProfile profile, MinecraftClient client, boolean showNotification) {
        if (profile == null) {
            return;
        }
        this.activeProfile = profile;

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
                opts.ao.setValue(profile.isAmbientOcclusion());
                opts.vignette.setValue(profile.isVignette());
                opts.getEnableVsync().setValue(profile.isVsync());
                opts.getMaxFps().setValue(profile.getMaxFps());
                opts.write();
            } catch (Throwable t) {
                AzyronClient.LOGGER.warn("[AZYRON] Non-fatal warning applying GameOptions preset: {}", t.getMessage());
            }
        }

        seedCompanionModConfigsIfMissing();

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
     * Fast-path particle gate called by ParticleManagerMixin.
     * Prevents particle storms from tanking FPS on low-end PCs and PojavLauncher mobile GPUs.
     */
    public boolean shouldAllowParticle(double x, double y, double z) {
        MinecraftClient client = MinecraftClient.getInstance();
        int budget = activeProfile.getMaxParticlesPerTick();

        // Smooth FPS Mode dynamically tightens particle budget during frame-time spikes
        if (activeProfile == PerformanceProfile.SMOOTH_FPS && FrameTimeTracker.isExperiencingFrameSpike()) {
            budget = Math.max(16, budget / 3);
        }

        if (particlesSpawnedThisTick >= budget) {
            culledParticlesCount++;
            return false;
        }

        if (client != null && client.player != null) {
            Entity p = client.player;
            double dx = x - p.getX();
            double dy = y - p.getY();
            double dz = z - p.getZ();
            double distSq = dx * dx + dy * dy + dz * dz;
            double maxDist = activeProfile.getMaxParticleDistanceBlocks();
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
        if (System.currentTimeMillis() > bannerExpireTimeMs) {
            return "";
        }
        return activeBannerMessage;
    }

    /**
     * Seeds compatible default config files for Sodium, Lithium, EntityCulling, MoreCulling,
     * Dynamic FPS, and ImmediatelyFast if the user does not already have custom configs.
     */
    public void seedCompanionModConfigsIfMissing() {
        try {
            Path configDir = FabricLoader.getInstance().getConfigDir();
            Files.createDirectories(configDir);

            writeIfAbsent(configDir.resolve("sodium-options.json"),
                    "{\n"
                    + "  \"quality\": {\n"
                    + "    \"weather_quality\": \"FAST\",\n"
                    + "    \"leaves_quality\": \"FAST\",\n"
                    + "    \"enable_vignette\": false\n"
                    + "  },\n"
                    + "  \"performance\": {\n"
                    + "    \"chunk_builder_threads\": 0,\n"
                    + "    \"chunk_build_defer_mode\": \"NEARBY\",\n"
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
                    + "  \"tracingDistance\": 96,\n"
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

            writeIfAbsent(configDir.resolve("immediatelyfast.json"),
                    "{\n"
                    + "  \"font_atlas_resizing\": false,\n"
                    + "  \"map_atlas_generation\": true,\n"
                    + "  \"hud_batching\": true,\n"
                    + "  \"fast_text_lookup\": true,\n"
                    + "  \"fast_buffer_upload\": false\n"
                    + "}\n"
            );

            writeIfAbsent(configDir.resolve("dynamic_fps.json"),
                    "{\n"
                    + "  \"idle\": { \"timeout\": 180, \"condition\": \"none\" },\n"
                    + "  \"states\": {\n"
                    + "    \"unfocused\": { \"frame_rate_target\": 15, \"volume_multipliers\": { \"master\": 0.25 } },\n"
                    + "    \"invisible\": { \"frame_rate_target\": 0, \"volume_multipliers\": { \"master\": 0.0 } },\n"
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
