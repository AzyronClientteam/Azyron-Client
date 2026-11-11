package com.jigneshgaming.azyron.profile;

import com.jigneshgaming.azyron.AzyronClient;
import java.util.Deque;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.client.gui.screen.ProgressScreen;
import net.minecraft.client.gui.screen.Screen;
import com.jigneshgaming.azyron.util.MinecraftVersionApi;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileEntity;

/**
 * Coordinates multiplayer server-join smoothing, protocol-native chunk batch pacing,
 * nanosecond-budgeted client light updates, and crowded-hub distant entity culling
 * for 3GB–8GB RAM systems without hiding errors or removing gameplay features.
 */
public final class MultiplayerPerformanceController {
    /** 80 client ticks (~4.0 seconds) smooth ramp after joining a server or switching worlds. */
    public static final int JOIN_WARMUP_TICKS = 80;

    private static volatile int joinWarmupTicksRemaining = 0;
    private static volatile long lastWorldJoinTimeMs = 0L;
    private static volatile int worldTransitionCount = 0;
    private static volatile long totalChunkBatchesPaced = 0L;
    private static volatile long totalLightUpdatesRun = 0L;
    private static volatile long totalLightUpdatesDeferred = 0L;
    private static volatile long totalEntitiesCulled = 0L;
    private static long lastErrorLogMs = 0L;

    private MultiplayerPerformanceController() {
    }

    /**
     * Returns true when Minecraft is displaying a connection, reconfiguration, or terrain-loading
     * transition screen during which world/HUD state is still synchronizing.
     */
    public static boolean isTransitionScreen(Screen screen) {
        if (screen == null) {
            return false;
        }
        if (MinecraftVersionApi.isLoadingOrConnectingScreen(screen)
                || screen instanceof ProgressScreen
                || screen instanceof DisconnectedScreen) {
            return true;
        }
        // ReconfiguringScreen (net.minecraft.class_8671) exists in 1.20.2+ / 1.21.11, absent in 1.20.1
        String clsName = screen.getClass().getName();
        return "net.minecraft.class_8671".equals(clsName) || clsName.endsWith(".ReconfiguringScreen");
    }

    /**
     * Records a chunk packet load on Minecraft 1.20.1 (where protocol ChunkBatchSizeCalculator does not exist)
     * so multiplayer chunk burst telemetry and light-update pacing stay synchronized across 1.20.1 & 1.21.11.
     */
    public static void recordChunkLoaded120() {
        totalChunkBatchesPaced++;
    }

    public static boolean isInJoinWarmup() {
        return joinWarmupTicksRemaining > 0;
    }

    public static int getJoinWarmupTicksRemaining() {
        return joinWarmupTicksRemaining;
    }

    /**
     * Returns warmup progress in [0.0 .. 1.0], where 0.0 is the instant of server join
     * and 1.0 is steady-state gameplay after warmup completes.
     */
    public static float getWarmupProgress() {
        int rem = joinWarmupTicksRemaining;
        if (rem <= 0) {
            return 1.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, 1.0f - (rem / (float) JOIN_WARMUP_TICKS)));
    }

    /**
     * Invoked whenever MinecraftClient transitions into a new ClientWorld (login, respawn,
     * BungeeCord/Velocity server switch) or disconnects (newWorld == null).
     */
    public static void onWorldJoinOrSwitch(MinecraftClient client, ClientWorld newWorld) {
        worldTransitionCount++;
        MemoryOptimizer.onWorldTransition();

        if (newWorld == null) {
            joinWarmupTicksRemaining = 0;
            return;
        }

        lastWorldJoinTimeMs = System.currentTimeMillis();
        joinWarmupTicksRemaining = JOIN_WARMUP_TICKS;

        // Ensure view/entity bounds match 3GB–8GB hardware tier right at world entry so
        // ClientInformation requests a safe chunk radius from the server.
        AzyronClient inst = AzyronClient.getInstance();
        if (inst != null && inst.getPerformanceEngine() != null && client != null) {
            boolean autoMem = inst.getConfig() == null || inst.getConfig().isAutoMemoryOptimization();
            if (autoMem) {
                inst.getPerformanceEngine().applyAutoRamCalibration(client);
            }
        }
    }

    public static void onClientTick(MinecraftClient client) {
        if (client == null || client.world == null) {
            joinWarmupTicksRemaining = 0;
            return;
        }
        // Only count down warmup ticks once initial LevelLoadingScreen has cleared so
        // the player gets a full smooth 4-second ramp upon entering the rendered world.
        if (!isTransitionScreen(client.currentScreen) && joinWarmupTicksRemaining > 0) {
            joinWarmupTicksRemaining--;
        }
    }

    /**
     * Adaptively bounds ChunkBatchSizeCalculator.getDesiredChunksPerTick() so multiplayer servers
     * stream chunks at a sustainable rate for 3GB–8GB RAM devices instead of flooding 25–64 chunks/tick.
     */
    public static float paceDesiredChunksPerTick(float vanillaDesired) {
        totalChunkBatchesPaced++;
        float safeVanilla = (Float.isFinite(vanillaDesired) && vanillaDesired > 0.0f) ? vanillaDesired : 2.0f;

        MemoryOptimizer.RamTier tier = MemoryOptimizer.detectRamTier();
        float steadyCap = switch (tier) {
            case TIER_3GB -> 3.5f;
            case TIER_4GB -> 5.5f;
            case TIER_6GB -> 9.0f;
            case TIER_8GB -> 14.0f;
        };

        float warmupStartCap = switch (tier) {
            case TIER_3GB -> 1.5f;
            case TIER_4GB -> 2.5f;
            case TIER_6GB -> 4.0f;
            case TIER_8GB -> 6.0f;
        };

        float progress = getWarmupProgress();
        float cap = warmupStartCap + (steadyCap - warmupStartCap) * progress;

        AzyronClient inst = AzyronClient.getInstance();
        if (inst != null && inst.getPerformanceEngine() != null
                && inst.getPerformanceEngine().getActiveProfile() == PerformanceProfile.POTATO) {
            cap *= 0.85f;
        }

        if (MemoryOptimizer.isCriticalMemoryPressure()) {
            cap = Math.min(cap, switch (tier) {
                case TIER_3GB -> 0.75f;
                case TIER_4GB -> 1.25f;
                case TIER_6GB -> 2.0f;
                case TIER_8GB -> 3.0f;
            });
        } else if (FrameTimeTracker.isExperiencingFrameSpike() || MemoryOptimizer.isHighMemoryPressure()) {
            cap = Math.min(cap, warmupStartCap);
        }

        return Math.max(0.5f, Math.min(safeVanilla, cap));
    }

    /**
     * Drains ClientWorld's queued chunk light updates within a per-frame nanosecond budget.
     * Prevents vanilla's unbudgeted 10..1000+ synchronous light updates from freezing the main thread
     * for multiple seconds during multiplayer server joins and hub teleports.
     */
    public static void runBudgetedChunkLightUpdates(Deque<Runnable> queue, MinecraftClient client) {
        if (queue == null || queue.isEmpty()) {
            return;
        }

        boolean loadingScreen = client != null && MinecraftVersionApi.isLevelLoadingScreen(client.currentScreen);
        MemoryOptimizer.RamTier tier = MemoryOptimizer.detectRamTier();

        long budgetNs;
        int maxUpdatesThisFrame;
        if (loadingScreen) {
            // While LevelLoadingScreen is open, allow a generous 5.5ms/frame (up to 24 chunks)
            // so initial spawn chunks finish fast without blocking Netty KeepAlive packets.
            budgetNs = 5_500_000L;
            maxUpdatesThisFrame = 24;
        } else {
            boolean spikeOrPressure = FrameTimeTracker.isExperiencingFrameSpike()
                    || MemoryOptimizer.isHighMemoryPressure();
            budgetNs = switch (tier) {
                case TIER_3GB -> spikeOrPressure ? 1_000_000L : 1_800_000L;
                case TIER_4GB -> spikeOrPressure ? 1_400_000L : 2_500_000L;
                case TIER_6GB -> spikeOrPressure ? 2_000_000L : 3_500_000L;
                case TIER_8GB -> spikeOrPressure ? 2_500_000L : 4_500_000L;
            };
            maxUpdatesThisFrame = switch (tier) {
                case TIER_3GB -> spikeOrPressure ? 3 : 6;
                case TIER_4GB -> spikeOrPressure ? 4 : 8;
                case TIER_6GB -> spikeOrPressure ? 6 : 12;
                case TIER_8GB -> spikeOrPressure ? 8 : 16;
            };
        }

        // Always execute at least 2 updates per frame so terrain lighting makes guaranteed forward progress
        int minGuaranteed = loadingScreen ? 4 : 2;
        long startNs = System.nanoTime();
        int executed = 0;

        while (executed < maxUpdatesThisFrame) {
            Runnable task = queue.poll();
            if (task == null) {
                break;
            }
            try {
                task.run();
            } catch (Throwable t) {
                logRateLimitedWarning("Chunk light update task error", t);
            }
            executed++;
            totalLightUpdatesRun++;

            if (executed >= minGuaranteed && (System.nanoTime() - startNs) >= budgetNs) {
                break;
            }
        }

        if (!queue.isEmpty()) {
            totalLightUpdatesDeferred += queue.size();
        }
    }

    /**
     * Gameplay-safe distant entity render filter for crowded multiplayer hubs and 3GB–8GB RAM devices.
     * Never hides nearby entities (<=16m), combat projectiles, ridden entities, glowing targets,
     * or nearby players/hostiles.
     */
    public static boolean shouldRenderEntity(
            Entity entity,
            double camX,
            double camY,
            double camZ,
            MinecraftClient client
    ) {
        if (entity == null || client == null) {
            return true;
        }
        if (entity == client.player || entity == client.getCameraEntity()) {
            return true;
        }
        if (entity.hasVehicle() || entity.hasPassengers() || entity.isGlowing()) {
            return true;
        }
        if (entity instanceof ProjectileEntity) {
            return true;
        }

        double dx = entity.getX() - camX;
        double dy = entity.getY() - camY;
        double dz = entity.getZ() - camZ;
        double distSq = dx * dx + dy * dy + dz * dz;

        // Always render every entity within 16 blocks unconditionally
        if (distSq <= 256.0) {
            return true;
        }

        MemoryOptimizer.RamTier tier = MemoryOptimizer.detectRamTier();
        AzyronClient inst = AzyronClient.getInstance();
        PerformanceProfile profile = (inst != null && inst.getPerformanceEngine() != null)
                ? inst.getPerformanceEngine().getActiveProfile()
                : PerformanceProfile.BALANCED;

        boolean potato = profile == PerformanceProfile.POTATO;
        boolean stressed = isInJoinWarmup()
                || FrameTimeTracker.isExperiencingFrameSpike()
                || MemoryOptimizer.isCriticalMemoryPressure();

        double maxDistBlocks;
        if (entity instanceof ExperienceOrbEntity || entity instanceof ItemEntity) {
            maxDistBlocks = switch (tier) {
                case TIER_3GB -> stressed ? 16.0 : 22.0;
                case TIER_4GB -> stressed ? 20.0 : (potato ? 24.0 : 28.0);
                case TIER_6GB -> stressed ? 24.0 : (potato ? 28.0 : 36.0);
                case TIER_8GB -> stressed ? 28.0 : (potato ? 32.0 : 48.0);
            };
        } else if (entity instanceof DisplayEntity || entity instanceof ItemFrameEntity || entity instanceof ArmorStandEntity) {
            // Multiplayer hubs frequently place hundreds of hologram ArmorStands, DisplayEntities & shop ItemFrames
            maxDistBlocks = switch (tier) {
                case TIER_3GB -> stressed ? 20.0 : 28.0;
                case TIER_4GB -> stressed ? 24.0 : (potato ? 28.0 : 36.0);
                case TIER_6GB -> stressed ? 32.0 : (potato ? 36.0 : 48.0);
                case TIER_8GB -> stressed ? 40.0 : (potato ? 44.0 : 64.0);
            };
        } else if (entity instanceof PlayerEntity) {
            maxDistBlocks = switch (tier) {
                case TIER_3GB -> stressed ? 32.0 : (potato ? 44.0 : 52.0);
                case TIER_4GB -> stressed ? 40.0 : (potato ? 52.0 : 64.0);
                case TIER_6GB -> stressed ? 56.0 : 96.0;
                case TIER_8GB -> stressed ? 64.0 : 128.0;
            };
        } else if (entity instanceof HostileEntity) {
            maxDistBlocks = switch (tier) {
                case TIER_3GB -> stressed ? 36.0 : 48.0;
                case TIER_4GB -> stressed ? 44.0 : 60.0;
                case TIER_6GB -> stressed ? 56.0 : 80.0;
                case TIER_8GB -> stressed ? 64.0 : 96.0;
            };
        } else {
            // Passive / ambient mobs & misc non-combat entities
            maxDistBlocks = switch (tier) {
                case TIER_3GB -> stressed ? 22.0 : 32.0;
                case TIER_4GB -> stressed ? 28.0 : (potato ? 34.0 : 44.0);
                case TIER_6GB -> stressed ? 36.0 : (potato ? 42.0 : 56.0);
                case TIER_8GB -> stressed ? 44.0 : (potato ? 48.0 : 72.0);
            };
        }

        if (distSq > maxDistBlocks * maxDistBlocks) {
            totalEntitiesCulled++;
            return false;
        }
        return true;
    }

    public static void logRateLimitedWarning(String context, Throwable t) {
        long now = System.currentTimeMillis();
        if (now - lastErrorLogMs >= 10_000L) {
            lastErrorLogMs = now;
            AzyronClient.LOGGER.warn("[AZYRON] {}: {}", context, t != null ? t.toString() : "unknown");
        }
    }

    public static int getWorldTransitionCount() {
        return worldTransitionCount;
    }

    public static long getLastWorldJoinTimeMs() {
        return lastWorldJoinTimeMs;
    }

    public static long getTotalChunkBatchesPaced() {
        return totalChunkBatchesPaced;
    }

    public static long getTotalLightUpdatesRun() {
        return totalLightUpdatesRun;
    }

    public static long getTotalLightUpdatesDeferred() {
        return totalLightUpdatesDeferred;
    }

    public static long getTotalEntitiesCulled() {
        return totalEntitiesCulled;
    }

    public static void resetCountersForTesting() {
        joinWarmupTicksRemaining = 0;
        worldTransitionCount = 0;
        totalChunkBatchesPaced = 0L;
        totalLightUpdatesRun = 0L;
        totalLightUpdatesDeferred = 0L;
        totalEntitiesCulled = 0L;
        lastErrorLogMs = 0L;
    }
}
