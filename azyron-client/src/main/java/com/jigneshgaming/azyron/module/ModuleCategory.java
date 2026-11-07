package com.jigneshgaming.azyron.module;

public enum ModuleCategory {
    ALL("All Mods"),
    HUD("HUD Widgets"),
    GAMEPLAY("Gameplay"),
    VISUAL("Visuals"),
    PERFORMANCE("Performance");

    private final String label;

    ModuleCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
