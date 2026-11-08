package com.jigneshgaming.azyron.util;

import com.jigneshgaming.azyron.ui.AzyronLoadingRenderer;
import com.mojang.blaze3d.shaders.ShaderType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gl.ShaderLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

/**
 * Hardware-safe UI rendering helpers built strictly on Minecraft 1.21.11 DrawContext.
 * Includes cold-boot ShaderLoader readiness verification so AZYRON CLIENT never triggers
 * premature "minecraft:core/rendertype_text" VERTEX/FRAGMENT shader errors during SplashOverlay.
 */
public final class UiRenderUtils {
    public static final Identifier LOGO_TEXTURE = Identifier.of("azyron", "textures/gui/logo.png");
    public static final Identifier CORE_TEXT_SHADER_ID = Identifier.ofVanilla("core/rendertype_text");

    private static ShaderLoader cachedVerifiedShaderLoader = null;

    private UiRenderUtils() {
    }

    /**
     * Returns true only when Minecraft 1.21.11's ShaderLoader has loaded both VERTEX and FRAGMENT
     * sources for "minecraft:core/rendertype_text" and initial cold-boot SplashOverlay reload has finished.
     * Caches verified ShaderLoader identity outside SplashOverlay for O(1) zero-overhead in-game HUD checks.
     */
    public static boolean isCoreTextShaderReady(MinecraftClient client) {
        if (client == null || client.textRenderer == null) {
            return false;
        }
        try {
            boolean splashActive = client.getOverlay() instanceof SplashOverlay;
            if (splashActive || !AzyronLoadingRenderer.isCoreResourcesReady()) {
                cachedVerifiedShaderLoader = null;
                if (!AzyronLoadingRenderer.isCoreResourcesReady()) {
                    return false;
                }
            }

            ShaderLoader shaderLoader = client.getShaderLoader();
            if (shaderLoader != null) {
                if (!splashActive && shaderLoader == cachedVerifiedShaderLoader) {
                    return true;
                }
                String vert = shaderLoader.getSource(CORE_TEXT_SHADER_ID, ShaderType.VERTEX);
                String frag = shaderLoader.getSource(CORE_TEXT_SHADER_ID, ShaderType.FRAGMENT);
                boolean ready = vert != null && !vert.isEmpty() && frag != null && !frag.isEmpty();
                if (ready && !splashActive) {
                    cachedVerifiedShaderLoader = shaderLoader;
                }
                return ready;
            }
        } catch (Throwable ignored) {
            return AzyronLoadingRenderer.isCoreResourcesReady();
        }
        return AzyronLoadingRenderer.isCoreResourcesReady();
    }

    public static void drawRoundedPanel(DrawContext ctx, int x, int y, int width, int height, int bgColor, int borderColor) {
        if (width <= 2 || height <= 2) {
            return;
        }
        ctx.fill(x + 1, y, x + width - 1, y + height, bgColor);
        ctx.fill(x, y + 1, x + 1, y + height - 1, bgColor);
        ctx.fill(x + width - 1, y + 1, x + width, y + height - 1, bgColor);

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
        ctx.fill(x, y + 2, x + 2, y + height - 2, accentColor);
    }

    public static void drawHorizontalGradient(DrawContext ctx, int x, int y, int width, int height, int leftColor, int rightColor) {
        if (width <= 0 || height <= 0) {
            return;
        }
        if (leftColor == rightColor || width <= 3) {
            ctx.fill(x, y, x + width, y + height, leftColor);
            return;
        }
        // Use compact quad count (6 segments for thin accent bars, 10 for wider bars) to cut GUI vertex overhead by 75%
        int maxSegments = height <= 3 ? 6 : 10;
        int steps = Math.min(width, maxSegments);
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

        int steps = Math.max(8, radius);
        for (int i = 0; i < steps; i++) {
            float t = (float) i / (steps - 1);
            int py = (centerY - radius + 5) + Math.round(t * (size - 10));
            int spread = Math.round(t * (radius - 5));
            int legCol = AzyronTheme.lerpColor(purple, cyan, t);
            ctx.fill(centerX - spread - 1, py, centerX - spread + 2, py + 2, legCol);
            ctx.fill(centerX + spread - 2, py, centerX + spread + 1, py + 2, legCol);
        }
        ctx.fill(centerX - radius / 2, centerY + 1, centerX + 2, centerY + 3, cyan);
        ctx.fill(centerX - 2, centerY - 3, centerX + radius / 2, centerY - 1, white);
        ctx.fill(centerX - 1, centerY - 5, centerX + 2, centerY + 5, white);
    }

    public static void drawLogoTexture(DrawContext ctx, int x, int y, int size) {
        try {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO_TEXTURE, x, y, 0.0f, 0.0f, size, size, size, size);
        } catch (Throwable ignored) {
            // Safe fallback if resource manager is mid-reload
        }
    }

    public static void drawScaledText(DrawContext ctx, TextRenderer font, String text, int x, int y, float scale, int color, boolean centered) {
        if (font == null || text == null || text.isEmpty()) {
            return;
        }
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

    /**
     * Cold-boot 5x7 pixel-matrix renderer using strictly DrawContext.fill (RenderPipelines.GUI).
     * Renders crisp boot text during SplashOverlay before ShaderLoader loads minecraft:core/rendertype_text.
     */
    public static void drawCenteredBootPixelText(DrawContext ctx, String text, int centerX, int y, int pixelSize, int argbColor) {
        if (text == null || text.isEmpty() || pixelSize <= 0) {
            return;
        }
        String upper = text.toUpperCase();
        int charAdvance = 6 * pixelSize;
        int totalWidth = (upper.length() * charAdvance) - pixelSize;
        int curX = centerX - (totalWidth / 2);

        for (int i = 0; i < upper.length(); i++) {
            long glyph = glyph5x7(upper.charAt(i));
            if (glyph != 0L) {
                for (int row = 0; row < 7; row++) {
                    int rowBits = (int) ((glyph >>> ((6 - row) * 5)) & 0x1FL);
                    if (rowBits == 0) {
                        continue;
                    }
                    for (int col = 0; col < 5; col++) {
                        if ((rowBits & (1 << (4 - col))) != 0) {
                            int px = curX + col * pixelSize;
                            int py = y + row * pixelSize;
                            ctx.fill(px, py, px + pixelSize, py + pixelSize, argbColor);
                        }
                    }
                }
            }
            curX += charAdvance;
        }
    }

    private static long glyph5x7(char c) {
        return switch (c) {
            case 'A' -> 0b01110_10001_10001_11111_10001_10001_10001L;
            case 'B' -> 0b11110_10001_10001_11110_10001_10001_11110L;
            case 'C' -> 0b01111_10000_10000_10000_10000_10000_01111L;
            case 'D' -> 0b11110_10001_10001_10001_10001_10001_11110L;
            case 'E' -> 0b11111_10000_10000_11110_10000_10000_11111L;
            case 'F' -> 0b11111_10000_10000_11110_10000_10000_10000L;
            case 'G' -> 0b01111_10000_10000_10011_10001_10001_01111L;
            case 'H' -> 0b10001_10001_10001_11111_10001_10001_10001L;
            case 'I' -> 0b01110_00100_00100_00100_00100_00100_01110L;
            case 'J' -> 0b00111_00010_00010_00010_10010_10010_01100L;
            case 'K' -> 0b10001_10010_10100_11000_10100_10010_10001L;
            case 'L' -> 0b10000_10000_10000_10000_10000_10000_11111L;
            case 'M' -> 0b10001_11011_10101_10101_10001_10001_10001L;
            case 'N' -> 0b10001_11001_10101_10011_10001_10001_10001L;
            case 'O' -> 0b01110_10001_10001_10001_10001_10001_01110L;
            case 'P' -> 0b11110_10001_10001_11110_10000_10000_10000L;
            case 'Q' -> 0b01110_10001_10001_10001_10101_10010_01101L;
            case 'R' -> 0b11110_10001_10001_11110_10100_10010_10001L;
            case 'S' -> 0b01111_10000_10000_01110_00001_00001_11110L;
            case 'T' -> 0b11111_00100_00100_00100_00100_00100_00100L;
            case 'U' -> 0b10001_10001_10001_10001_10001_10001_01110L;
            case 'V' -> 0b10001_10001_10001_10001_10001_01010_00100L;
            case 'W' -> 0b10001_10001_10001_10101_10101_11011_10001L;
            case 'X' -> 0b10001_10001_01010_00100_01010_10001_10001L;
            case 'Y' -> 0b10001_10001_01010_00100_00100_00100_00100L;
            case 'Z' -> 0b11111_00001_00010_00100_01000_11111_11111L;
            case '0' -> 0b01110_10001_10011_10101_11001_10001_01110L;
            case '1' -> 0b00100_01100_00100_00100_00100_00100_01110L;
            case '2' -> 0b01110_10001_00001_00110_01000_10000_11111L;
            case '3' -> 0b11110_00001_00001_01110_00001_00001_11110L;
            case '4' -> 0b10010_10010_10010_11111_00010_00010_00010L;
            case '5' -> 0b11111_10000_10000_11110_00001_00001_11110L;
            case '6' -> 0b01111_10000_10000_11110_10001_10001_01110L;
            case '7' -> 0b11111_00001_00010_00100_01000_01000_01000L;
            case '8' -> 0b01110_10001_10001_01110_10001_10001_01110L;
            case '9' -> 0b01110_10001_10001_01111_00001_00001_11110L;
            case '%' -> 0b11001_11010_00100_00100_01011_10011_00000L;
            case '.' -> 0b00000_00000_00000_00000_00000_00100_00100L;
            case ':' -> 0b00000_00100_00100_00000_00100_00100_00000L;
            case '-' -> 0b00000_00000_00000_11111_00000_00000_00000L;
            case '|' -> 0b00100_00100_00100_00100_00100_00100_00100L;
            case '/' -> 0b00001_00010_00100_00100_01000_10000_00000L;
            case '•', '*' -> 0b00000_00000_01110_01110_01110_00000_00000L;
            default -> 0L;
        };
    }

    public static boolean isHovered(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
