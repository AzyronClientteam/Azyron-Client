package com.jigneshgaming.azyron.ui;

/**
 * Version-neutral GUI interaction contract implemented by all AZYRON CLIENT screens
 * so mouse clicks, drags, releases, scrolling, and keyboard search work identically
 * across Minecraft 1.20.1 (primitive callbacks) and Minecraft 1.21.11 (Click/KeyInput/CharInput).
 */
public interface AzyronInteractiveScreen {
    default boolean azyronMouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean azyronMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return false;
    }

    default boolean azyronMouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean azyronMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return false;
    }

    default boolean azyronKeyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    default boolean azyronCharTyped(char chr, int modifiers) {
        return false;
    }
}
