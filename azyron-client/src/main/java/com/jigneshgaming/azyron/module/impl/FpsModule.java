package com.jigneshgaming.azyron.module.impl;

import com.jigneshgaming.azyron.module.HudModule;
import com.jigneshgaming.azyron.module.Setting;
import com.jigneshgaming.azyron.profile.FrameTimeTracker;
import com.jigneshgaming.azyron.util.AzyronTheme;
import com.jigneshgaming.azyron.util.UiRenderUtils;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Live FPS Counter HUD with optional 1% Low FPS and frame-time ms readout.
 */
public final class FpsModule extends HudModule {
    private final Setting.BooleanSetting showOnePercentLow;
    private final Setting.BooleanSetting showFrameMs;

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
        StringBuilder sb = new StringBuilder();
        sb.append(fps).append(" FPS");
        if (showOnePercentLow.getValue()) {
            int low = FrameTimeTracker.getOnePercentLowFps(fps);
            sb.append(" • ").append(low).append("L");
        }
        if (showFrameMs.getValue()) {
            sb.append(String.format(Locale.ROOT, " (%.1fms)", FrameTimeTracker.getLatestFrameMs()));
        }
        String text = sb.toString();
        this.width = Math.max(66, client.textRenderer.getWidth(text) + 14);
        this.height = 18;

        int accent = resolveAccentColor();
        UiRenderUtils.drawHudCard(context, 0, 0, width, height, showBackground.getValue(), bgOpacity.getIntValue(), accent);
        context.drawTextWithShadow(client.textRenderer, text, 7, 5, AzyronTheme.TEXT_PRIMARY);
    }
}
