package com.jigneshgaming.azyron.module;

import com.jigneshgaming.azyron.util.AzyronTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix3x2fStack;

/**
 * Base class for draggable, scalable HUD overlay widgets in AZYRON CLIENT.
 * Uses normalized [0..1] screen anchors so positions adapt cleanly to both PC monitors and PojavLauncher screens.
 */
public abstract class HudModule extends AzyronModule {
    private final float defaultAnchorX;
    private final float defaultAnchorY;
    private float anchorX;
    private float anchorY;
    private float scale = 1.0f;
    protected int width = 76;
    protected int height = 20;

    protected final Setting.BooleanSetting showBackground;
    protected final Setting.NumberSetting bgOpacity;
    protected final Setting.ModeSetting accentMode;

    protected HudModule(
            String id,
            String name,
            String description,
            boolean defaultEnabled,
            float defaultAnchorX,
            float defaultAnchorY
    ) {
        super(id, name, description, ModuleCategory.HUD, defaultEnabled);
        this.defaultAnchorX = defaultAnchorX;
        this.defaultAnchorY = defaultAnchorY;
        this.anchorX = defaultAnchorX;
        this.anchorY = defaultAnchorY;

        this.showBackground = addSetting(new Setting.BooleanSetting(
                "show_background", "Show Background", "Draw sleek obsidian HUD card behind text", true
        ));
        this.bgOpacity = addSetting(new Setting.NumberSetting(
                "bg_opacity", "Card Opacity", "Opacity of the HUD card background", 170, 40, 245, 5
        ));
        this.accentMode = addSetting(new Setting.ModeSetting(
                "accent_color", "Accent Color", "Accent color of border & highlights",
                "Chroma", "Chroma", "Electric Blue", "Purple", "Emerald", "Crimson", "White"
        ));
    }

    public float getAnchorX() { return anchorX; }
    public float getAnchorY() { return anchorY; }

    public void setAnchor(float x, float y) {
        this.anchorX = Math.max(0.0f, Math.min(1.0f, x));
        this.anchorY = Math.max(0.0f, Math.min(1.0f, y));
    }

    public float getScale() { return scale; }

    public void setScale(float scale) {
        this.scale = Math.max(0.5f, Math.min(2.0f, scale));
    }

    public int getWidth() { return width; }
    public int getHeight() { return height; }

    public int getScaledWidth() {
        return Math.max(8, Math.round(width * scale));
    }

    public int getScaledHeight() {
        return Math.max(8, Math.round(height * scale));
    }

    public int getScreenX(int screenWidth) {
        int maxX = Math.max(2, screenWidth - getScaledWidth() - 2);
        return Math.max(2, Math.min(maxX, Math.round(anchorX * maxX)));
    }

    public int getScreenY(int screenHeight) {
        int maxY = Math.max(2, screenHeight - getScaledHeight() - 2);
        return Math.max(2, Math.min(maxY, Math.round(anchorY * maxY)));
    }

    public void setScreenPosition(int px, int py, int screenWidth, int screenHeight) {
        int maxX = Math.max(1, screenWidth - getScaledWidth() - 2);
        int maxY = Math.max(1, screenHeight - getScaledHeight() - 2);
        setAnchor((float) (px - 2) / maxX, (float) (py - 2) / maxY);
    }

    public void resetPosition() {
        this.anchorX = defaultAnchorX;
        this.anchorY = defaultAnchorY;
        this.scale = 1.0f;
    }

    @Override
    public void resetSettings() {
        super.resetSettings();
        resetPosition();
    }

    protected int resolveAccentColor() {
        return AzyronTheme.resolvePresetColor(accentMode.getValue());
    }

    public final void renderOnHud(DrawContext context, MinecraftClient client, boolean editorPreview) {
        int sw = context.getScaledWindowWidth();
        int sh = context.getScaledWindowHeight();
        int px = getScreenX(sw);
        int py = getScreenY(sh);

        Matrix3x2fStack matrices = context.getMatrices();
        matrices.pushMatrix();
        matrices.translate(px, py);
        matrices.scale(scale, scale);
        renderWidget(context, client, editorPreview);
        matrices.popMatrix();
    }

    protected abstract void renderWidget(DrawContext context, MinecraftClient client, boolean editorPreview);
}
