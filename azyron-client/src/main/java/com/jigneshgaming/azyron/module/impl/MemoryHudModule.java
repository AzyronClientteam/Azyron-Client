package com.jigneshgaming.azyron.module.impl;

import com.jigneshgaming.azyron.module.HudModule;
import com.jigneshgaming.azyron.module.Setting;
import com.jigneshgaming.azyron.profile.MemoryOptimizer;
import com.jigneshgaming.azyron.util.AzyronTheme;
import com.jigneshgaming.azyron.util.UiRenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Live JVM Memory Optimization HUD displaying RAM percentage, used MB, and allocation rate
 * with zero per-frame String allocations between 250ms sample updates.
 */
public final class MemoryHudModule extends HudModule {
    private final Setting.BooleanSetting showAllocRate;

    private int lastPct = -1;
    private int lastUsed = -1;
    private int lastAlloc = -1;
    private boolean lastShowAlloc = true;
    private String cachedText = "RAM: 0% (0MB)";
    private int cachedWidth = 96;

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
        boolean showAlloc = showAllocRate.getValue();

        if (pct != lastPct || used != lastUsed || alloc != lastAlloc || showAlloc != lastShowAlloc) {
            lastPct = pct;
            lastUsed = used;
            lastAlloc = alloc;
            lastShowAlloc = showAlloc;
            cachedText = showAlloc
                    ? ("RAM: " + pct + "% (" + used + "MB) • " + alloc + "MB/s")
                    : ("RAM: " + pct + "% (" + used + "MB)");
            cachedWidth = Math.max(96, client.textRenderer.getWidth(cachedText) + 14);
        }

        this.width = cachedWidth;
        this.height = 18;
        UiRenderUtils.drawHudCard(context, 0, 0, width, height, showBackground.getValue(), bgOpacity.getIntValue(), resolveAccentColor());
        UiRenderUtils.drawText(context, client.textRenderer, cachedText, 7, 5, AzyronTheme.TEXT_PRIMARY);
    }
}
