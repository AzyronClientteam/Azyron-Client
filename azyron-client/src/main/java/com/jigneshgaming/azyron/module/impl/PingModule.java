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
 * Live Server Ping / Latency HUD with connection quality color-coding and cached label widths.
 */
public final class PingModule extends HudModule {
    private final Setting.BooleanSetting colorByQuality;

    private int sampledPingMs = -1;
    private boolean hasSampledThisWorld = false;
    private int tickCounter = 0;

    private int lastPingMs = Integer.MIN_VALUE;
    private int lastStateKind = -1;
    private String cachedText = "-- ms";
    private int cachedLabelWidth = 0;
    private int cachedCardWidth = 66;

    public PingModule() {
        super("ping_display", "Ping Display", "Shows live server latency in milliseconds", true, 0.012f, 0.128f);
        this.colorByQuality = addSetting(new Setting.BooleanSetting(
                "color_quality", "Quality Color", "Color-code ping green, amber, or red by latency", true
        ));
    }

    public void clearWorldCache() {
        sampledPingMs = -1;
        hasSampledThisWorld = false;
        tickCounter = 0;
        lastPingMs = Integer.MIN_VALUE;
        lastStateKind = -1;
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (!isEnabled() || client == null || client.player == null || client.world == null) {
            sampledPingMs = -1;
            hasSampledThisWorld = false;
            return;
        }
        tickCounter++;
        // Sample network PlayerListEntry once every 10 ticks (0.5s) instead of per-frame HashMap lookups
        if (!hasSampledThisWorld || tickCounter >= 10) {
            tickCounter = 0;
            sampleLatency(client);
        }
    }

    private void sampleLatency(MinecraftClient client) {
        if (client != null && client.player != null) {
            ClientPlayNetworkHandler net = client.getNetworkHandler();
            if (net != null) {
                PlayerListEntry entry = net.getPlayerListEntry(client.player.getUuid());
                if (entry != null) {
                    sampledPingMs = Math.max(0, entry.getLatency());
                    hasSampledThisWorld = true;
                    return;
                }
            }
        }
        sampledPingMs = -1;
    }

    @Override
    protected void renderWidget(DrawContext context, MinecraftClient client, boolean editorPreview) {
        int pingMs = -1;
        if (client != null && client.player != null && client.world != null) {
            if (!hasSampledThisWorld) {
                sampleLatency(client);
            }
            pingMs = sampledPingMs;
        }

        int stateKind;
        if (pingMs >= 0) {
            stateKind = 0;
        } else if (editorPreview) {
            pingMs = 24;
            stateKind = 1;
        } else if (client != null && client.isInSingleplayer()) {
            pingMs = 0;
            stateKind = 2;
        } else {
            stateKind = 3;
        }

        if (pingMs != lastPingMs || stateKind != lastStateKind || cachedLabelWidth <= 0) {
            lastPingMs = pingMs;
            lastStateKind = stateKind;
            cachedText = switch (stateKind) {
                case 0, 1 -> pingMs + " ms";
                case 2 -> "0 ms (Local)";
                default -> "-- ms";
            };
            cachedLabelWidth = client.textRenderer.getWidth("Ping: ");
            cachedCardWidth = Math.max(66, cachedLabelWidth + client.textRenderer.getWidth(cachedText) + 14);
        }

        this.width = cachedCardWidth;
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
        UiRenderUtils.drawText(context, client.textRenderer, "Ping: ", 7, 5, AzyronTheme.TEXT_SECONDARY);
        UiRenderUtils.drawText(context, client.textRenderer, cachedText, 7 + cachedLabelWidth, 5, textColor);
    }
}
