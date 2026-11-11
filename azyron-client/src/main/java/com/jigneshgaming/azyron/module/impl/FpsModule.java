package com.jigneshgaming.azyron.module.impl;

import com.jigneshgaming.azyron.module.HudModule;
import com.jigneshgaming.azyron.module.Setting;
import com.jigneshgaming.azyron.profile.FrameTimeTracker;
import com.jigneshgaming.azyron.util.AzyronTheme;
import com.jigneshgaming.azyron.util.UiRenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Live FPS Counter HUD with optional 1% Low FPS and frame-time ms readout.
 * Uses dirty-keyed string & width caching to eliminate per-frame Formatter/StringBuilder allocations.
 */
public final class FpsModule extends HudModule {
    private final Setting.BooleanSetting showOnePercentLow;
    private final Setting.BooleanSetting showFrameMs;

    private int lastFps = -1;
    private int lastLow = -1;
    private int lastTenthsMs = -1;
    private boolean lastShowLow = true;
    private boolean lastShowMs = false;
    private String cachedText = "240 FPS";
    private int cachedWidth = 66;

    public FpsModule() {
        super("fps_counter", "FPS Counter", "Displays real-time FPS, 1% lows & frame-time ms", true, 0.012f, 0.025f);
        this.showOnePercentLow = addSetting(new Setting.BooleanSetting(
                "show_1pct_low", "Show 1% Low", "Display 1% low FPS stability metric", true
        ));
        this.showFrameMs = addSetting(new Setting.BooleanSetting(
                "show_frame_ms", "Show Frame Time", "Display latest frame duration in milliseconds", false
        ));
    }

    @Override
    protected void renderWidget(DrawContext context, MinecraftClient client, boolean editorPreview) {
        int fps = (client != null) ? Math.max(0, client.getCurrentFps()) : 240;
        if (editorPreview && fps == 0) {
            fps = 240;
        }
        boolean showLow = showOnePercentLow.getValue();
        boolean showMs = showFrameMs.getValue();
        int low = showLow ? FrameTimeTracker.getOnePercentLowFps(fps) : -1;
        int tenthsMs = showMs ? Math.max(0, Math.round(FrameTimeTracker.getLatestFrameMs() * 10.0f)) : -1;

        if (fps != lastFps || low != lastLow || tenthsMs != lastTenthsMs
                || showLow != lastShowLow || showMs != lastShowMs) {
            lastFps = fps;
            lastLow = low;
            lastTenthsMs = tenthsMs;
            lastShowLow = showLow;
            lastShowMs = showMs;

            StringBuilder sb = new StringBuilder(28);
            sb.append(fps).append(" FPS");
            if (showLow) {
                sb.append(" • ").append(low).append('L');
            }
            if (showMs) {
                sb.append(" (").append(tenthsMs / 10).append('.').append(tenthsMs % 10).append("ms)");
            }
            cachedText = sb.toString();
            cachedWidth = Math.max(66, client.textRenderer.getWidth(cachedText) + 14);
        }

        this.width = cachedWidth;
        this.height = 18;

        int accent = resolveAccentColor();
        UiRenderUtils.drawHudCard(context, 0, 0, width, height, showBackground.getValue(), bgOpacity.getIntValue(), accent);
        UiRenderUtils.drawText(context, client.textRenderer, cachedText, 7, 5, AzyronTheme.TEXT_PRIMARY);
    }
}
