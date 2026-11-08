package com.jigneshgaming.azyron.module.impl;

import com.jigneshgaming.azyron.module.HudModule;
import com.jigneshgaming.azyron.module.Setting;
import com.jigneshgaming.azyron.util.AzyronTheme;
import com.jigneshgaming.azyron.util.CpsTracker;
import com.jigneshgaming.azyron.util.UiRenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Real-time CPS (Clicks Per Second) HUD for Left & Right Mouse / PojavLauncher Touch inputs.
 */
public final class CpsModule extends HudModule {
    private final Setting.BooleanSetting showRightCps;
    private final Setting.BooleanSetting showBurstBar;

    public CpsModule() {
        super("cps_counter", "CPS Counter", "Tracks Left & Right Clicks Per Second accurately", true, 0.012f, 0.072f);
        this.showRightCps = addSetting(new Setting.BooleanSetting(
                "show_rmb", "Show Right CPS", "Display both LMB and RMB click rates", true
        ));
        this.showBurstBar = addSetting(new Setting.BooleanSetting(
                "show_burst_bar", "Show Burst Meter", "Render animated click velocity bar", true
        ));
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (client != null && client.options != null && client.currentScreen == null) {
            boolean mouseLeft = client.mouse != null && client.mouse.wasLeftButtonClicked();
            boolean mouseRight = client.mouse != null && client.mouse.wasRightButtonClicked();
            CpsTracker.pollFallbackInputs(
                    mouseLeft,
                    mouseRight,
                    client.options.attackKey.isPressed(),
                    client.options.useKey.isPressed()
            );
        }
    }

    @Override
    protected void renderWidget(DrawContext context, MinecraftClient client, boolean editorPreview) {
        int lmb = CpsTracker.getLeftCps();
        int rmb = CpsTracker.getRightCps();
        if (editorPreview && lmb == 0 && rmb == 0) {
            lmb = 12;
            rmb = 8;
        }
        String text = showRightCps.getValue()
                ? (lmb + " | " + rmb + " CPS")
                : (lmb + " CPS");

        this.width = Math.max(66, client.textRenderer.getWidth(text) + 14);
        this.height = showBurstBar.getValue() ? 21 : 18;

        int accent = resolveAccentColor();
        UiRenderUtils.drawHudCard(context, 0, 0, width, height, showBackground.getValue(), bgOpacity.getIntValue(), accent);
        context.drawTextWithShadow(client.textRenderer, text, 7, 4, AzyronTheme.TEXT_PRIMARY);

        if (showBurstBar.getValue()) {
            float ratio = Math.min(1.0f, Math.max(lmb, rmb) / 20.0f);
            int barW = width - 12;
            context.fill(6, 16, 6 + barW, 18, 0x661E293B);
            int fillW = Math.round(barW * ratio);
            if (fillW > 0) {
                UiRenderUtils.drawHorizontalGradient(context, 6, 16, fillW, 2, AzyronTheme.ACCENT_PURPLE_BRIGHT, AzyronTheme.ACCENT_ELECTRIC_BLUE);
            }
        }
    }
}
