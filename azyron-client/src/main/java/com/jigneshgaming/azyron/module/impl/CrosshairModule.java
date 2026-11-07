package com.jigneshgaming.azyron.module.impl;

import com.jigneshgaming.azyron.module.AzyronModule;
import com.jigneshgaming.azyron.module.ModuleCategory;
import com.jigneshgaming.azyron.module.Setting;
import com.jigneshgaming.azyron.util.AzyronTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Competitive Custom Crosshair with 5 styles, dynamic movement expansion, outline & entity-target color shift.
 */
public final class CrosshairModule extends AzyronModule {
    private final Setting.ModeSetting style;
    private final Setting.NumberSetting size;
    private final Setting.NumberSetting gap;
    private final Setting.NumberSetting thickness;
    private final Setting.BooleanSetting centerDot;
    private final Setting.BooleanSetting outline;
    private final Setting.BooleanSetting dynamicGap;
    private final Setting.BooleanSetting targetHighlight;
    private final Setting.ModeSetting colorPreset;

    private float animatedExtraGap = 0.0f;

    public CrosshairModule() {
        super("custom_crosshair", "Custom Crosshair", "Precision PvP crosshair with dynamic gap & entity highlight", ModuleCategory.VISUAL, false);
        this.style = addSetting(new Setting.ModeSetting(
                "style", "Crosshair Style", "Shape of the custom crosshair",
                "Cross", "Cross", "Circle-Cross", "Dot", "Chevron", "Box"
        ));
        this.size = addSetting(new Setting.NumberSetting(
                "size", "Line Length", "Arm length in pixels", 5, 2, 14, 1
        ));
        this.gap = addSetting(new Setting.NumberSetting(
                "gap", "Center Gap", "Distance from center in pixels", 3, 0, 10, 1
        ));
        this.thickness = addSetting(new Setting.NumberSetting(
                "thickness", "Thickness", "Stroke width in pixels", 1, 1, 3, 1
        ));
        this.centerDot = addSetting(new Setting.BooleanSetting(
                "center_dot", "Center Dot", "Render crisp 1px/2px center point", true
        ));
        this.outline = addSetting(new Setting.BooleanSetting(
                "outline", "Dark Outline", "Draw high-contrast 1px dark outline", true
        ));
        this.dynamicGap = addSetting(new Setting.BooleanSetting(
                "dynamic_gap", "Dynamic Movement", "Expand gap slightly when sprinting", true
        ));
        this.targetHighlight = addSetting(new Setting.BooleanSetting(
                "target_highlight", "Target Highlight", "Turn crimson when aiming at an entity", true
        ));
        this.colorPreset = addSetting(new Setting.ModeSetting(
                "color", "Crosshair Color", "Base color when not targeting an entity",
                "Electric Blue", "Electric Blue", "Purple", "Chroma", "Emerald", "White", "Crimson"
        ));
    }

    public void renderCrosshair(DrawContext ctx, MinecraftClient client) {
        int cx = ctx.getScaledWindowWidth() / 2;
        int cy = ctx.getScaledWindowHeight() / 2;

        float targetBoost = 0.0f;
        if (dynamicGap.getValue() && client != null && client.player != null && client.player.isSprinting()) {
            targetBoost = 2.5f;
        }
        animatedExtraGap += (targetBoost - animatedExtraGap) * 0.25f;

        int g = gap.getIntValue() + Math.round(animatedExtraGap);
        int len = size.getIntValue();
        int t = thickness.getIntValue();
        int halfT = t / 2;

        int color = AzyronTheme.resolvePresetColor(colorPreset.getValue());
        if (targetHighlight.getValue() && client != null && client.targetedEntity != null) {
            color = AzyronTheme.STATUS_DANGER;
        }

        boolean drawOutline = outline.getValue();
        String mode = style.getValue();

        if (centerDot.getValue() || "Dot".equalsIgnoreCase(mode)) {
            drawRectWithOutline(ctx, cx - halfT, cy - halfT, cx - halfT + t, cy - halfT + t, color, drawOutline);
        }

        if ("Dot".equalsIgnoreCase(mode)) {
            return;
        }

        if ("Cross".equalsIgnoreCase(mode) || "Circle-Cross".equalsIgnoreCase(mode)) {
            // Top, Bottom, Left, Right arms
            drawRectWithOutline(ctx, cx - halfT, cy - g - len, cx - halfT + t, cy - g, color, drawOutline);
            drawRectWithOutline(ctx, cx - halfT, cy + g + 1, cx - halfT + t, cy + g + 1 + len, color, drawOutline);
            drawRectWithOutline(ctx, cx - g - len, cy - halfT, cx - g, cy - halfT + t, color, drawOutline);
            drawRectWithOutline(ctx, cx + g + 1, cy - halfT, cx + g + 1 + len, cy - halfT + t, color, drawOutline);

            if ("Circle-Cross".equalsIgnoreCase(mode)) {
                int r = g + len / 2 + 1;
                ctx.drawStrokedRectangle(cx - r, cy - r, r * 2 + 1, r * 2 + 1, AzyronTheme.withAlpha(color, 150));
            }
        } else if ("Chevron".equalsIgnoreCase(mode)) {
            for (int i = 0; i <= len; i++) {
                drawRectWithOutline(ctx, cx - i, cy + g + i, cx - i + t, cy + g + i + t, color, drawOutline);
                drawRectWithOutline(ctx, cx + i, cy + g + i, cx + i + t, cy + g + i + t, color, drawOutline);
            }
        } else if ("Box".equalsIgnoreCase(mode)) {
            int r = g + len / 2 + 1;
            if (drawOutline) {
                ctx.drawStrokedRectangle(cx - r - 1, cy - r - 1, (r + 1) * 2 + 1, (r + 1) * 2 + 1, 0xCC000000);
            }
            ctx.drawStrokedRectangle(cx - r, cy - r, r * 2 + 1, r * 2 + 1, color);
        }
    }

    private static void drawRectWithOutline(DrawContext ctx, int x1, int y1, int x2, int y2, int color, boolean outline) {
        if (outline) {
            ctx.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, 0xCC000000);
        }
        ctx.fill(x1, y1, x2, y2, color);
    }
}
