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
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AZYRON CLIENT – PERFORMANCE. SMOOTHNESS. STYLE.
 * High-FPS Minecraft 1.21.11 Fabric Client created by JIGNESH GAMING.
 */
public final class AzyronClient implements ClientModInitializer {
    public static final String MOD_ID = "azyron-client";
    public static final String CLIENT_NAME = "AZYRON CLIENT";
    public static final String CLIENT_VERSION = "1.0.2";
    public static final String TARGET_MC_VERSION = "1.21.11";
    public static final String DEVELOPER = "JIGNESH GAMING";

    public static final Logger LOGGER = LoggerFactory.getLogger(CLIENT_NAME);

    private static AzyronClient instance;

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
        LOGGER.info("==========================================================");
        LOGGER.info(" Initializing {} v{} for Minecraft {} (Fabric)", CLIENT_NAME, CLIENT_VERSION, TARGET_MC_VERSION);
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
        this.config.save();

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
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of("azyron", "controls"));

        this.openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.azyron.open_menu",
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                category
        ));
        this.hudEditorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.azyron.hud_editor",
                GLFW.GLFW_KEY_H,
                category
        ));
        this.zoomKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.azyron.zoom",
                GLFW.GLFW_KEY_C,
                category
        ));
        this.cycleProfileKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.azyron.cycle_profile",
                GLFW.GLFW_KEY_F8,
                category
        ));
        this.toggleSprintKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.azyron.toggle_sprint",
                GLFW.GLFW_KEY_I,
                category
        ));
    }

    private void registerLifecycleCallbacks() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);
    }

    public void onEndClientTick(MinecraftClient client) {
        if (client == null) {
            return;
        }

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
