package com.jigneshgaming.azyron.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.MinecraftClient;

/**
 * Base class for every AZYRON CLIENT feature module.
 */
public abstract class AzyronModule {
    private final String id;
    private final String name;
    private final String description;
    private final ModuleCategory category;
    private final boolean defaultEnabled;
    private boolean enabled;
    private final List<Setting<?>> settings = new ArrayList<>();

    protected AzyronModule(String id, String name, String description, ModuleCategory category, boolean defaultEnabled) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.category = category;
        this.defaultEnabled = defaultEnabled;
        this.enabled = defaultEnabled;
    }

    protected <S extends Setting<?>> S addSetting(S setting) {
        this.settings.add(setting);
        return setting;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ModuleCategory getCategory() { return category; }
    public boolean isEnabled() { return enabled; }
    public boolean isDefaultEnabled() { return defaultEnabled; }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            this.enabled = enabled;
            if (enabled) {
                onEnable();
            } else {
                onDisable();
            }
        }
    }

    public void toggle() {
        setEnabled(!enabled);
    }

    public List<Setting<?>> getSettings() {
        return Collections.unmodifiableList(settings);
    }

    public void resetSettings() {
        this.setEnabled(defaultEnabled);
        for (Setting<?> s : settings) {
            s.reset();
        }
    }

    protected void onEnable() {}
    protected void onDisable() {}

    public void onTick(MinecraftClient client) {}
}
