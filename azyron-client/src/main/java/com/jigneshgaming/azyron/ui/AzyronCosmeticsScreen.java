package com.jigneshgaming.azyron.ui;

import com.jigneshgaming.azyron.AzyronClient;
import com.jigneshgaming.azyron.cosmetics.CosmeticsManager;
import com.jigneshgaming.azyron.util.AzyronTheme;
import com.jigneshgaming.azyron.util.UiRenderUtils;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Interactive AZYRON CLIENT Cosmetics Locker for equipping Capes, Animated Halos & Client Badges.
 */
public final class AzyronCosmeticsScreen extends Screen {
    private final Screen parent;
    private int activeTab = 0; // 0 = Capes, 1 = Halos, 2 = Badges

    public AzyronCosmeticsScreen(Screen parent) {
        super(Text.literal("AZYRON Cosmetics"));
        this.parent = parent;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fillGradient(0, 0, this.width, this.height, 0xDF070913, 0xEB0E1226);

        int panelW = Math.min(430, this.width - 24);
        int panelH = Math.min(256, this.height - 24);
        int px = (this.width - panelW) / 2;
        int py = (this.height - panelH) / 2;

        UiRenderUtils.drawRoundedPanel(ctx, px, py, panelW, panelH, AzyronTheme.BG_PANEL, AzyronTheme.ACCENT_PURPLE_BRIGHT);
        UiRenderUtils.drawHorizontalGradient(ctx, px + 2, py, panelW - 4, 2, AzyronTheme.ACCENT_PURPLE_BRIGHT, AzyronTheme.ACCENT_ELECTRIC_BLUE);

        // Header
        ctx.drawTextWithShadow(this.textRenderer, "AZYRON COSMETICS LOCKER", px + 14, py + 10, AzyronTheme.TEXT_PRIMARY);
        String closeLabel = "✕ Back";
        int closeW = 52;
        int closeX = px + panelW - closeW - 10;
        boolean closeHover = UiRenderUtils.isHovered(mouseX, mouseY, closeX, py + 6, closeW, 18);
        UiRenderUtils.drawRoundedPanel(ctx, closeX, py + 6, closeW, 18, closeHover ? AzyronTheme.BG_CARD_HOVER : AzyronTheme.BG_CARD, AzyronTheme.BORDER_SUBTLE);
        ctx.drawCenteredTextWithShadow(this.textRenderer, closeLabel, closeX + closeW / 2, py + 11, AzyronTheme.TEXT_PRIMARY);

        // Tabs
        String[] tabs = {"Cloaks & Capes", "Halos & Auras", "Client Badges"};
        int tabW = 96;
        for (int i = 0; i < tabs.length; i++) {
            int tx = px + 14 + i * (tabW + 6);
            int ty = py + 28;
            boolean hov = UiRenderUtils.isHovered(mouseX, mouseY, tx, ty, tabW, 18);
            UiRenderUtils.drawAccentCard(ctx, tx, ty, tabW, 18, activeTab == i ? AzyronTheme.BG_CARD_ACTIVE : AzyronTheme.BG_CARD, hov, activeTab == i);
            ctx.drawCenteredTextWithShadow(this.textRenderer, tabs[i], tx + tabW / 2, ty + 5, activeTab == i ? AzyronTheme.ACCENT_ELECTRIC_BLUE : AzyronTheme.TEXT_SECONDARY);
        }

        CosmeticsManager cm = AzyronClient.getInstance().getCosmeticsManager();

        // Left: Options list
        int listX = px + 14;
        int listY = py + 54;
        int listW = panelW - 156;

        if (activeTab == 0) {
            CosmeticsManager.CapeStyle[] styles = CosmeticsManager.CapeStyle.values();
            for (int i = 0; i < styles.length; i++) {
                CosmeticsManager.CapeStyle s = styles[i];
                int cy = listY + i * 36;
                boolean eq = cm.getActiveCape() == s;
                boolean hov = UiRenderUtils.isHovered(mouseX, mouseY, listX, cy, listW, 32);
                UiRenderUtils.drawAccentCard(ctx, listX, cy, listW, 32, eq ? AzyronTheme.BG_CARD_ACTIVE : AzyronTheme.BG_CARD, hov, eq);
                UiRenderUtils.drawHorizontalGradient(ctx, listX + 6, cy + 7, 18, 18, s.getPrimaryColor(), s.getSecondaryColor());
                ctx.drawTextWithShadow(this.textRenderer, s.getDisplayName(), listX + 30, cy + 6, AzyronTheme.TEXT_PRIMARY);
                ctx.drawTextWithShadow(this.textRenderer, s.getDescription(), listX + 30, cy + 18, AzyronTheme.TEXT_MUTED);
                if (eq) {
                    ctx.drawTextWithShadow(this.textRenderer, "EQUIPPED", listX + listW - 58, cy + 12, AzyronTheme.STATUS_GOOD);
                }
            }
        } else if (activeTab == 1) {
            CosmeticsManager.HaloStyle[] styles = CosmeticsManager.HaloStyle.values();
            for (int i = 0; i < styles.length; i++) {
                CosmeticsManager.HaloStyle s = styles[i];
                int cy = listY + i * 38;
                boolean eq = cm.getActiveHalo() == s;
                boolean hov = UiRenderUtils.isHovered(mouseX, mouseY, listX, cy, listW, 34);
                UiRenderUtils.drawAccentCard(ctx, listX, cy, listW, 34, eq ? AzyronTheme.BG_CARD_ACTIVE : AzyronTheme.BG_CARD, hov, eq);
                ctx.fill(listX + 8, cy + 9, listX + 22, cy + 23, s.getColor());
                ctx.drawTextWithShadow(this.textRenderer, s.getDisplayName(), listX + 30, cy + 7, AzyronTheme.TEXT_PRIMARY);
                ctx.drawTextWithShadow(this.textRenderer, s.getDescription(), listX + 30, cy + 19, AzyronTheme.TEXT_MUTED);
                if (eq) {
                    ctx.drawTextWithShadow(this.textRenderer, "EQUIPPED", listX + listW - 58, cy + 13, AzyronTheme.STATUS_GOOD);
                }
            }
        } else {
            CosmeticsManager.BadgeStyle[] styles = CosmeticsManager.BadgeStyle.values();
            for (int i = 0; i < styles.length; i++) {
                CosmeticsManager.BadgeStyle s = styles[i];
                int cy = listY + i * 38;
                boolean eq = cm.getActiveBadge() == s;
                boolean hov = UiRenderUtils.isHovered(mouseX, mouseY, listX, cy, listW, 34);
                UiRenderUtils.drawAccentCard(ctx, listX, cy, listW, 34, eq ? AzyronTheme.BG_CARD_ACTIVE : AzyronTheme.BG_CARD, hov, eq);
                String title = s.getTag().isEmpty() ? "None" : s.getTag();
                ctx.drawTextWithShadow(this.textRenderer, title, listX + 10, cy + 7, AzyronTheme.ACCENT_ELECTRIC_BLUE);
                ctx.drawTextWithShadow(this.textRenderer, s.getDescription(), listX + 10, cy + 19, AzyronTheme.TEXT_MUTED);
                if (eq) {
                    ctx.drawTextWithShadow(this.textRenderer, "EQUIPPED", listX + listW - 58, cy + 13, AzyronTheme.STATUS_GOOD);
                }
            }
        }

        // Right: Live Animated Character & Cloak Preview Stage
        int stageX = px + panelW - 132;
        int stageY = py + 54;
        int stageW = 118;
        int stageH = panelH - 68;
        renderPreviewStage(ctx, stageX, stageY, stageW, stageH, cm);
    }

    private void renderPreviewStage(DrawContext ctx, int x, int y, int w, int h, CosmeticsManager cm) {
        UiRenderUtils.drawRoundedPanel(ctx, x, y, w, h, 0xE6090B17, AzyronTheme.BORDER_SUBTLE);
        ctx.drawCenteredTextWithShadow(this.textRenderer, "LIVE PREVIEW", x + w / 2, y + 8, AzyronTheme.TEXT_SECONDARY);

        int cx = x + w / 2;
        int cy = y + h / 2 + 8;

        // Animated Halo above head
        if (cm.getActiveHalo() != CosmeticsManager.HaloStyle.NONE) {
            int bob = (int) Math.round(Math.sin(System.currentTimeMillis() / 320.0) * 3.0);
            int hc = cm.getActiveHalo().getColor();
            ctx.drawHorizontalLine(cx - 14, cx + 14, cy - 42 + bob, hc);
            ctx.drawHorizontalLine(cx - 10, cx + 10, cy - 44 + bob, AzyronTheme.TEXT_PRIMARY);
        }

        // Stylized Minecraft Player Silhouette + Equipped Cloak
        if (cm.getActiveCape() != CosmeticsManager.CapeStyle.NONE) {
            CosmeticsManager.CapeStyle cape = cm.getActiveCape();
            int sway = (int) Math.round(Math.sin(System.currentTimeMillis() / 450.0) * 2.0);
            ctx.fillGradient(cx - 14 + sway, cy - 16, cx + 14 + sway, cy + 28, cape.getPrimaryColor(), cape.getSecondaryColor());
            UiRenderUtils.drawAzyronEmblem(ctx, cx + sway, cy + 5, 8, 235);
        } else {
            UiRenderUtils.drawRoundedPanel(ctx, cx - 10, cy - 34, 20, 20, 0xFF334155, AzyronTheme.TEXT_SECONDARY);
            UiRenderUtils.drawRoundedPanel(ctx, cx - 10, cy - 13, 20, 26, 0xFF1E293B, AzyronTheme.BORDER_SUBTLE);
        }

        // Head
        UiRenderUtils.drawRoundedPanel(ctx, cx - 9, cy - 36, 18, 18, 0xFF1E243D, AzyronTheme.ACCENT_PURPLE_BRIGHT);

        String user = (this.client != null && this.client.getSession() != null) ? this.client.getSession().getUsername() : "Player";
        String tag = cm.getActiveBadge() != CosmeticsManager.BadgeStyle.NONE ? (" " + cm.getActiveBadge().getTag()) : "";
        UiRenderUtils.drawScaledText(ctx, this.textRenderer, user + tag, cx, y + h - 18, 0.8f, AzyronTheme.ACCENT_ELECTRIC_BLUE, true);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click.button() != 0) {
            return super.mouseClicked(click, doubled);
        }
        double mx = click.x();
        double my = click.y();

        int panelW = Math.min(430, this.width - 24);
        int panelH = Math.min(256, this.height - 24);
        int px = (this.width - panelW) / 2;
        int py = (this.height - panelH) / 2;

        if (UiRenderUtils.isHovered(mx, my, px + panelW - 62, py + 6, 52, 18)) {
            close();
            return true;
        }

        int tabW = 96;
        for (int i = 0; i < 3; i++) {
            if (UiRenderUtils.isHovered(mx, my, px + 14 + i * (tabW + 6), py + 28, tabW, 18)) {
                activeTab = i;
                return true;
            }
        }

        CosmeticsManager cm = AzyronClient.getInstance().getCosmeticsManager();
        int listX = px + 14;
        int listY = py + 54;
        int listW = panelW - 156;

        if (activeTab == 0) {
            CosmeticsManager.CapeStyle[] styles = CosmeticsManager.CapeStyle.values();
            for (int i = 0; i < styles.length; i++) {
                if (UiRenderUtils.isHovered(mx, my, listX, listY + i * 36, listW, 32)) {
                    cm.setActiveCape(styles[i]);
                    AzyronClient.getInstance().getConfig().save();
                    return true;
                }
            }
        } else if (activeTab == 1) {
            CosmeticsManager.HaloStyle[] styles = CosmeticsManager.HaloStyle.values();
            for (int i = 0; i < styles.length; i++) {
                if (UiRenderUtils.isHovered(mx, my, listX, listY + i * 38, listW, 34)) {
                    cm.setActiveHalo(styles[i]);
                    AzyronClient.getInstance().getConfig().save();
                    return true;
                }
            }
        } else {
            CosmeticsManager.BadgeStyle[] styles = CosmeticsManager.BadgeStyle.values();
            for (int i = 0; i < styles.length; i++) {
                if (UiRenderUtils.isHovered(mx, my, listX, listY + i * 38, listW, 34)) {
                    cm.setActiveBadge(styles[i]);
                    AzyronClient.getInstance().getConfig().save();
                    return true;
                }
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
