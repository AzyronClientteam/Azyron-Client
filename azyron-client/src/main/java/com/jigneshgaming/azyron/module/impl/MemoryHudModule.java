package com.jigneshgaming.azyron.module.impl;

import com.jigneshgaming.azyron.module.HudModule;
import com.jigneshgaming.azyron.module.Setting;
import com.jigneshgaming.azyron.profile.MemoryOptimizer;
import com.jigneshgaming.azyron.util.AzyronTheme;
import com.jigneshgaming.azyron.util.UiRenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Live JVM Memory Optimization HUD displaying RAM percentage, used MB, and allocation rate.
 */
public final class MemoryHudModule extends HudModule {
    private final Setting.BooleanSetting showAllocRate;

    public MemoryHudModule() {
        super("memory_hud", "Memory HUD", "Shows live JVM RAM usage % and allocation rate", false, 0.86f, 0.06f);
        this.showAllocRate = addSetting(new Setting.BooleanSetting(
                "show_alloc", "Show Alloc Rate", "Display MB/s heap allocation rate", true
        ));
    }

    @Override
    protected void renderWidget(DrawContext context, MinecraftClient client, boolean editorPreview) {
        int pct = MemoryOptimizer.getUsagePercent();
        int used = MemoryOptimizer.getUsedMb();
        int alloc = MemoryOptimizer.getAllocationRateMbPerSec();
        String text = "RAM: " + pct + "% (" + used + "MB)"
                + (showAllocRate.getValue() ? (" • " + alloc + "MB/s") : "");

        this.width = Math.max(96, client.textRenderer.getWidth(text) + 14);
        this.height = 18;
        UiRenderUtils.drawHudCard(context, 0, 0, width, height, showBackground.getValue(), bgOpacity.getIntValue(), resolveAccentColor());
        context.drawTextWithShadow(client.textRenderer, text, 7, 5, AzyronTheme.TEXT_PRIMARY);
    }
}
