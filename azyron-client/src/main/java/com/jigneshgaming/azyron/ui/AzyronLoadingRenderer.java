package com.jigneshgaming.azyron.ui;

import com.jigneshgaming.azyron.AzyronClient;
import com.jigneshgaming.azyron.util.AzyronTheme;
import com.jigneshgaming.azyron.util.PlatformCompat;
import com.jigneshgaming.azyron.util.UiRenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

/**
 * Lightweight, hardware-safe AZYRON CLIENT loading screen renderer.
 * During cold-boot SplashOverlay (before ShaderLoader compiles minecraft:core/rendertype_text),
 * renders crisp pixel-matrix vector text via RenderPipelines.GUI (ctx.fill) so zero shader errors occur,
 * and seamlessly transitions to native TextRenderer once core shaders finish loading.
 */
public final class AzyronLoadingRenderer {
    private static float smoothedProgress = 0.0f;
    private static volatile boolean coreResourcesReady = true;

    private AzyronLoadingRenderer() {
    }

    public static boolean isCoreResourcesReady() {
        return coreResourcesReady;
    }

    public static void setCoreResourcesReady(boolean ready) {
        coreResourcesReady = ready;
    }

    public static void render(DrawContext ctx, MinecraftClient client, float rawProgress, float fadeOutAlpha) {
        render(ctx, client, rawProgress, fadeOutAlpha, UiRenderUtils.isCoreTextShaderReady(client));
    }

    public static void render(
            DrawContext ctx,
            MinecraftClient client,
            float rawProgress,
            float fadeOutAlpha,
            boolean textShaderReady
    ) {
        int a = Math.max(0, Math.min(255, Math.round(fadeOutAlpha * 255.0f)));
        if (a <= 4) {
            return;
        }

        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        int cx = w / 2;
        int cy = h / 2;

        float target = Math.max(0.0f, Math.min(1.0f, rawProgress));
        if (target < smoothedProgress && target < 0.05f) {
            smoothedProgress = target;
        } else {
            smoothedProgress += (target - smoothedProgress) * 0.18f;
        }

        if (PlatformCompat.isAndroidOrPojav()) {
            renderMobileLoading(ctx, client, w, h, smoothedProgress, a, textShaderReady);
            return;
        }

        // Clean obsidian dark gradient backdrop
        ctx.fillGradient(
                0, 0, w, h,
                AzyronTheme.withAlpha(0x070812, a),
                AzyronTheme.withAlpha(0x11152C, a)
        );

        // Top & bottom subtle accent strips
        int purple = AzyronTheme.withAlpha(AzyronTheme.ACCENT_PURPLE_BRIGHT, a);
        int cyan = AzyronTheme.withAlpha(AzyronTheme.ACCENT_ELECTRIC_BLUE, a);
        UiRenderUtils.drawHorizontalGradient(ctx, 0, 0, w, 2, purple, cyan);
        UiRenderUtils.drawHorizontalGradient(ctx, 0, h - 2, w, 2, cyan, purple);

        // Animated orbital cyber dots around emblem
        double timeSec = System.currentTimeMillis() / 1000.0;
        int orbitR = 34;
        for (int i = 0; i < 8; i++) {
            double ang = timeSec * 2.8 + (i * Math.PI / 4.0);
            int ox = cx + (int) Math.round(Math.cos(ang) * orbitR);
            int oy = (cy - 36) + (int) Math.round(Math.sin(ang) * orbitR);
            int dotCol = (i % 2 == 0) ? cyan : purple;
            int dotAlpha = (int) (a * (0.35f + 0.65f * ((i + 1) / 8.0f)));
            ctx.fill(ox - 1, oy - 1, ox + 2, oy + 2, AzyronTheme.withAlpha(dotCol, dotAlpha));
        }

        // Center AZYRON Lightning Crest
        UiRenderUtils.drawAzyronEmblem(ctx, cx, cy - 36, 22, a);

        // Sleek progress bar
        int barW = Math.min(260, Math.max(140, w / 2));
        int barH = 6;
        int barX = cx - barW / 2;
        int barY = cy + 44;

        UiRenderUtils.drawRoundedPanel(
                ctx, barX - 1, barY - 1, barW + 2, barH + 2,
                AzyronTheme.withAlpha(0x0B0E1D, a),
                AzyronTheme.withAlpha(0x334155, a)
        );

        int fillW = Math.max(4, Math.round(barW * smoothedProgress));
        UiRenderUtils.drawHorizontalGradient(ctx, barX, barY, fillW, barH, purple, cyan);

        // Moving shimmer pulse inside progress bar
        int pulseW = Math.min(28, fillW);
        if (pulseW > 4) {
            int travel = barX + (int) ((System.currentTimeMillis() / 6L) % Math.max(1, fillW - pulseW + 1));
            ctx.fill(travel, barY, travel + pulseW, barY + barH, AzyronTheme.withAlpha(0xFFFFFF, a / 3));
        }

        int pct = Math.min(100, Math.round(smoothedProgress * 100.0f));
        TextRenderer font = (textShaderReady && client != null) ? client.textRenderer : null;

        if (font != null) {
            UiRenderUtils.drawScaledText(
                    ctx, font, AzyronClient.CLIENT_DISPLAY_NAME, cx, cy - 4, 1.2f,
                    AzyronTheme.withAlpha(AzyronTheme.TEXT_PRIMARY, a), true
            );
            UiRenderUtils.drawCenteredText(
                    ctx, font, "Welcome to " + AzyronClient.CLIENT_DISPLAY_NAME, cx, cy + 14,
                    AzyronTheme.withAlpha(AzyronTheme.ACCENT_ELECTRIC_BLUE, a)
            );
            UiRenderUtils.drawCenteredText(
                    ctx, font, getStageDescription(smoothedProgress), cx, cy + 28,
                    AzyronTheme.withAlpha(AzyronTheme.TEXT_MUTED, a)
            );
            UiRenderUtils.drawCenteredText(
                    ctx, font, pct + "%", cx, barY + 11,
                    AzyronTheme.withAlpha(AzyronTheme.TEXT_SECONDARY, a)
            );
            UiRenderUtils.drawCenteredText(
                    ctx, font, "PERFORMANCE • SMOOTHNESS • STYLE  |  Made by JIGNESH GAMING",
                    cx, h - 16, AzyronTheme.withAlpha(AzyronTheme.TEXT_MUTED, a)
            );
        } else {
            // Cold-boot shader-safe GUI quad text (never invokes minecraft:core/rendertype_text prematurely)
            UiRenderUtils.drawCenteredBootPixelText(
                    ctx, AzyronClient.CLIENT_DISPLAY_NAME, cx, cy - 4, 1,
                    AzyronTheme.withAlpha(AzyronTheme.TEXT_PRIMARY, a)
            );
            UiRenderUtils.drawCenteredBootPixelText(
                    ctx, "WELCOME TO " + AzyronClient.CLIENT_DISPLAY_NAME, cx, cy + 16, 1,
                    AzyronTheme.withAlpha(AzyronTheme.ACCENT_ELECTRIC_BLUE, a)
            );
            UiRenderUtils.drawCenteredBootPixelText(
                    ctx, getStageDescription(smoothedProgress), cx, cy + 28, 1,
                    AzyronTheme.withAlpha(AzyronTheme.TEXT_MUTED, a)
            );
            UiRenderUtils.drawCenteredBootPixelText(
                    ctx, pct + "%", cx, barY + 11, 1,
                    AzyronTheme.withAlpha(AzyronTheme.TEXT_SECONDARY, a)
            );
            UiRenderUtils.drawCenteredBootPixelText(
                    ctx, "PERFORMANCE • SMOOTHNESS • STYLE | JIGNESH GAMING",
                    cx, h - 16, 1, AzyronTheme.withAlpha(AzyronTheme.TEXT_MUTED, a)
            );
        }
    }

    /** Android path: no orbit/shimmer animation, textures or per-pixel footer text.
     * Does not alter resource-reload ordering, GL capabilities or launcher worker pools.
     */
    private static void renderMobileLoading(DrawContext ctx, MinecraftClient client,
            int w, int h, float progress, int alpha, boolean textReady) {
        int cx = w / 2, cy = h / 2;
        int barW = Math.min(240, Math.max(80, w - 32));
        int left = cx - barW / 2;
        ctx.fill(0, 0, w, h, AzyronTheme.withAlpha(0x0B0E1D, alpha));
        ctx.fill(left, cy + 12, left + barW, cy + 18, AzyronTheme.withAlpha(0x334155, alpha));
        ctx.fill(left, cy + 12, left + Math.round(barW * progress), cy + 18,
                AzyronTheme.withAlpha(AzyronTheme.ACCENT_ELECTRIC_BLUE, alpha));
        int color = AzyronTheme.withAlpha(AzyronTheme.TEXT_PRIMARY, alpha);
        if (textReady && client != null && client.textRenderer != null) {
            UiRenderUtils.drawCenteredText(ctx, client.textRenderer, AzyronClient.CLIENT_DISPLAY_NAME, cx, cy - 12, color);
        } else {
            UiRenderUtils.drawCenteredBootPixelText(ctx, AzyronClient.CLIENT_DISPLAY_NAME, cx, cy - 12, 1, color);
        }
    }

    private static String getStageDescription(float progress) {
        if (progress < 0.25f) return "Initializing " + AzyronClient.CLIENT_DISPLAY_NAME + "...";
        if (progress < 0.55f) return "Optimizing Render Pipelines & Memory...";
        if (progress < 0.85f) return "Loading Client Modules, HUD & Textures...";
        return "Finalizing Client Setup...";
    }
}
