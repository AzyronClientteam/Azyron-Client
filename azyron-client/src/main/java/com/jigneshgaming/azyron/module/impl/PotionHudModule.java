package com.jigneshgaming.azyron.module.impl;

import com.jigneshgaming.azyron.module.HudModule;
import com.jigneshgaming.azyron.module.Setting;
import com.jigneshgaming.azyron.util.AzyronTheme;
import com.jigneshgaming.azyron.util.UiRenderUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;

/**
 * Active Potion Effects HUD with Roman numeral amplifiers, effect color accents, and expiring warning blink.
 */
public final class PotionHudModule extends HudModule {
    private final Setting.BooleanSetting hideVanillaOverlay;
    private final Setting.BooleanSetting blinkExpiring;

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

        List<StatusEffectInstance> sorted = new ArrayList<>(raw);
        sorted.sort(Comparator.comparingInt(StatusEffectInstance::getDuration).reversed());

        int maxLineW = 80;
        List<String> names = new ArrayList<>(sorted.size());
        List<String> timers = new ArrayList<>(sorted.size());
        List<Integer> effectColors = new ArrayList<>(sorted.size());
        List<Boolean> expiringFlags = new ArrayList<>(sorted.size());

        for (StatusEffectInstance inst : sorted) {
            StatusEffect effect = inst.getEffectType() != null ? inst.getEffectType().value() : null;
            String baseName = (effect != null && effect.getName() != null) ? effect.getName().getString() : "Effect";
            String amp = toRoman(inst.getAmplifier() + 1);
            String fullTitle = amp.isEmpty() ? baseName : (baseName + " " + amp);
            String dur = inst.isInfinite() ? "INF" : formatTicks(inst.getDuration());
            int col = (effect != null) ? (0xFF000000 | (effect.getColor() & 0xFFFFFF)) : AzyronTheme.ACCENT_ELECTRIC_BLUE;

            names.add(fullTitle);
            timers.add(dur);
            effectColors.add(col);
            expiringFlags.add(!inst.isInfinite() && inst.getDuration() <= 200);
            maxLineW = Math.max(maxLineW, client.textRenderer.getWidth(fullTitle + "  " + dur) + 20);
        }

        this.width = maxLineW;
        this.height = sorted.size() * 14 + 6;
        UiRenderUtils.drawHudCard(context, 0, 0, width, height, showBackground.getValue(), bgOpacity.getIntValue(), resolveAccentColor());

        boolean blinkPhase = (System.currentTimeMillis() / 300L) % 2L == 0L;
        for (int i = 0; i < sorted.size(); i++) {
            int ry = 4 + i * 14;
            context.fill(5, ry, 8, ry + 9, effectColors.get(i));
            context.drawTextWithShadow(client.textRenderer, names.get(i), 12, ry, AzyronTheme.TEXT_PRIMARY);

            String t = timers.get(i);
            int tw = client.textRenderer.getWidth(t);
            int timeCol = (blinkExpiring.getValue() && expiringFlags.get(i))
                    ? (blinkPhase ? AzyronTheme.STATUS_DANGER : AzyronTheme.STATUS_WARN)
                    : AzyronTheme.ACCENT_ELECTRIC_BLUE;
            context.drawTextWithShadow(client.textRenderer, t, width - tw - 6, ry, timeCol);
        }
    }

    private static String formatTicks(int ticks) {
        int totalSec = Math.max(0, ticks / 20);
        int mins = totalSec / 60;
        int secs = totalSec % 60;
        return String.format(Locale.ROOT, "%02d:%02d", mins, secs);
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
