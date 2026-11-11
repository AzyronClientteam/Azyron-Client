package com.jigneshgaming.azyron.ui;

import com.jigneshgaming.azyron.AzyronClient;
import com.jigneshgaming.azyron.cosmetics.CosmeticsManager;
import com.jigneshgaming.azyron.profile.PerformanceProfile;
import com.jigneshgaming.azyron.util.AzyronTheme;
import com.jigneshgaming.azyron.util.McVersionBridge;
import com.jigneshgaming.azyron.util.UiRenderUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.SplashOverlay;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;

/**
 * Original Azyron-branded main menu for AZYRON CLIENT v1.0.3.
 * Renders live 3D Minecraft Panorama + ambient purple/electric-blue starfield + complete navigation.
 */
public final class AzyronMainMenuScreen extends AzyronBaseScreen {
    private record MenuButton(String id, String label, String subtitle, int x, int y, int w, int h, boolean primary, boolean accentDiscord) {}

    private final List<MenuButton> buttons = new ArrayList<>();

    private String lastUsername = "";
    private CosmeticsManager.BadgeStyle lastBadge = null;
    private String cachedUserLabel = "";
    private int cachedUserWidth = 0;

    private int lastStatsFps = -1;
    private int lastStatsRamPct = -1;
    private String cachedStatsLabel = "";
    private int cachedStatsWidth = 0;

    public AzyronMainMenuScreen() {
        super(Text.literal(AzyronClient.CLIENT_DISPLAY_NAME));
    }

    @Override
    protected void init() {
        rebuildLayout();
    }

    private void rebuildLayout() {
        buttons.clear();
        int cx = this.width / 2;
        int panelW = Math.min(244, Math.max(190, this.width - 40));
        int halfW = (panelW - 6) / 2;
        int startY = Math.max(88, this.height / 2 - 22);
        int btnH = 22;
        int gap = 5;

        // Primary Gameplay Actions
        buttons.add(new MenuButton("singleplayer", "Singleplayer", "Play local worlds", cx - panelW / 2, startY, panelW, btnH, true, false));
        startY += btnH + gap;

        buttons.add(new MenuButton("multiplayer", "Multiplayer", "Join servers", cx - panelW / 2, startY, panelW, btnH, true, false));
        startY += btnH + gap + 2;

        // Client Hub Row: Cosmetics & Mods
        buttons.add(new MenuButton("cosmetics", "Cosmetics", "Capes & Halos", cx - panelW / 2, startY, halfW, btnH, false, false));
        buttons.add(new MenuButton("mods", "Mods", "AZYRON Menu", cx - panelW / 2 + halfW + 6, startY, panelW - halfW - 6, btnH, false, false));
        startY += btnH + gap;

        // System Row: Settings & Quit Game
        buttons.add(new MenuButton("settings", "Settings", "Game Options", cx - panelW / 2, startY, halfW, btnH, false, false));
        buttons.add(new MenuButton("quit", "Quit Game", "Exit Desktop", cx - panelW / 2 + halfW + 6, startY, panelW - halfW - 6, btnH, false, false));
        startY += btnH + gap + 3;

        // Dedicated Discord Community Section
        buttons.add(new MenuButton("discord", "Discord  •  Join JIGNESH GAMING Community", "News, Updates & Support", cx - panelW / 2, startY, panelW, 22, false, true));

        // Top-right quick utility pills
        buttons.add(new MenuButton("profile", "Profile", "", this.width - 158, 8, 92, 18, false, false));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Skip rendering underneath an opaque cold-boot SplashOverlay before ShaderLoader finishes
        // compiling minecraft:core/rendertype_text and panorama textures.
        if (this.client != null && this.client.getOverlay() instanceof SplashOverlay && !AzyronLoadingRenderer.isCoreResourcesReady()) {
            return;
        }
        if (!UiRenderUtils.isCoreTextShaderReady(this.client)) {
            context.fillGradient(0, 0, this.width, this.height, 0xFF080A16, 0xFF0D1024);
            return;
        }

        // 1. Live rotating Minecraft 3D Panorama / version-native background
        McVersionBridge.renderMainMenuBackground(this, context, delta);

        // 2. Premium dark obsidian + purple/electric-blue vignette overlay
        context.fillGradient(0, 0, this.width, this.height, 0xB8080A16, 0xDC0D1024);
        renderAmbientParticles(context);

        // Top status bar
        renderTopBar(context, mouseX, mouseY);

        // Center Hero Logo & Branding
        int cx = this.width / 2;
        int heroTop = Math.max(30, this.height / 2 - 78);

        UiRenderUtils.drawLogoTexture(context, cx - 18, heroTop - 4, 36);
        UiRenderUtils.drawAzyronEmblem(context, cx, heroTop + 14, 15, 255);

        UiRenderUtils.drawScaledText(
                context, this.textRenderer, AzyronClient.CLIENT_DISPLAY_NAME, cx, heroTop + 34,
                1.15f, AzyronTheme.TEXT_PRIMARY, true
        );
        UiRenderUtils.drawCenteredText(
                context, this.textRenderer,
                "PERFORMANCE  •  SMOOTHNESS  •  STYLE",
                cx, heroTop + 50, AzyronTheme.ACCENT_ELECTRIC_BLUE
        );

        // Glassmorphic backdrop behind main buttons
        int panelW = Math.min(260, Math.max(206, this.width - 24));
        int panelTop = Math.max(80, this.height / 2 - 29);
        int panelH = 145;
        UiRenderUtils.drawRoundedPanel(
                context, cx - panelW / 2, panelTop, panelW, panelH,
                0x9E0B0E1D, AzyronTheme.BORDER_SUBTLE
        );
        UiRenderUtils.drawHorizontalGradient(
                context, cx - panelW / 2 + 2, panelTop, panelW - 4, 2,
                AzyronTheme.ACCENT_PURPLE_BRIGHT, AzyronTheme.ACCENT_ELECTRIC_BLUE
        );

        // Render interactive buttons
        for (MenuButton btn : buttons) {
            renderMenuButton(context, btn, mouseX, mouseY);
        }

        // Product brand and developer credit
        String leftFooter = AzyronClient.CLIENT_DISPLAY_NAME;
        String rightFooter = "Made by JIGNESH GAMING";
        UiRenderUtils.drawText(context, this.textRenderer, leftFooter, 8, this.height - 14, AzyronTheme.TEXT_MUTED);
        int rw = this.textRenderer.getWidth(rightFooter);
        UiRenderUtils.drawText(context, this.textRenderer, rightFooter, this.width - rw - 8, this.height - 14, AzyronTheme.ACCENT_PURPLE_BRIGHT);
    }

    private void renderTopBar(DrawContext ctx, int mouseX, int mouseY) {
        String username = (this.client != null && this.client.getSession() != null)
                ? this.client.getSession().getUsername()
                : "Player";
        AzyronClient inst = AzyronClient.getInstance();
        CosmeticsManager.BadgeStyle bs = (inst != null && inst.getCosmeticsManager() != null)
                ? inst.getCosmeticsManager().getActiveBadge()
                : CosmeticsManager.BadgeStyle.NONE;

        if (!username.equals(lastUsername) || bs != lastBadge || cachedUserWidth <= 0) {
            lastUsername = username;
            lastBadge = bs;
            String badge = (bs != null && bs != CosmeticsManager.BadgeStyle.NONE) ? (" " + bs.getTag()) : "";
            cachedUserLabel = "● " + username + badge;
            cachedUserWidth = this.textRenderer.getWidth(cachedUserLabel) + 16;
        }
        int uw = cachedUserWidth;
        UiRenderUtils.drawRoundedPanel(ctx, 8, 8, uw, 18, AzyronTheme.BG_CARD, AzyronTheme.ACCENT_PURPLE_BRIGHT);
        UiRenderUtils.drawText(ctx, this.textRenderer, cachedUserLabel, 16, 13, AzyronTheme.TEXT_PRIMARY);

        int fps = (this.client != null) ? this.client.getCurrentFps() : 240;
        int ramPct = MemoryOptimizer.getUsagePercent();
        if (fps != lastStatsFps || ramPct != lastStatsRamPct || cachedStatsWidth <= 0) {
            lastStatsFps = fps;
            lastStatsRamPct = ramPct;
            cachedStatsLabel = fps + " FPS  |  RAM " + ramPct + "%";
            cachedStatsWidth = this.textRenderer.getWidth(cachedStatsLabel) + 14;
        }
        int sw = cachedStatsWidth;
        int sx = 14 + uw;
        if (sx + sw < this.width - 166) {
            UiRenderUtils.drawRoundedPanel(ctx, sx, 8, sw, 18, AzyronTheme.BG_CARD, AzyronTheme.BORDER_SUBTLE);
            UiRenderUtils.drawText(ctx, this.textRenderer, cachedStatsLabel, sx + 7, 13, AzyronTheme.TEXT_SECONDARY);
        }
    }

    private void renderMenuButton(DrawContext ctx, MenuButton btn, int mouseX, int mouseY) {
        boolean hovered = UiRenderUtils.isHovered(mouseX, mouseY, btn.x, btn.y, btn.w, btn.h);

        if ("profile".equals(btn.id)) {
            PerformanceProfile prof = AzyronClient.getInstance() != null
                    ? AzyronClient.getInstance().getPerformanceEngine().getActiveProfile()
                    : PerformanceProfile.BALANCED;
            String pName = switch (prof) {
                case POTATO -> "⚡ Potato";
                case BALANCED -> "✦ Balanced";
                case SMOOTH_FPS -> "◈ Smooth FPS";
            };
            UiRenderUtils.drawAccentCard(ctx, btn.x, btn.y, btn.w, btn.h, hovered ? AzyronTheme.BG_CARD_HOVER : AzyronTheme.BG_CARD, hovered, true);
            UiRenderUtils.drawCenteredText(ctx, this.textRenderer, pName, btn.x + btn.w / 2, btn.y + 5, AzyronTheme.ACCENT_ELECTRIC_BLUE);
            return;
        }
        if (btn.accentDiscord) {
            int bg = hovered ? 0xEB3B44A8 : 0xD8262D6E;
            int border = hovered ? AzyronTheme.ACCENT_ELECTRIC_BLUE : AzyronTheme.DISCORD_BLURPLE;
            UiRenderUtils.drawRoundedPanel(ctx, btn.x, btn.y, btn.w, btn.h, bg, border);
            UiRenderUtils.drawCenteredText(ctx, this.textRenderer, btn.label, btn.x + btn.w / 2, btn.y + 7, AzyronTheme.TEXT_PRIMARY);
            return;
        }

        int bg = hovered
                ? AzyronTheme.lerpColor(AzyronTheme.BG_CARD_HOVER, AzyronTheme.ACCENT_PURPLE, 0.28f)
                : (btn.primary ? AzyronTheme.BG_CARD_ACTIVE : AzyronTheme.BG_CARD);
        UiRenderUtils.drawAccentCard(ctx, btn.x, btn.y, btn.w, btn.h, bg, hovered, btn.primary && hovered);
        int textCol = ("quit".equals(btn.id) && hovered) ? AzyronTheme.STATUS_DANGER : AzyronTheme.TEXT_PRIMARY;
        UiRenderUtils.drawCenteredText(ctx, this.textRenderer, btn.label, btn.x + btn.w / 2, btn.y + 7, textCol);
    }

    private void renderAmbientParticles(DrawContext ctx) {
        AzyronClient inst = AzyronClient.getInstance();
        if (inst != null && inst.getPerformanceEngine() != null) {
            if (!inst.getPerformanceEngine().isWindowFocused()
                    || inst.getPerformanceEngine().getActiveProfile() == PerformanceProfile.POTATO) {
                return;
            }
        }
        long t = System.currentTimeMillis();
        int count = MemoryOptimizer.detectRamTier() == MemoryOptimizer.RamTier.TIER_3GB ? 8 : 14;
        for (int i = 0; i < count; i++) {
            double speed = 0.012 + (i % 5) * 0.004;
            int px = (int) (((i * 97L) + (long) (t * speed)) % Math.max(1, this.width));
            int py = (int) (((i * 53L) + (long) (t * speed * 0.6)) % Math.max(1, this.height));
            int col = (i % 2 == 0) ? AzyronTheme.ACCENT_PURPLE_BRIGHT : AzyronTheme.ACCENT_ELECTRIC_BLUE;
            ctx.fill(px, py, px + 2, py + 2, AzyronTheme.withAlpha(col, 85));
        }
    }

    @Override
    public boolean azyronMouseClicked(double mx, double my, int button) {
        if (button == 0 && this.client != null) {
            for (MenuButton btn : buttons) {
                if (UiRenderUtils.isHovered(mx, my, btn.x, btn.y, btn.w, btn.h)) {
                    handleAction(btn.id);
                    return true;
                }
            }
        }
        return false;
    }

    public void handleAction(String actionId) {
        if (this.client == null) {
            return;
        }
        AzyronClient inst = AzyronClient.getInstance();
        switch (actionId) {
            case "singleplayer" -> this.client.setScreen(new SelectWorldScreen(this));
            case "multiplayer" -> this.client.setScreen(new MultiplayerScreen(this));
            case "cosmetics" -> this.client.setScreen(new AzyronCosmeticsScreen(this));
            case "mods" -> this.client.setScreen(new AzyronModsScreen(this));
            case "settings" -> this.client.setScreen(new OptionsScreen(this, this.client.options));
            case "quit" -> this.client.scheduleStop();
            case "discord" -> {
                String url = (inst != null && inst.getConfig() != null)
                        ? inst.getConfig().getDiscordInviteUrl()
                        : "https://discord.gg/jigneshgaming";
                McVersionBridge.openConfirmLink(this, url);
            }
            case "profile" -> {
                if (inst != null && inst.getPerformanceEngine() != null) {
                    PerformanceProfile next = inst.getPerformanceEngine().getActiveProfile().next();
                    inst.getPerformanceEngine().applyProfile(next, this.client, true);
                }
            }
            default -> {}
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
