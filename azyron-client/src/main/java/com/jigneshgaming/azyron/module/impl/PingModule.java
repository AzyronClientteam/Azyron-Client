package com.jigneshgaming.azyron.module.impl;

import com.jigneshgaming.azyron.module.HudModule;
import com.jigneshgaming.azyron.module.Setting;
import com.jigneshgaming.azyron.util.AzyronTheme;
import com.jigneshgaming.azyron.util.UiRenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PlayerListEntry;

/**
 * Live Server Ping / Latency HUD with connection quality color-coding.
 */
public final class PingModule extends HudModule {
    private final Setting.BooleanSetting colorByQuality;

    public PingModule() {
        super("ping_display", "Ping Display", "Shows live server latency in milliseconds", true, 0.012f, 0.128f);
        this.colorByQuality = addSetting(new Setting.BooleanSetting(
                "color_quality", "Quality Color", "Color-code ping green, amber, or red by latency", true
        ));
    }

    @Override
    protected void renderWidget(DrawContext context, MinecraftClient client, boolean editorPreview) {
        int pingMs = -1;
        if (client != null && client.player != null) {
            ClientPlayNetworkHandler net = client.getNetworkHandler();
            if (net != null) {
                PlayerListEntry entry = net.getPlayerListEntry(client.player.getUuid());
                if (entry != null) {
                    pingMs = Math.max(0, entry.getLatency());
                }
            }
        }
        String text;
        if (pingMs >= 0) {
            text = pingMs + " ms";
        } else if (editorPreview) {
            pingMs = 24;
            text = "24 ms";
        } else if (client != null && client.isInSingleplayer()) {
            pingMs = 0;
            text = "0 ms (Local)";
        } else {
            text = "-- ms";
        }

        this.width = Math.max(66, client.textRenderer.getWidth("Ping: " + text) + 14);
        this.height = 18;

        int accent = resolveAccentColor();
        UiRenderUtils.drawHudCard(context, 0, 0, width, height, showBackground.getValue(), bgOpacity.getIntValue(), accent);

        int textColor = AzyronTheme.TEXT_PRIMARY;
        if (colorByQuality.getValue() && pingMs >= 0) {
            if (pingMs <= 60) {
                textColor = AzyronTheme.STATUS_GOOD;
            } else if (pingMs <= 140) {
                textColor = AzyronTheme.STATUS_WARN;
            } else {
                textColor = AzyronTheme.STATUS_DANGER;
            }
        }
        context.drawTextWithShadow(client.textRenderer, "Ping: ", 7, 5, AzyronTheme.TEXT_SECONDARY);
        int labelW = client.textRenderer.getWidth("Ping: ");
        context.drawTextWithShadow(client.textRenderer, text, 7 + labelW, 5, textColor);
    }
}
