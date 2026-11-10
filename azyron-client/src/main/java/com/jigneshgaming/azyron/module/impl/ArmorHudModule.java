package com.jigneshgaming.azyron.module.impl;

import com.jigneshgaming.azyron.module.HudModule;
import com.jigneshgaming.azyron.module.Setting;
import com.jigneshgaming.azyron.util.AzyronTheme;
import com.jigneshgaming.azyron.util.UiRenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

/**
 * Zero-allocation Armor & Equipped Item Durability HUD with color-coded health thresholds
 * and reusable fixed-size slot buffers.
 */
public final class ArmorHudModule extends HudModule {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };
    private static final String[] PREVIEW_ROWS = {
            "Helmet 100%",
            "Chest 100%",
            "Legs 100%",
            "Boots 100%"
    };

    private final Setting.ModeSetting durabilityMode;
    private final Setting.BooleanSetting showMainHand;

    private final ItemStack[] activeStacks = new ItemStack[5];
    private final String[] slotLabels = {"", "", "", "", ""};
    private final int[] slotWidths = new int[5];
    private final int[] slotColors = new int[5];
    private final int[] cachedDamage = {-1, -1, -1, -1, -1};
    private final int[] cachedMaxDamage = {-1, -1, -1, -1, -1};
    private String lastMode = "";

    public ArmorHudModule() {
        super("armor_hud", "Armor HUD", "Displays equipped armor & weapon durability with low-durability alerts", true, 0.91f, 0.62f);
        this.durabilityMode = addSetting(new Setting.ModeSetting(
                "durability_mode", "Durability Format", "Display points, percentage, or icon bar only",
                "Percentage", "Percentage", "Points", "Bar Only"
        ));
        this.showMainHand = addSetting(new Setting.BooleanSetting(
                "show_mainhand", "Show Mainhand", "Include held mainhand item in Armor HUD", true
        ));
    }

    public void clearWorldCache() {
        for (int i = 0; i < 5; i++) {
            activeStacks[i] = null;
            slotLabels[i] = "";
            cachedDamage[i] = -1;
            cachedMaxDamage[i] = -1;
        }
    }

    @Override
    protected void renderWidget(DrawContext context, MinecraftClient client, boolean editorPreview) {
        if (context == null || client == null || client.textRenderer == null) {
            return;
        }
        int count = 0;
        if (client.player != null) {
            if (showMainHand.getValue()) {
                ItemStack held = client.player.getEquippedStack(EquipmentSlot.MAINHAND);
                if (held != null && !held.isEmpty()) {
                    activeStacks[count++] = held;
                }
            }
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                ItemStack stack = client.player.getEquippedStack(slot);
                if (stack != null && !stack.isEmpty()) {
                    activeStacks[count++] = stack;
                }
            }
        }

        if (count == 0) {
            if (!editorPreview) {
                this.width = 76;
                this.height = 76;
                return;
            }
            this.width = 84;
            this.height = 4 * 18 + 6;
            UiRenderUtils.drawHudCard(context, 0, 0, width, height, showBackground.getValue(), bgOpacity.getIntValue(), resolveAccentColor());
            for (int i = 0; i < PREVIEW_ROWS.length; i++) {
                int ry = 4 + i * 18;
                UiRenderUtils.drawRoundedPanel(context, 5, ry, 14, 14, 0xFF1E293B, AzyronTheme.ACCENT_PURPLE_BRIGHT);
                UiRenderUtils.drawText(context, client.textRenderer, PREVIEW_ROWS[i], 23, ry + 3, AzyronTheme.STATUS_GOOD);
            }
            return;
        }

        String mode = durabilityMode.getValue();
        boolean modeChanged = !mode.equals(lastMode);
        if (modeChanged) {
            lastMode = mode;
        }
        boolean showText = !"Bar Only".equalsIgnoreCase(mode);
        boolean pctMode = "Percentage".equalsIgnoreCase(mode);
        int maxTextW = 0;

        for (int i = 0; i < count; i++) {
            ItemStack stack = activeStacks[i];
            if (showText && stack.isDamageable() && stack.getMaxDamage() > 0) {
                int max = stack.getMaxDamage();
                int dmg = stack.getDamage();
                if (modeChanged || dmg != cachedDamage[i] || max != cachedMaxDamage[i]) {
                    cachedDamage[i] = dmg;
                    cachedMaxDamage[i] = max;
                    int rem = Math.max(0, max - dmg);
                    int pct = Math.round((rem * 100.0f) / max);
                    String lbl = pctMode ? (pct + "%") : (rem + "/" + max);
                    slotLabels[i] = lbl;
                    slotWidths[i] = client.textRenderer.getWidth(lbl);
                    slotColors[i] = durabilityColor(pct);
                }
                maxTextW = Math.max(maxTextW, slotWidths[i]);
            } else {
                slotLabels[i] = "";
                slotWidths[i] = 0;
                slotColors[i] = AzyronTheme.TEXT_PRIMARY;
                cachedDamage[i] = -2;
            }
        }

        this.width = showText ? Math.max(58, 28 + maxTextW) : 26;
        this.height = count * 18 + 6;

        UiRenderUtils.drawHudCard(context, 0, 0, width, height, showBackground.getValue(), bgOpacity.getIntValue(), resolveAccentColor());

        for (int i = 0; i < count; i++) {
            ItemStack stack = activeStacks[i];
            int ry = 3 + i * 18;
            try {
                context.drawItem(stack, 5, ry);
                // Minecraft 1.21.11: DrawContext.drawItemBar (method_64860) is private;
                // use public DrawContext.drawStackOverlay (method_51431) for durability bar, stack count & cooldowns.
                context.drawStackOverlay(client.textRenderer, stack, 5, ry);
            } catch (Throwable t) {
                // Log rate-limited diagnostic and draw clean fallback marker if a modded server item model fails
                com.jigneshgaming.azyron.profile.MultiplayerPerformanceController
                        .logRateLimitedWarning("ArmorHud item render fallback", t);
                UiRenderUtils.drawRoundedPanel(context, 5, ry + 1, 14, 14, 0xFF1E293B, slotColors[i]);
            }
            String lbl = slotLabels[i];
            if (!lbl.isEmpty()) {
                UiRenderUtils.drawText(context, client.textRenderer, lbl, 24, ry + 4, slotColors[i]);
            }
            activeStacks[i] = null;
        }
    }

    private static int durabilityColor(int pct) {
        if (pct >= 65) return AzyronTheme.STATUS_GOOD;
        if (pct >= 30) return AzyronTheme.STATUS_WARN;
        return AzyronTheme.STATUS_DANGER;
    }
}
