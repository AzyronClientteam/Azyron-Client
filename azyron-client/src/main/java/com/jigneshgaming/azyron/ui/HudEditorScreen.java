package com.jigneshgaming.azyron.ui;

import com.jigneshgaming.azyron.AzyronClient;
import com.jigneshgaming.azyron.module.HudModule;
import com.jigneshgaming.azyron.util.AzyronTheme;
import com.jigneshgaming.azyron.util.UiRenderUtils;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Interactive Drag-and-Drop HUD Editor for AZYRON CLIENT.
 * Supports magnetic center snapping, scroll-wheel scaling (0.5x..2.0x), right-click Mod Settings, and one-click reset.
 */
public final class HudEditorScreen extends Screen {
    private final Screen parent;
    private HudModule draggingModule = null;
    private HudModule selectedModule = null;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private boolean snapGrid = true;
    private boolean snappedCenterX = false;
    private boolean snappedCenterY = false;

    public HudEditorScreen(Screen parent) {
        super(Text.literal("AZYRON HUD Editor"));
        this.parent = parent;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fill(0, 0, this.width, this.height, 0x7A060812);

        // Subtle alignment grid
        if (snapGrid) {
            int gridStep = 24;
            for (int gx = gridStep; gx < this.width; gx += gridStep) {
                ctx.drawVerticalLine(gx, 0, this.height, 0x168B5CF6);
            }
            for (int gy = gridStep; gy < this.height; gy += gridStep) {
                ctx.drawHorizontalLine(0, this.width, gy, 0x1600D4FF);
            }
        }

        // Center snap guide lines
        int midX = this.width / 2;
        int midY = this.height / 2;
        ctx.drawVerticalLine(midX, 0, this.height, snappedCenterX ? AzyronTheme.ACCENT_ELECTRIC_BLUE : 0x3500D4FF);
        ctx.drawHorizontalLine(0, this.width, midY, snappedCenterY ? AzyronTheme.ACCENT_ELECTRIC_BLUE : 0x3500D4FF);

        // Render all enabled HUD widgets with bounding boxes
        List<HudModule> huds = AzyronClient.getInstance().getModuleManager().getHudModules();
        for (HudModule hud : huds) {
            if (!hud.isEnabled()) {
                continue;
            }
            hud.renderOnHud(ctx, this.client, true);

            int hx = hud.getScreenX(this.width);
            int hy = hud.getScreenY(this.height);
            int hw = hud.getScaledWidth();
            int hh = hud.getScaledHeight();

            boolean hovered = UiRenderUtils.isHovered(mouseX, mouseY, hx, hy, hw, hh);
            boolean selected = (hud == selectedModule || hud == draggingModule);
            int outlineCol = selected
                    ? AzyronTheme.ACCENT_ELECTRIC_BLUE
                    : (hovered ? AzyronTheme.ACCENT_PURPLE_BRIGHT : 0x5594A3B8);
            ctx.drawStrokedRectangle(hx - 1, hy - 1, hw + 2, hh + 2, outlineCol);

            if (hovered || selected) {
                String tag = String.format(Locale.ROOT, "%s (%.2fx)", hud.getName(), hud.getScale());
                int tw = this.textRenderer.getWidth(tag) + 8;
                int labelY = hy >= 14 ? (hy - 12) : (hy + hh + 2);
                ctx.fill(hx, labelY, hx + tw, labelY + 11, 0xDE0B0E1D);
                ctx.drawTextWithShadow(this.textRenderer, tag, hx + 4, labelY + 2, AzyronTheme.ACCENT_ELECTRIC_BLUE);
            }
        }

        // Floating Center Control Pill
        int pillW = 286;
        int pillH = 48;
        int pillX = midX - pillW / 2;
        int pillY = this.height / 2 - pillH / 2;

        UiRenderUtils.drawRoundedPanel(ctx, pillX, pillY, pillW, pillH, 0xEE0B0E1D, AzyronTheme.ACCENT_PURPLE_BRIGHT);
        UiRenderUtils.drawHorizontalGradient(ctx, pillX + 2, pillY, pillW - 4, 2, AzyronTheme.ACCENT_PURPLE_BRIGHT, AzyronTheme.ACCENT_ELECTRIC_BLUE);
        ctx.drawCenteredTextWithShadow(
                this.textRenderer,
                "HUD EDITOR  •  Drag to Move  |  Scroll to Scale  |  Right-Click Settings",
                midX, pillY + 7, AzyronTheme.TEXT_SECONDARY
        );

        int btnY = pillY + 23;
        drawActionBtn(ctx, pillX + 10, btnY, 84, 18, snapGrid ? "Snap: ON" : "Snap: OFF", mouseX, mouseY, snapGrid);
        drawActionBtn(ctx, pillX + 101, btnY, 84, 18, "Reset Layout", mouseX, mouseY, false);
        drawActionBtn(ctx, pillX + 192, btnY, 84, 18, "Save & Done", mouseX, mouseY, true);
    }

    private void drawActionBtn(DrawContext ctx, int x, int y, int w, int h, String label, int mx, int my, boolean active) {
        boolean hov = UiRenderUtils.isHovered(mx, my, x, y, w, h);
        UiRenderUtils.drawAccentCard(ctx, x, y, w, h, hov ? AzyronTheme.BG_CARD_HOVER : AzyronTheme.BG_CARD, hov, active);
        ctx.drawCenteredTextWithShadow(this.textRenderer, label, x + w / 2, y + 5, active ? AzyronTheme.ACCENT_ELECTRIC_BLUE : AzyronTheme.TEXT_PRIMARY);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x();
        double my = click.y();
        int btn = click.button();

        int pillW = 286;
        int pillH = 48;
        int pillX = this.width / 2 - pillW / 2;
        int pillY = this.height / 2 - pillH / 2;
        int btnY = pillY + 23;

        if (btn == 0) {
            if (UiRenderUtils.isHovered(mx, my, pillX + 10, btnY, 84, 18)) {
                snapGrid = !snapGrid;
                return true;
            }
            if (UiRenderUtils.isHovered(mx, my, pillX + 101, btnY, 84, 18)) {
                AzyronClient.getInstance().getModuleManager().resetAllHudPositions();
                AzyronClient.getInstance().getConfig().save();
                return true;
            }
            if (UiRenderUtils.isHovered(mx, my, pillX + 192, btnY, 84, 18)) {
                close();
                return true;
            }
        }

        // Check HUD widgets in reverse draw order so top-most widget captures drag
        List<HudModule> huds = AzyronClient.getInstance().getModuleManager().getHudModules();
        for (int i = huds.size() - 1; i >= 0; i--) {
            HudModule hud = huds.get(i);
            if (!hud.isEnabled()) {
                continue;
            }
            int hx = hud.getScreenX(this.width);
            int hy = hud.getScreenY(this.height);
            if (UiRenderUtils.isHovered(mx, my, hx, hy, hud.getScaledWidth(), hud.getScaledHeight())) {
                selectedModule = hud;
                if (btn == 1 && this.client != null) {
                    AzyronModsScreen modsScreen = new AzyronModsScreen(this);
                    modsScreen.openModuleSettings(hud);
                    this.client.setScreen(modsScreen);
                    return true;
                }
                if (btn == 0) {
                    draggingModule = hud;
                    dragOffsetX = (int) Math.round(mx) - hx;
                    dragOffsetY = (int) Math.round(my) - hy;
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (draggingModule != null && click.button() == 0) {
            int newX = (int) Math.round(click.x()) - dragOffsetX;
            int newY = (int) Math.round(click.y()) - dragOffsetY;
            int w = draggingModule.getScaledWidth();
            int h = draggingModule.getScaledHeight();

            snappedCenterX = false;
            snappedCenterY = false;

            if (snapGrid) {
                int centerTargetX = (this.width - w) / 2;
                int centerTargetY = (this.height - h) / 2;
                if (Math.abs(newX - centerTargetX) <= 6) {
                    newX = centerTargetX;
                    snappedCenterX = true;
                } else {
                    newX = Math.round(newX / 4.0f) * 4;
                }
                if (Math.abs(newY - centerTargetY) <= 6) {
                    newY = centerTargetY;
                    snappedCenterY = true;
                } else {
                    newY = Math.round(newY / 4.0f) * 4;
                }
            }

            draggingModule.setScreenPosition(newX, newY, this.width, this.height);
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (draggingModule != null) {
            draggingModule = null;
            snappedCenterX = false;
            snappedCenterY = false;
            AzyronClient.getInstance().getConfig().save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount != 0.0) {
            List<HudModule> huds = AzyronClient.getInstance().getModuleManager().getHudModules();
            for (int i = huds.size() - 1; i >= 0; i--) {
                HudModule hud = huds.get(i);
                if (!hud.isEnabled()) continue;
                int hx = hud.getScreenX(this.width);
                int hy = hud.getScreenY(this.height);
                if (UiRenderUtils.isHovered(mouseX, mouseY, hx, hy, hud.getScaledWidth(), hud.getScaledHeight())) {
                    float step = verticalAmount > 0 ? 0.05f : -0.05f;
                    hud.setScale(hud.getScale() + step);
                    selectedModule = hud;
                    AzyronClient.getInstance().getConfig().save();
                    return true;
                }
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        AzyronClient.getInstance().getConfig().save();
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
