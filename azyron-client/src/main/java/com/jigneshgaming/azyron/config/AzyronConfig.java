package com.jigneshgaming.azyron.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jigneshgaming.azyron.AzyronClient;
import com.jigneshgaming.azyron.cosmetics.CosmeticsManager;
import com.jigneshgaming.azyron.module.AzyronModule;
import com.jigneshgaming.azyron.module.HudModule;
import com.jigneshgaming.azyron.module.ModuleManager;
import com.jigneshgaming.azyron.module.Setting;
import com.jigneshgaming.azyron.profile.MemoryOptimizer;
import com.jigneshgaming.azyron.profile.PerformanceProfile;
import com.jigneshgaming.azyron.util.McVersionBridge;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Persistent JSON configuration for AZYRON CLIENT stored at config/azyron-client.json.
 * Automatically selects the optimal initial PerformanceProfile for 3GB–8GB RAM devices on first boot
 * and uses multi-launcher-safe atomic file writes.
 */
public final class AzyronConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static final class ModuleSnapshot {
        public boolean enabled = true;
        public float anchorX = 0.02f;
        public float anchorY = 0.02f;
        public float scale = 1.0f;
        public Map<String, String> settings = new LinkedHashMap<>();
    }

    public static final class ConfigData {
        public String configVersion = "1.0.4";
        public String selectedMcVersion = McVersionBridge.getActiveMcVersionString();
        public String activeProfile = MemoryOptimizer.getRecommendedProfileForHardware().name();
        public boolean customMainMenuEnabled = true;
        public boolean customLoadingScreenEnabled = true;
        public boolean autoMemoryOptimization = true;
        public boolean fastLoadingFade = true;
        public boolean snapGridInHudEditor = true;
        public String discordInviteUrl = "https://discord.gg/jigneshgaming";
        public String capeStyle = CosmeticsManager.CapeStyle.AZYRON_NEBULA.name();
        public String haloStyle = CosmeticsManager.HaloStyle.AZYRON_HALO.name();
        public String badgeStyle = CosmeticsManager.BadgeStyle.AZYRON_BOLT.name();
        public Map<String, ModuleSnapshot> modules = new LinkedHashMap<>();
    }

    private final Path configFile;
    private ConfigData data = new ConfigData();

    public AzyronConfig() {
        this(FabricLoader.getInstance().getConfigDir().resolve("azyron-client.json"));
    }

    public AzyronConfig(Path configFile) {
        this.configFile = configFile;
    }

    public Path getConfigFile() {
        return configFile;
    }

    public void load() {
        if (!Files.exists(configFile)) {
            this.data = new ConfigData();
            this.data.activeProfile = MemoryOptimizer.getRecommendedProfileForHardware().name();
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            ConfigData loaded = GSON.fromJson(reader, ConfigData.class);
            if (loaded != null) {
                this.data = loaded;
                if (this.data.modules == null) {
                    this.data.modules = new LinkedHashMap<>();
                }
                if (this.data.activeProfile == null || this.data.activeProfile.isBlank()) {
                    this.data.activeProfile = MemoryOptimizer.getRecommendedProfileForHardware().name();
                }
                if (this.data.selectedMcVersion == null || this.data.selectedMcVersion.isBlank()) {
                    this.data.selectedMcVersion = McVersionBridge.getActiveMcVersionString();
                }
                this.data.configVersion = "1.0.4";
                if (this.data.discordInviteUrl == null || this.data.discordInviteUrl.isBlank()) {
                    this.data.discordInviteUrl = "https://discord.gg/jigneshgaming";
                }
            } else {
                backupCorruptedConfigFile("empty-or-null-json");
                this.data = new ConfigData();
            }
        } catch (Exception e) {
            backupCorruptedConfigFile(e.getClass().getSimpleName());
            AzyronClient.LOGGER.warn("[AZYRON] Backed up malformed config and restored hardware-tuned defaults: {}", e.getMessage());
            this.data = new ConfigData();
        }
    }

    private void backupCorruptedConfigFile(String reason) {
        try {
            if (Files.exists(configFile) && Files.isRegularFile(configFile)) {
                String ts = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
                        .withZone(java.time.ZoneOffset.UTC)
                        .format(java.time.Instant.now());
                Path bak = configFile.resolveSibling(configFile.getFileName().toString() + "." + ts + ".bak");
                Files.copy(configFile, bak, StandardCopyOption.COPY_ATTRIBUTES);
                AzyronClient.LOGGER.warn("[AZYRON] Backed up malformed config ({}) -> {}", reason, bak);
            }
        } catch (Throwable ignored) {
        }
    }

    public void applyToManagers(ModuleManager moduleManager, CosmeticsManager cosmetics) {
        if (cosmetics != null) {
            cosmetics.setActiveCape(parseEnum(CosmeticsManager.CapeStyle.class, data.capeStyle, CosmeticsManager.CapeStyle.AZYRON_NEBULA));
            cosmetics.setActiveHalo(parseEnum(CosmeticsManager.HaloStyle.class, data.haloStyle, CosmeticsManager.HaloStyle.AZYRON_HALO));
            cosmetics.setActiveBadge(parseEnum(CosmeticsManager.BadgeStyle.class, data.badgeStyle, CosmeticsManager.BadgeStyle.AZYRON_BOLT));
        }
        if (moduleManager != null) {
            for (AzyronModule module : moduleManager.getModules()) {
                ModuleSnapshot snap = data.modules.get(module.getId());
                if (snap == null) {
                    continue;
                }
                module.setEnabled(snap.enabled);
                if (module instanceof HudModule hud) {
                    hud.setAnchor(snap.anchorX, snap.anchorY);
                    hud.setScale(snap.scale);
                }
                if (snap.settings != null) {
                    for (Setting<?> setting : module.getSettings()) {
                        String raw = snap.settings.get(setting.getKey());
                        if (raw != null) {
                            setting.deserialize(raw);
                        }
                    }
                }
            }
        }
    }

    public void captureFromManagers(ModuleManager moduleManager, CosmeticsManager cosmetics) {
        if (cosmetics != null) {
            data.capeStyle = cosmetics.getActiveCape().name();
            data.haloStyle = cosmetics.getActiveHalo().name();
            data.badgeStyle = cosmetics.getActiveBadge().name();
        }
        if (moduleManager != null) {
            for (AzyronModule module : moduleManager.getModules()) {
                ModuleSnapshot snap = data.modules.get(module.getId());
                if (snap == null) {
                    snap = new ModuleSnapshot();
                    data.modules.put(module.getId(), snap);
                } else if (snap.settings == null) {
                    snap.settings = new LinkedHashMap<>();
                } else {
                    snap.settings.clear();
                }
                snap.enabled = module.isEnabled();
                if (module instanceof HudModule hud) {
                    snap.anchorX = hud.getAnchorX();
                    snap.anchorY = hud.getAnchorY();
                    snap.scale = hud.getScale();
                }
                for (Setting<?> setting : module.getSettings()) {
                    snap.settings.put(setting.getKey(), setting.serialize());
                }
            }
        }
    }

    public synchronized void save() {
        try {
            AzyronClient inst = AzyronClient.getInstance();
            if (inst != null) {
                captureFromManagers(inst.getModuleManager(), inst.getCosmeticsManager());
            }
            Path parent = configFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tmp = configFile.resolveSibling(configFile.getFileName().toString() + ".tmp");
            try (Writer writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
                GSON.toJson(data, writer);
            }
            try {
                Files.move(tmp, configFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicEx) {
                try {
                    Files.move(tmp, configFile, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException moveEx) {
                    // Fallback for Windows antivirus or Android FUSE file locks
                    try (Writer writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                        GSON.toJson(data, writer);
                    }
                    Files.deleteIfExists(tmp);
                }
            }
        } catch (IOException e) {
            AzyronClient.LOGGER.warn("[AZYRON] Could not save config file {}: {}", configFile, e.getMessage());
        }
    }

    public PerformanceProfile getActiveProfile() {
        return parseEnum(PerformanceProfile.class, data.activeProfile, PerformanceProfile.BALANCED);
    }

    public void setActiveProfile(PerformanceProfile profile) {
        if (profile != null) {
            data.activeProfile = profile.name();
        }
    }

    public boolean isCustomMainMenu() { return data.customMainMenuEnabled; }
    public void setCustomMainMenu(boolean enabled) { data.customMainMenuEnabled = enabled; }
    public boolean isCustomMainMenuEnabled() { return data.customMainMenuEnabled; }
    public void setCustomMainMenuEnabled(boolean enabled) { data.customMainMenuEnabled = enabled; }

    public boolean isCustomLoadingScreen() { return data.customLoadingScreenEnabled; }
    public void setCustomLoadingScreen(boolean enabled) { data.customLoadingScreenEnabled = enabled; }
    public boolean isCustomLoadingScreenEnabled() { return data.customLoadingScreenEnabled; }
    public void setCustomLoadingScreenEnabled(boolean enabled) { data.customLoadingScreenEnabled = enabled; }

    public boolean isAutoMemoryOptimization() { return data.autoMemoryOptimization; }
    public void setAutoMemoryOptimization(boolean enabled) { data.autoMemoryOptimization = enabled; }

    public boolean isFastLoadingFade() { return data.fastLoadingFade; }
    public void setFastLoadingFade(boolean enabled) { data.fastLoadingFade = enabled; }

    public boolean isSnapGridInHudEditor() { return data.snapGridInHudEditor; }
    public void setSnapGridInHudEditor(boolean enabled) { data.snapGridInHudEditor = enabled; }

    public String getDiscordInviteUrl() { return data.discordInviteUrl; }

    public McVersionBridge.SupportedMcVersion getSelectedMcVersion() {
        return McVersionBridge.SupportedMcVersion.fromString(
                data.selectedMcVersion,
                McVersionBridge.getRuntimeMcVersion()
        );
    }

    public void setSelectedMcVersion(McVersionBridge.SupportedMcVersion version) {
        if (version != null) {
            data.selectedMcVersion = version.getVersionId();
        }
    }

    public McVersionBridge.SupportedMcVersion cycleSelectedMcVersion() {
        McVersionBridge.SupportedMcVersion next = getSelectedMcVersion().next();
        setSelectedMcVersion(next);
        return next;
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String raw, E fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return fallback;
        }
    }
}
