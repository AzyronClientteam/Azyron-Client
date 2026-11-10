package com.jigneshgaming.azyron;

import com.jigneshgaming.azyron.config.AzyronConfig;
import com.jigneshgaming.azyron.cosmetics.CosmeticsManager;
import com.jigneshgaming.azyron.module.ModuleManager;
import com.jigneshgaming.azyron.module.impl.ToggleSprintModule;
import com.jigneshgaming.azyron.module.impl.ZoomModule;
import com.jigneshgaming.azyron.profile.PerformanceEngine;
import com.jigneshgaming.azyron.profile.PerformanceProfile;
import com.jigneshgaming.azyron.ui.AzyronModsScreen;
import com.jigneshgaming.azyron.ui.HudEditorScreen;
import com.jigneshgaming.azyron.util.McVersionBridge;
import java.util.List;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AZYRON CLIENT – PERFORMANCE. SMOOTHNESS. STYLE.
 * Unified High-FPS Minecraft 1.21.11 & 1.20.1 Fabric Client created by JIGNESH GAMING.
 */
public final class AzyronClient implements ClientModInitializer {
    public static final String MOD_ID = "azyron-client";
    public static final String CLIENT_NAME = "AZYRON CLIENT";
    public static final String CLIENT_VERSION = "1.0.3";
    public static final String TARGET_MC_VERSION = "1.21.11";
    public static final String LEGACY_MC_VERSION = "1.20.1";
    public static final List<String> SUPPORTED_MC_VERSIONS = List.of("1.21.11", "1.20.1");
    public static final String DEVELOPER = "JIGNESH GAMING";

    public static final Logger LOGGER = LoggerFactory.getLogger(CLIENT_NAME);

    private static AzyronClient instance;
    private static boolean keyBindingsRegistered = false;
    private static boolean tickCallbackRegistered = false;

    private AzyronConfig config;
    private CosmeticsManager cosmeticsManager;
    private ModuleManager moduleManager;
    private PerformanceEngine performanceEngine;

    private KeyBinding openMenuKey;
    private KeyBinding hudEditorKey;
    private KeyBinding zoomKey;
    private KeyBinding cycleProfileKey;
    private KeyBinding toggleSprintKey;

    @Override
    public void onInitializeClient() {
        instance = this;
        String activeMc = McVersionBridge.getActiveMcVersionString();
        LOGGER.info("==========================================================");
        LOGGER.info(" Initializing {} v{} for Minecraft {} (Fabric)", CLIENT_NAME, CLIENT_VERSION, activeMc);
        LOGGER.info(" Supported Versions: Minecraft 1.21.11 & 1.20.1 (Fabric)");
        LOGGER.info(" Tagline: AZYRON CLIENT - PERFORMANCE. SMOOTHNESS. STYLE.");
        LOGGER.info(" Created by {}", DEVELOPER);
        LOGGER.info("==========================================================");

        this.cosmeticsManager = new CosmeticsManager();
        this.moduleManager = new ModuleManager();
        this.config = new AzyronConfig();
        this.config.load();
        this.config.applyToManagers(this.moduleManager, this.cosmeticsManager);

        this.performanceEngine = new PerformanceEngine(this.config.getActiveProfile());
        this.performanceEngine.seedCompanionModConfigsIfMissing();
        com.jigneshgaming.azyron.util.PlatformCompat.auditInstalledModCompatibility();
        // load() already writes first-run defaults; avoid a second startup flash write.
        // Normal setting changes still save through the existing config/UI paths.

        registerKeyBindings();
        registerLifecycleCallbacks();

        LOGGER.info("[AZYRON] Loaded {} client modules & active profile '{}'",
                this.moduleManager.getModules().size(),
                this.performanceEngine.getActiveProfile().getDisplayName());
    }

    /**
     * Standalone headless bootstrap helper used by automated unit/integration test suites.
     */
    public static AzyronClient bootstrapHeadlessForTests(java.nio.file.Path tempConfigFile) {
        AzyronClient client = new AzyronClient();
        instance = client;
        client.cosmeticsManager = new CosmeticsManager();
        client.moduleManager = new ModuleManager();
        client.config = new AzyronConfig(tempConfigFile);
        client.config.load();
        client.config.applyToManagers(client.moduleManager, client.cosmeticsManager);
        client.performanceEngine = new PerformanceEngine(client.config.getActiveProfile());
        client.config.save();
        return client;
    }

    private void registerKeyBindings() {
        if (keyBindingsRegistered) {
            return;
        }
        try {
            this.openMenuKey = KeyBindingHelper.registerKeyBinding(McVersionBridge.createKeyBinding(
                    "key.azyron.open_menu",
                    GLFW.GLFW_KEY_RIGHT_SHIFT,
                    "azyron",
                    "controls"
            ));
            this.hudEditorKey = KeyBindingHelper.registerKeyBinding(McVersionBridge.createKeyBinding(
                    "key.azyron.hud_editor",
                    GLFW.GLFW_KEY_H,
                    "azyron",
                    "controls"
            ));
            this.zoomKey = KeyBindingHelper.registerKeyBinding(McVersionBridge.createKeyBinding(
                    "key.azyron.zoom",
                    GLFW.GLFW_KEY_C,
                    "azyron",
                    "controls"
            ));
            this.cycleProfileKey = KeyBindingHelper.registerKeyBinding(McVersionBridge.createKeyBinding(
                    "key.azyron.cycle_profile",
                    GLFW.GLFW_KEY_F8,
                    "azyron",
                    "controls"
            ));
            this.toggleSprintKey = KeyBindingHelper.registerKeyBinding(McVersionBridge.createKeyBinding(
                    "key.azyron.toggle_sprint",
                    GLFW.GLFW_KEY_I,
                    "azyron",
                    "controls"
            ));
            keyBindingsRegistered = true;
        } catch (Throwable t) {
            LOGGER.warn("[AZYRON] Non-fatal warning registering keybindings: {}", t.getMessage());
        }
    }

    private void registerLifecycleCallbacks() {
        if (tickCallbackRegistered) {
            return;
        }
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            AzyronClient active = instance != null ? instance : this;
            active.onEndClientTick(client);
        });
        tickCallbackRegistered = true;
    }

    public void onEndClientTick(MinecraftClient client) {
        if (client == null) {
            return;
        }

        try {
            if (this.performanceEngine != null) {
                this.performanceEngine.onClientTick(client);
            }

            if (this.moduleManager != null) {
                ZoomModule zoom = this.moduleManager.getZoomModule();
                if (zoom != null && this.zoomKey != null) {
                    zoom.setZooming(client.currentScreen == null && this.zoomKey.isPressed());
                }
                this.moduleManager.onTick(client);
            }

            while (this.openMenuKey != null && this.openMenuKey.wasPressed()) {
                if (!(client.currentScreen instanceof AzyronModsScreen)) {
                    client.setScreen(new AzyronModsScreen(client.currentScreen));
                }
            }

            while (this.hudEditorKey != null && this.hudEditorKey.wasPressed()) {
                if (!(client.currentScreen instanceof HudEditorScreen)) {
                    client.setScreen(new HudEditorScreen(client.currentScreen));
                }
            }

            while (this.cycleProfileKey != null && this.cycleProfileKey.wasPressed()) {
                if (this.performanceEngine != null) {
                    PerformanceProfile next = this.performanceEngine.getActiveProfile().next();
                    this.performanceEngine.applyProfile(next, client, true);
                }
            }

            while (this.toggleSprintKey != null && this.toggleSprintKey.wasPressed()) {
                if (this.moduleManager != null) {
                    ToggleSprintModule ts = this.moduleManager.getToggleSprintModule();
                    if (ts != null) {
                        ts.toggleSprintState();
                    }
                }
            }
        } catch (Throwable t) {
            LOGGER.debug("[AZYRON] Guarded tick error: {}", t.getMessage());
        }
    }

    public static AzyronClient getInstance() {
        return instance;
    }

    public AzyronConfig getConfig() {
        return config;
    }

    public CosmeticsManager getCosmeticsManager() {
        return cosmeticsManager;
    }

    public ModuleManager getModuleManager() {
        return moduleManager;
    }

    public PerformanceEngine getPerformanceEngine() {
        return performanceEngine;
    }
}
