package com.jigneshgaming.azyron.profile;

import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;

/**
 * Three engineered AZYRON CLIENT performance profiles.
 */
public enum PerformanceProfile {
    POTATO(
            "Potato Mode",
            "Maximum FPS boost. Minimal particles, low graphics & aggressive culling.",
            GraphicsMode.FAST,
            ParticleQuality.MINIMAL,
            CloudRenderMode.OFF,
            6,
            5,
            0,
            0,
            0.50,
            false,
            false,
            false,
            false,
            60,
            40,
            18.0
    ),
    BALANCED(
            "Balanced Mode",
            "Balanced high FPS & crisp Minecraft visuals for everyday play.",
            GraphicsMode.FANCY,
            ParticleQuality.DECREASED,
            CloudRenderMode.FAST,
            10,
            8,
            2,
            2,
            0.75,
            true,
            true,
            false,
            false,
            260,
            180,
            42.0
    ),
    SMOOTH_FPS(
            "Smooth FPS Mode",
            "Prioritizes ultra-stable frame pacing, 1% lows & zero micro-stutter.",
            GraphicsMode.FAST,
            ParticleQuality.DECREASED,
            CloudRenderMode.FAST,
            8,
            6,
            2,
            2,
            0.75,
            false,
            true,
            false,
            false,
            120,
            100,
            28.0
    );

    public enum ParticleQuality { ALL, DECREASED, MINIMAL }

    private final String displayName;
    private final String summary;
    private final GraphicsMode graphicsMode;
    private final ParticleQuality particlesMode;
    private final CloudRenderMode cloudRenderMode;
    private final int viewDistance;
    private final int simulationDistance;
    private final int biomeBlendRadius;
    private final int mipmapLevels;
    private final double entityDistanceScaling;
    private final boolean entityShadows;
    private final boolean ambientOcclusion;
    private final boolean vignette;
    private final boolean vsync;
    private final int maxFps;
    private final int maxParticlesPerTick;
    private final double maxParticleDistanceBlocks;

    PerformanceProfile(
            String displayName,
            String summary,
            GraphicsMode graphicsMode,
            ParticleQuality particlesMode,
            CloudRenderMode cloudRenderMode,
            int viewDistance,
            int simulationDistance,
            int biomeBlendRadius,
            int mipmapLevels,
            double entityDistanceScaling,
            boolean entityShadows,
            boolean ambientOcclusion,
            boolean vignette,
            boolean vsync,
            int maxFps,
            int maxParticlesPerTick,
            double maxParticleDistanceBlocks
    ) {
        this.displayName = displayName;
        this.summary = summary;
        this.graphicsMode = graphicsMode;
        this.particlesMode = particlesMode;
        this.cloudRenderMode = cloudRenderMode;
        this.viewDistance = viewDistance;
        this.simulationDistance = simulationDistance;
        this.biomeBlendRadius = biomeBlendRadius;
        this.mipmapLevels = mipmapLevels;
        this.entityDistanceScaling = entityDistanceScaling;
        this.entityShadows = entityShadows;
        this.ambientOcclusion = ambientOcclusion;
        this.vignette = vignette;
        this.vsync = vsync;
        this.maxFps = maxFps;
        this.maxParticlesPerTick = maxParticlesPerTick;
        this.maxParticleDistanceBlocks = maxParticleDistanceBlocks;
    }

    public String getDisplayName() { return displayName; }
    public String getSummary() { return summary; }
    public GraphicsMode getGraphicsMode() { return graphicsMode; }
    public ParticleQuality getParticlesMode() { return particlesMode; }
    public CloudRenderMode getCloudRenderMode() { return cloudRenderMode; }
    public int getViewDistance() { return viewDistance; }
    public int getSimulationDistance() { return simulationDistance; }
    public int getBiomeBlendRadius() { return biomeBlendRadius; }
    public int getMipmapLevels() { return mipmapLevels; }
    public double getEntityDistanceScaling() { return entityDistanceScaling; }
    public boolean isEntityShadows() { return entityShadows; }
    public boolean isAmbientOcclusion() { return ambientOcclusion; }
    public boolean isVignette() { return vignette; }
    public boolean isVsync() { return vsync; }
    public int getMaxFps() { return maxFps; }
    public int getMaxParticlesPerTick() { return maxParticlesPerTick; }
    public double getMaxParticleDistanceBlocks() { return maxParticleDistanceBlocks; }

    public PerformanceProfile next() {
        PerformanceProfile[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
