package com.jigneshgaming.azyron.module.impl;

import com.jigneshgaming.azyron.module.HudModule;
import com.jigneshgaming.azyron.module.Setting;
import com.jigneshgaming.azyron.util.AzyronTheme;
import com.jigneshgaming.azyron.util.UiRenderUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

/**
 * Armor & Equipped Item Durability HUD with color-coded health thresholds and HUD Editor preview.
 */
public final class ArmorHudModule extends HudModule {
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD,
            EquipmentSlot.CHEST,
            EquipmentSlot.LEGS,
            EquipmentSlot.FEET
    };
    private static final String[] PREVIEW_NAMES = {"Helmet", "Chest", "Legs", "Boots"};

    private final Setting.ModeSetting durabilityMode;
    private final Setting.BooleanSetting showMainHand;

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

    @Override
    protected void renderWidget(DrawContext context, MinecraftClient client, boolean editorPreview) {
        List<ItemStack> stacks = new ArrayList<>(5);
        if (client != null && client.player != null) {
            if (showMainHand.getValue()) {
                ItemStack held = client.player.getEquippedStack(EquipmentSlot.MAINHAND);
                if (held != null && !held.isEmpty()) {
                    stacks.add(held);
                }
            }
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                ItemStack stack = client.player.getEquippedStack(slot);
                if (stack != null && !stack.isEmpty()) {
                    stacks.add(stack);
                }
            }
        }

        if (stacks.isEmpty()) {
            if (!editorPreview) {
                this.width = 76;
                this.height = 76;
                return;
            }
            // Clean HUD Editor preview when player has no armor equipped
            this.width = 84;
            this.height = 4 * 18 + 6;
            UiRenderUtils.drawHudCard(context, 0, 0, width, height, showBackground.getValue(), bgOpacity.getIntValue(), resolveAccentColor());
            for (int i = 0; i < PREVIEW_NAMES.length; i++) {
                int ry = 4 + i * 18;
                UiRenderUtils.drawRoundedPanel(context, 5, ry, 14, 14, 0xFF1E293B, AzyronTheme.ACCENT_PURPLE_BRIGHT);
                context.drawTextWithShadow(client.textRenderer, PREVIEW_NAMES[i] + " 100%", 23, ry + 3, AzyronTheme.STATUS_GOOD);
            }
            return;
        }

        boolean showText = !"Bar Only".equalsIgnoreCase(durabilityMode.getValue());
        int maxTextW = 0;
        List<String> labels = new ArrayList<>(stacks.size());
        List<Integer> colors = new ArrayList<>(stacks.size());

        for (ItemStack stack : stacks) {
            if (showText && stack.isDamageable() && stack.getMaxDamage() > 0) {
                int max = stack.getMaxDamage();
                int rem = Math.max(0, max - stack.getDamage());
                int pct = Math.round((rem * 100.0f) / max);
                String lbl = "Percentage".equalsIgnoreCase(durabilityMode.getValue())
                        ? (pct + "%")
                        : (rem + "/" + max);
                labels.add(lbl);
                colors.add(durabilityColor(pct));
                maxTextW = Math.max(maxTextW, client.textRenderer.getWidth(lbl));
            } else {
                labels.add("");
                colors.add(AzyronTheme.TEXT_PRIMARY);
            }
        }

        this.width = showText ? Math.max(58, 28 + maxTextW) : 26;
        this.height = stacks.size() * 18 + 6;

        UiRenderUtils.drawHudCard(context, 0, 0, width, height, showBackground.getValue(), bgOpacity.getIntValue(), resolveAccentColor());

        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            int ry = 3 + i * 18;
            context.drawItem(stack, 5, ry);
            context.drawItemBar(stack, 5, ry);
            String lbl = labels.get(i);
            if (!lbl.isEmpty()) {
                context.drawTextWithShadow(client.textRenderer, lbl, 24, ry + 4, colors.get(i));
            }
        }
    }

    private static int durabilityColor(int pct) {
        if (pct >= 65) return AzyronTheme.STATUS_GOOD;
        if (pct >= 30) return AzyronTheme.STATUS_WARN;
        return AzyronTheme.STATUS_DANGER;
    }
}
