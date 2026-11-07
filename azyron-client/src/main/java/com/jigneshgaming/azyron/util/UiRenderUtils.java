package com.jigneshgaming.azyron.util;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

/**
 * Hardware-safe UI rendering helpers built strictly on Minecraft 1.21.11 DrawContext.
 * Fully compatible with PojavLauncher mobile renderers (LTW, Holy GL4ES, Zink, Turnip) and Sodium 0.8+.
 */
public final class UiRenderUtils {
    public static final Identifier LOGO_TEXTURE = Identifier.of("azyron", "textures/gui/logo.png");

    private UiRenderUtils() {
    }

    public static void drawRoundedPanel(DrawContext ctx, int x, int y, int width, int height, int bgColor, int borderColor) {
        if (width <= 2 || height <= 2) {
            return;
        }
        // Corner-cut sleek dark panel
        ctx.fill(x + 1, y, x + width - 1, y + height, bgColor);
        ctx.fill(x, y + 1, x + 1, y + height - 1, bgColor);
        ctx.fill(x + width - 1, y + 1, x + width, y + height - 1, bgColor);

        // Crisp 1px border with chamfered corners
        ctx.fill(x + 1, y, x + width - 1, y + 1, borderColor);
        ctx.fill(x + 1, y + height - 1, x + width - 1, y + height, borderColor);
        ctx.fill(x, y + 1, x + 1, y + height - 1, borderColor);
        ctx.fill(x + width - 1, y + 1, x + width, y + height - 1, borderColor);
    }

    public static void drawAccentCard(DrawContext ctx, int x, int y, int width, int height, int bgColor, boolean hovered, boolean active) {
        drawRoundedPanel(
                ctx,
                x,
                y,
                width,
                height,
                bgColor,
                active ? AzyronTheme.ACCENT_ELECTRIC_BLUE : (hovered ? AzyronTheme.ACCENT_PURPLE_BRIGHT : AzyronTheme.BORDER_SUBTLE)
        );
        // Top dual-accent highlight bar (Purple -> Electric Blue)
        if (active || hovered) {
            int barAlpha = active ? 245 : 175;
            drawHorizontalGradient(
                    ctx,
                    x + 1,
                    y,
                    width - 2,
                    2,
                    AzyronTheme.withAlpha(AzyronTheme.ACCENT_PURPLE_BRIGHT, barAlpha),
                    AzyronTheme.withAlpha(AzyronTheme.ACCENT_ELECTRIC_BLUE, barAlpha)
            );
        }
    }

    public static void drawHudCard(DrawContext ctx, int x, int y, int width, int height, boolean showBg, int bgAlpha, int accentColor) {
        if (!showBg) {
            return;
        }
        int bg = AzyronTheme.withAlpha(AzyronTheme.BG_DARK, bgAlpha);
        int border = AzyronTheme.withAlpha(accentColor, Math.min(200, bgAlpha + 30));
        drawRoundedPanel(ctx, x, y, width, height, bg, border);
        // Left 2px accent strip
        ctx.fill(x, y + 2, x + 2, y + height - 2, accentColor);
    }

    public static void drawHorizontalGradient(DrawContext ctx, int x, int y, int width, int height, int leftColor, int rightColor) {
        if (width <= 0 || height <= 0) {
            return;
        }
        int steps = Math.min(width, 24);
        float stepW = (float) width / steps;
        for (int i = 0; i < steps; i++) {
            int sx = x + Math.round(i * stepW);
            int ex = (i == steps - 1) ? (x + width) : (x + Math.round((i + 1) * stepW));
            if (ex > sx) {
                float t = (steps <= 1) ? 0.0f : (float) i / (steps - 1);
                ctx.fill(sx, y, ex, y + height, AzyronTheme.lerpColor(leftColor, rightColor, t));
            }
        }
    }

    public static void drawToggleSwitch(DrawContext ctx, int x, int y, int width, int height, boolean enabled, boolean hovered) {
        int trackColor = enabled
                ? AzyronTheme.lerpColor(AzyronTheme.ACCENT_PURPLE, AzyronTheme.ACCENT_ELECTRIC_BLUE, hovered ? 0.7f : 0.45f)
                : (hovered ? 0xFF2D3555 : 0xFF1E243B);
        int border = enabled ? AzyronTheme.ACCENT_ELECTRIC_BLUE : AzyronTheme.BORDER_SUBTLE;
        drawRoundedPanel(ctx, x, y, width, height, trackColor, border);

        int knobSize = height - 4;
        int knobX = enabled ? (x + width - knobSize - 2) : (x + 2);
        int knobY = y + 2;
        ctx.fill(knobX, knobY, knobX + knobSize, knobY + knobSize, AzyronTheme.TEXT_PRIMARY);
    }

    public static void drawSlider(DrawContext ctx, int x, int y, int width, int height, float normalizedValue, boolean hovered) {
        float clamped = AzyronTheme.clamp01(normalizedValue);
        drawRoundedPanel(ctx, x, y, width, height, 0xFF13172B, hovered ? AzyronTheme.ACCENT_PURPLE_BRIGHT : AzyronTheme.BORDER_SUBTLE);
        int fillW = Math.max(2, Math.round((width - 2) * clamped));
        drawHorizontalGradient(
                ctx,
                x + 1,
                y + 1,
                fillW,
                height - 2,
                AzyronTheme.ACCENT_PURPLE_BRIGHT,
                AzyronTheme.ACCENT_ELECTRIC_BLUE
        );
        int knobX = Math.max(x + 1, Math.min(x + width - 4, x + fillW - 1));
        ctx.fill(knobX, y - 1, knobX + 3, y + height + 1, AzyronTheme.TEXT_PRIMARY);
    }

    /**
     * Draws the crisp vector AZYRON crest (works even before resource packs mount on frame 0 of SplashOverlay).
     */
    public static void drawAzyronEmblem(DrawContext ctx, int centerX, int centerY, int radius, int alpha) {
        int bg = AzyronTheme.withAlpha(0x0B0E1E, alpha);
        int purple = AzyronTheme.withAlpha(AzyronTheme.ACCENT_PURPLE_BRIGHT, alpha);
        int cyan = AzyronTheme.withAlpha(AzyronTheme.ACCENT_ELECTRIC_BLUE, alpha);
        int white = AzyronTheme.withAlpha(0xE0F7FF, alpha);

        int left = centerX - radius;
        int top = centerY - radius;
        int size = radius * 2;
        drawRoundedPanel(ctx, left, top, size, size, bg, purple);
        drawHorizontalGradient(ctx, left + 2, top, size - 4, 2, purple, cyan);
        drawHorizontalGradient(ctx, left + 2, top + size - 2, size - 4, 2, cyan, purple);

        // Stylized "A" legs + lightning bolt core
        int steps = Math.max(8, radius);
        for (int i = 0; i < steps; i++) {
            float t = (float) i / (steps - 1);
            int py = (centerY - radius + 5) + Math.round(t * (size - 10));
            int spread = Math.round(t * (radius - 5));
            int legCol = AzyronTheme.lerpColor(purple, cyan, t);
            ctx.fill(centerX - spread - 1, py, centerX - spread + 2, py + 2, legCol);
            ctx.fill(centerX + spread - 2, py, centerX + spread + 1, py + 2, legCol);
        }
        // Lightning crossbar
        ctx.fill(centerX - radius / 2, centerY + 1, centerX + 2, centerY + 3, cyan);
        ctx.fill(centerX - 2, centerY - 3, centerX + radius / 2, centerY - 1, white);
        ctx.fill(centerX - 1, centerY - 5, centerX + 2, centerY + 5, white);
    }

    public static void drawLogoTexture(DrawContext ctx, int x, int y, int size) {
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO_TEXTURE, x, y, 0.0f, 0.0f, size, size, size, size);
    }

    public static void drawScaledText(DrawContext ctx, TextRenderer font, String text, int x, int y, float scale, int color, boolean centered) {
        Matrix3x2fStack matrices = ctx.getMatrices();
        matrices.pushMatrix();
        matrices.translate(x, y);
        matrices.scale(scale, scale);
        if (centered) {
            ctx.drawCenteredTextWithShadow(font, text, 0, 0, color);
        } else {
            ctx.drawTextWithShadow(font, text, 0, 0, color);
        }
        matrices.popMatrix();
    }

    public static boolean isHovered(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
