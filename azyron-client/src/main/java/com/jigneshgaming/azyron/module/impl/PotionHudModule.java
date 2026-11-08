package com.jigneshgaming.azyron.module.impl;

import com.jigneshgaming.azyron.module.HudModule;
import com.jigneshgaming.azyron.module.Setting;
import com.jigneshgaming.azyron.util.AzyronTheme;
import com.jigneshgaming.azyron.util.UiRenderUtils;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;

/**
 * Active Potion Effects HUD with Roman numeral amplifiers, effect color accents, and expiring warning blink.
 * Caches formatted titles, MM:SS timers, and pixel widths once per second instead of allocating
 * five ArrayLists and Formatter strings on every rendered frame.
 */
public final class PotionHudModule extends HudModule {
    private static final int MAX_TRACKED_EFFECTS = 16;

    private final Setting.BooleanSetting hideVanillaOverlay;
    private final Setting.BooleanSetting blinkExpiring;

    private final StatusEffectInstance[] scratchEffects = new StatusEffectInstance[MAX_TRACKED_EFFECTS];
    private final String[] rowNames = new String[MAX_TRACKED_EFFECTS];
    private final String[] rowTimers = new String[MAX_TRACKED_EFFECTS];
    private final int[] rowTimerWidths = new int[MAX_TRACKED_EFFECTS];
    private final int[] rowColors = new int[MAX_TRACKED_EFFECTS];
    private final boolean[] rowExpiring = new boolean[MAX_TRACKED_EFFECTS];

    private int cachedCount = -1;
    private int lastStateHash = 0;
    private int cachedMaxLineW = 80;

    public PotionHudModule() {
        super("potion_hud", "Potion Effects HUD", "Displays active status effects, amplifiers & countdown timers", true, 0.86f, 0.14f);
        this.hideVanillaOverlay = addSetting(new Setting.BooleanSetting(
                "hide_vanilla", "Hide Vanilla Icons", "Suppress default top-right status effect boxes", false
        ));
        this.blinkExpiring = addSetting(new Setting.BooleanSetting(
                "blink_expiring", "Blink Under 10s", "Pulse timer warning when effect is about to expire", true
        ));
    }

    public boolean shouldHideVanillaOverlay() {
        return isEnabled() && hideVanillaOverlay.getValue();
    }

    @Override
    protected void renderWidget(DrawContext context, MinecraftClient client, boolean editorPreview) {
        Collection<StatusEffectInstance> raw = (client != null && client.player != null)
                ? client.player.getStatusEffects()
                : List.of();

        if (raw == null || raw.isEmpty()) {
            cachedCount = 0;
            if (!editorPreview) {
                this.width = 108;
                this.height = 38;
                return;
            }
            this.width = 114;
            this.height = 34;
            UiRenderUtils.drawHudCard(context, 0, 0, width, height, showBackground.getValue(), bgOpacity.getIntValue(), resolveAccentColor());
            context.fill(6, 6, 9, 14, AzyronTheme.ACCENT_ELECTRIC_BLUE);
            context.drawTextWithShadow(client.textRenderer, "Speed II  01:30", 13, 6, AzyronTheme.TEXT_PRIMARY);
            context.fill(6, 19, 9, 27, AzyronTheme.ACCENT_MAGENTA);
            context.drawTextWithShadow(client.textRenderer, "Strength I  00:45", 13, 19, AzyronTheme.TEXT_PRIMARY);
            return;
        }

        int n = 0;
        int stateHash = 1;
        for (StatusEffectInstance inst : raw) {
            if (inst == null) {
                continue;
            }
            if (n < MAX_TRACKED_EFFECTS) {
                scratchEffects[n++] = inst;
            }
            int sec = inst.isInfinite() ? -1 : (inst.getDuration() / 20);
            stateHash = 31 * stateHash + sec;
            stateHash = 31 * stateHash + inst.getAmplifier();
        }

        if (n != cachedCount || stateHash != lastStateHash) {
            cachedCount = n;
            lastStateHash = stateHash;

            // Simple insertion sort by descending duration (zero Comparator/lambda allocation for small N)
            for (int i = 1; i < n; i++) {
                StatusEffectInstance key = scratchEffects[i];
                int keyDur = key.isInfinite() ? Integer.MAX_VALUE : key.getDuration();
                int j = i - 1;
                while (j >= 0) {
                    int jDur = scratchEffects[j].isInfinite() ? Integer.MAX_VALUE : scratchEffects[j].getDuration();
                    if (jDur >= keyDur) {
                        break;
                    }
                    scratchEffects[j + 1] = scratchEffects[j];
                    j--;
                }
                scratchEffects[j + 1] = key;
            }

            int maxW = 80;
            for (int i = 0; i < n; i++) {
                StatusEffectInstance inst = scratchEffects[i];
                StatusEffect effect = inst.getEffectType() != null ? inst.getEffectType().value() : null;
                String baseName = (effect != null && effect.getName() != null) ? effect.getName().getString() : "Effect";
                String amp = toRoman(inst.getAmplifier() + 1);
                String fullTitle = amp.isEmpty() ? baseName : (baseName + " " + amp);
                String dur = inst.isInfinite() ? "INF" : formatTicks(inst.getDuration());
                int col = (effect != null) ? (0xFF000000 | (effect.getColor() & 0xFFFFFF)) : AzyronTheme.ACCENT_ELECTRIC_BLUE;

                rowNames[i] = fullTitle;
                rowTimers[i] = dur;
                int tw = client.textRenderer.getWidth(dur);
                rowTimerWidths[i] = tw;
                rowColors[i] = col;
                rowExpiring[i] = !inst.isInfinite() && inst.getDuration() <= 200;
                int lineW = client.textRenderer.getWidth(fullTitle) + tw + 28;
                if (lineW > maxW) {
                    maxW = lineW;
                }
            }
            cachedMaxLineW = maxW;
        }
        Arrays.fill(scratchEffects, 0, n, null);

        this.width = cachedMaxLineW;
        this.height = n * 14 + 6;
        UiRenderUtils.drawHudCard(context, 0, 0, width, height, showBackground.getValue(), bgOpacity.getIntValue(), resolveAccentColor());

        boolean blinkPhase = (System.currentTimeMillis() / 300L) % 2L == 0L;
        boolean blinkEnabled = blinkExpiring.getValue();
        for (int i = 0; i < n; i++) {
            int ry = 4 + i * 14;
            context.fill(5, ry, 8, ry + 9, rowColors[i]);
            context.drawTextWithShadow(client.textRenderer, rowNames[i], 12, ry, AzyronTheme.TEXT_PRIMARY);

            int timeCol = (blinkEnabled && rowExpiring[i])
                    ? (blinkPhase ? AzyronTheme.STATUS_DANGER : AzyronTheme.STATUS_WARN)
                    : AzyronTheme.ACCENT_ELECTRIC_BLUE;
            context.drawTextWithShadow(client.textRenderer, rowTimers[i], width - rowTimerWidths[i] - 6, ry, timeCol);
        }
    }

    private static String formatTicks(int ticks) {
        int totalSec = Math.max(0, ticks / 20);
        int mins = totalSec / 60;
        int secs = totalSec % 60;
        char[] buf = new char[5];
        buf[0] = (char) ('0' + (mins / 10) % 10);
        buf[1] = (char) ('0' + (mins % 10));
        buf[2] = ':';
        buf[3] = (char) ('0' + (secs / 10));
        buf[4] = (char) ('0' + (secs % 10));
        return new String(buf);
    }

    private static String toRoman(int level) {
        return switch (level) {
            case 1 -> "";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            default -> level > 5 ? Integer.toString(level) : "";
        };
    }
}
