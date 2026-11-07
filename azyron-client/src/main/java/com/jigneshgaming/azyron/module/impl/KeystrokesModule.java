package com.jigneshgaming.azyron.module.impl;

import com.jigneshgaming.azyron.module.HudModule;
import com.jigneshgaming.azyron.module.Setting;
import com.jigneshgaming.azyron.util.AzyronTheme;
import com.jigneshgaming.azyron.util.CpsTracker;
import com.jigneshgaming.azyron.util.UiRenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.GameOptions;

/**
 * Animated Keystrokes HUD (WASD + LMB/RMB with CPS + Spacebar) with smooth press-glow interpolation.
 */
public final class KeystrokesModule extends HudModule {
    private final Setting.BooleanSetting showMouseButtons;
    private final Setting.BooleanSetting showMouseCps;
    private final Setting.BooleanSetting showSpacebar;

    private float wFade, aFade, sFade, dFade, lmbFade, rmbFade, spaceFade;

    public KeystrokesModule() {
        super("keystrokes_hud", "Keystrokes HUD", "Animated WASD, LMB/RMB & Spacebar overlay with press glow", true, 0.012f, 0.34f);
        this.showMouseButtons = addSetting(new Setting.BooleanSetting(
                "show_mouse", "Show LMB / RMB", "Render Left and Right mouse tiles below WASD", true
        ));
        this.showMouseCps = addSetting(new Setting.BooleanSetting(
                "show_mouse_cps", "CPS on Mouse Keys", "Display live CPS inside LMB and RMB tiles", true
        ));
        this.showSpacebar = addSetting(new Setting.BooleanSetting(
                "show_space", "Show Spacebar", "Render jump bar beneath movement keys", true
        ));
    }

    @Override
    protected void renderWidget(DrawContext context, MinecraftClient client, boolean editorPreview) {
        boolean wDown = false, aDown = false, sDown = false, dDown = false, lmbDown = false, rmbDown = false, jumpDown = false;
        if (client != null && client.options != null) {
            GameOptions opts = client.options;
            wDown = opts.forwardKey.isPressed();
            aDown = opts.leftKey.isPressed();
            sDown = opts.backKey.isPressed();
            dDown = opts.rightKey.isPressed();
            jumpDown = opts.jumpKey.isPressed();
            lmbDown = opts.attackKey.isPressed() || (client.mouse != null && client.mouse.wasLeftButtonClicked());
            rmbDown = opts.useKey.isPressed() || (client.mouse != null && client.mouse.wasRightButtonClicked());
        }
        if (editorPreview && !wDown && !lmbDown) {
            wDown = true;
            lmbDown = true;
        }

        wFade = stepFade(wFade, wDown);
        aFade = stepFade(aFade, aDown);
        sFade = stepFade(sFade, sDown);
        dFade = stepFade(dFade, dDown);
        lmbFade = stepFade(lmbFade, lmbDown);
        rmbFade = stepFade(rmbFade, rmbDown);
        spaceFade = stepFade(spaceFade, jumpDown);

        int keySize = 22;
        int gap = 2;
        int totalW = keySize * 3 + gap * 2;
        int curY = 0;

        // Row 1: W centered
        drawKeyTile(context, client, keySize + gap, curY, keySize, keySize, "W", null, wFade);
        curY += keySize + gap;

        // Row 2: A S D
        drawKeyTile(context, client, 0, curY, keySize, keySize, "A", null, aFade);
        drawKeyTile(context, client, keySize + gap, curY, keySize, keySize, "S", null, sFade);
        drawKeyTile(context, client, (keySize + gap) * 2, curY, keySize, keySize, "D", null, dFade);
        curY += keySize + gap;

        // Row 3: LMB & RMB
        if (showMouseButtons.getValue()) {
            int halfW = (totalW - gap) / 2;
            int tileH = showMouseCps.getValue() ? 22 : 18;
            String lSub = showMouseCps.getValue() ? ((editorPreview && CpsTracker.getLeftCps() == 0 ? 12 : CpsTracker.getLeftCps()) + " CPS") : null;
            String rSub = showMouseCps.getValue() ? ((editorPreview && CpsTracker.getRightCps() == 0 ? 8 : CpsTracker.getRightCps()) + " CPS") : null;
            drawKeyTile(context, client, 0, curY, halfW, tileH, "LMB", lSub, lmbFade);
            drawKeyTile(context, client, halfW + gap, curY, totalW - halfW - gap, tileH, "RMB", rSub, rmbFade);
            curY += tileH + gap;
        }

        // Row 4: Spacebar
        if (showSpacebar.getValue()) {
            int spaceH = 12;
            drawSpaceTile(context, 0, curY, totalW, spaceH, spaceFade);
            curY += spaceH;
        } else {
            curY -= gap;
        }

        this.width = totalW;
        this.height = Math.max(46, curY);
    }

    private void drawKeyTile(DrawContext ctx, MinecraftClient client, int x, int y, int w, int h, String label, String subLabel, float fade) {
        int baseBg = AzyronTheme.withAlpha(AzyronTheme.BG_DARK, showBackground.getValue() ? bgOpacity.getIntValue() : 90);
        int activeFill = AzyronTheme.lerpColor(AzyronTheme.ACCENT_PURPLE_BRIGHT, AzyronTheme.ACCENT_ELECTRIC_BLUE, 0.55f);
        int bg = AzyronTheme.lerpColor(baseBg, AzyronTheme.withAlpha(activeFill, 225), fade);
        int border = AzyronTheme.lerpColor(AzyronTheme.BORDER_SUBTLE, AzyronTheme.ACCENT_ELECTRIC_BLUE, fade);

        UiRenderUtils.drawRoundedPanel(ctx, x, y, w, h, bg, border);
        int textColor = fade > 0.55f ? AzyronTheme.BG_DEEPEST : AzyronTheme.TEXT_PRIMARY;
        if (subLabel == null) {
            ctx.drawCenteredTextWithShadow(client.textRenderer, label, x + w / 2, y + (h - 8) / 2, textColor);
        } else {
            ctx.drawCenteredTextWithShadow(client.textRenderer, label, x + w / 2, y + 3, textColor);
            UiRenderUtils.drawScaledText(ctx, client.textRenderer, subLabel, x + w / 2, y + 13, 0.72f, AzyronTheme.TEXT_SECONDARY, true);
        }
    }

    private void drawSpaceTile(DrawContext ctx, int x, int y, int w, int h, float fade) {
        int baseBg = AzyronTheme.withAlpha(AzyronTheme.BG_DARK, showBackground.getValue() ? bgOpacity.getIntValue() : 90);
        int bg = AzyronTheme.lerpColor(baseBg, AzyronTheme.withAlpha(AzyronTheme.ACCENT_ELECTRIC_BLUE, 215), fade);
        int border = AzyronTheme.lerpColor(AzyronTheme.BORDER_SUBTLE, AzyronTheme.ACCENT_ELECTRIC_BLUE, fade);
        UiRenderUtils.drawRoundedPanel(ctx, x, y, w, h, bg, border);
        int barCol = fade > 0.5f ? AzyronTheme.BG_DEEPEST : resolveAccentColor();
        ctx.fill(x + 14, y + h / 2 - 1, x + w - 14, y + h / 2 + 1, barCol);
    }

    private static float stepFade(float current, boolean pressed) {
        float target = pressed ? 1.0f : 0.0f;
        return current + (target - current) * 0.35f;
    }
}
