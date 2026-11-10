package com.jigneshgaming.azyron.ui;

import com.jigneshgaming.azyron.AzyronClient;
import com.jigneshgaming.azyron.config.AzyronConfig;
import com.jigneshgaming.azyron.module.AzyronModule;
import com.jigneshgaming.azyron.module.HudModule;
import com.jigneshgaming.azyron.module.ModuleCategory;
import com.jigneshgaming.azyron.module.Setting;
import com.jigneshgaming.azyron.profile.FrameTimeTracker;
import com.jigneshgaming.azyron.profile.MemoryOptimizer;
import com.jigneshgaming.azyron.profile.PerformanceEngine;
import com.jigneshgaming.azyron.profile.PerformanceProfile;
import com.jigneshgaming.azyron.util.AzyronTheme;
import com.jigneshgaming.azyron.util.McVersionBridge;
import com.jigneshgaming.azyron.util.UiRenderUtils;
import java.util.List;
import java.util.Locale;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * AZYRON CLIENT Main Mod Hub (Opened with Right Shift or from Main Menu / Pause Menu).
 * Unified across Minecraft 1.21.11 (Fabric) and Minecraft 1.20.1 (Fabric):
 *  - Sidebar category tabs + instant HUD Editor launcher
 *  - Real-time module search filter
 *  - 2-column responsive module cards with ON/OFF toggle + Mod Settings drawer
 *  - Dedicated Performance Profiles, Selectable MC Version Stack & Optimization center
 */
public final class AzyronModsScreen extends AzyronBaseScreen {
    private enum Section {
        ALL_MODS("All Mods", ModuleCategory.ALL),
        HUD_MODS("HUD Widgets", ModuleCategory.HUD),
        GAMEPLAY("Gameplay", ModuleCategory.GAMEPLAY),
        VISUALS("Visuals", ModuleCategory.VISUAL),
        PROFILES("FPS Profiles", null),
        CLIENT_SETTINGS("Client Options", null);

        final String title;
        final ModuleCategory category;

        Section(String title, ModuleCategory category) {
            this.title = title;
            this.category = category;
        }
    }

    private final Screen parent;
    private Section activeSection = Section.ALL_MODS;
    private AzyronModule editingModule = null;
    private Setting.NumberSetting draggingSlider = null;
    private int sliderTrackX = 0;
    private int sliderTrackW = 1;
    private boolean draggingHudScale = false;

    private String searchQuery = "";
    private boolean searchFocused = false;
    private double scrollOffset = 0.0;
    private int maxScroll = 0;

    private ModuleCategory lastFilteredCategory = null;
    private String lastFilteredSearch = null;
    private List<AzyronModule> cachedFilteredModules = List.of();
    private static final java.util.Map<String, Boolean> MOD_LOADED_CACHE = new java.util.concurrent.ConcurrentHashMap<>();

    private List<AzyronModule> getFilteredModulesCached() {
        ModuleCategory cat = activeSection.category;
        if (cat != lastFilteredCategory || !searchQuery.equals(lastFilteredSearch)) {
            lastFilteredCategory = cat;
            lastFilteredSearch = searchQuery;
            cachedFilteredModules = AzyronClient.getInstance().getModuleManager().filterModules(cat, searchQuery);
        }
        return cachedFilteredModules;
    }

    public AzyronModsScreen(Screen parent) {
        super(Text.literal("AZYRON CLIENT Menu"));
        this.parent = parent;
    }

    public void openModuleSettings(AzyronModule module) {
        this.editingModule = module;
        this.scrollOffset = 0.0;
    }

    public void openProfilesTab() {
        this.editingModule = null;
        this.activeSection = Section.PROFILES;
        this.scrollOffset = 0.0;
    }

    public void openClientOptionsTab() {
        this.editingModule = null;
        this.activeSection = Section.CLIENT_SETTINGS;
        this.scrollOffset = 0.0;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fillGradient(0, 0, this.width, this.height, 0xCC060812, 0xDC0B0F22);

        int winW = Math.min(476, this.width - 16);
        int winH = Math.min(286, this.height - 16);
        int wx = (this.width - winW) / 2;
        int wy = (this.height - winH) / 2;

        // Outer glassmorphic client window
        UiRenderUtils.drawRoundedPanel(ctx, wx, wy, winW, winH, AzyronTheme.BG_PANEL, AzyronTheme.ACCENT_PURPLE_BRIGHT);
        UiRenderUtils.drawHorizontalGradient(ctx, wx + 2, wy, winW - 4, 2, AzyronTheme.ACCENT_PURPLE_BRIGHT, AzyronTheme.ACCENT_ELECTRIC_BLUE);

        int sideW = 108;
        renderSidebar(ctx, wx, wy, sideW, winH, mouseX, mouseY);
        renderTopHeader(ctx, wx + sideW, wy, winW - sideW, mouseX, mouseY);

        int contentX = wx + sideW + 8;
        int contentY = wy + 34;
        int contentW = winW - sideW - 16;
        int contentH = winH - 42;

        ctx.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);
        if (editingModule != null) {
            renderModuleSettingsDrawer(ctx, contentX, contentY, contentW, contentH, mouseX, mouseY);
        } else if (activeSection == Section.PROFILES) {
            renderProfilesDashboard(ctx, contentX, contentY, contentW, contentH, mouseX, mouseY);
        } else if (activeSection == Section.CLIENT_SETTINGS) {
            renderClientSettingsTab(ctx, contentX, contentY, contentW, contentH, mouseX, mouseY);
        } else {
            renderModuleGrid(ctx, contentX, contentY, contentW, contentH, mouseX, mouseY);
        }
        ctx.disableScissor();
    }

    private void renderSidebar(DrawContext ctx, int wx, int wy, int sideW, int winH, int mx, int my) {
        ctx.fill(wx + 1, wy + 2, wx + sideW, wy + winH - 1, AzyronTheme.BG_SIDEBAR);
        UiRenderUtils.drawVLine(ctx, wx + sideW, wy + 2, wy + winH - 2, AzyronTheme.BORDER_SUBTLE);

        // Brand badge
        UiRenderUtils.drawAzyronEmblem(ctx, wx + 16, wy + 17, 9, 255);
        UiRenderUtils.drawText(ctx, this.textRenderer, "AZYRON", wx + 29, wy + 10, AzyronTheme.TEXT_PRIMARY);
        UiRenderUtils.drawScaledText(
                ctx, this.textRenderer,
                McVersionBridge.getActiveMcVersionString() + " FABRIC",
                wx + 29, wy + 20, 0.72f, AzyronTheme.ACCENT_ELECTRIC_BLUE, false
        );

        int navY = wy + 36;
        for (Section sec : Section.values()) {
            boolean selected = (editingModule == null && activeSection == sec);
            boolean hov = UiRenderUtils.isHovered(mx, my, wx + 6, navY, sideW - 12, 20);
            UiRenderUtils.drawAccentCard(
                    ctx, wx + 6, navY, sideW - 12, 20,
                    selected ? AzyronTheme.BG_CARD_ACTIVE : (hov ? AzyronTheme.BG_CARD_HOVER : AzyronTheme.BG_CARD),
                    hov, selected
            );
            UiRenderUtils.drawText(
                    ctx, this.textRenderer, sec.title, wx + 13, navY + 6,
                    selected ? AzyronTheme.ACCENT_ELECTRIC_BLUE : AzyronTheme.TEXT_PRIMARY
            );
            navY += 24;
        }

        // Bottom HUD Editor Launch CTA + Cosmetics shortcut
        int hudBtnY = wy + winH - 50;
        boolean hudHov = UiRenderUtils.isHovered(mx, my, wx + 6, hudBtnY, sideW - 12, 20);
        UiRenderUtils.drawAccentCard(ctx, wx + 6, hudBtnY, sideW - 12, 20, hudHov ? AzyronTheme.ACCENT_PURPLE : AzyronTheme.BG_CARD_ACTIVE, hudHov, true);
        UiRenderUtils.drawCenteredText(ctx, this.textRenderer, "✦ HUD Editor", wx + sideW / 2, hudBtnY + 6, AzyronTheme.TEXT_PRIMARY);

        int cosBtnY = wy + winH - 26;
        boolean cosHov = UiRenderUtils.isHovered(mx, my, wx + 6, cosBtnY, sideW - 12, 18);
        UiRenderUtils.drawRoundedPanel(ctx, wx + 6, cosBtnY, sideW - 12, 18, cosHov ? AzyronTheme.BG_CARD_HOVER : AzyronTheme.BG_CARD, AzyronTheme.BORDER_SUBTLE);
        UiRenderUtils.drawCenteredText(ctx, this.textRenderer, "Cosmetics", wx + sideW / 2, cosBtnY + 5, AzyronTheme.TEXT_SECONDARY);
    }

    private void renderTopHeader(DrawContext ctx, int x, int y, int w, int mx, int my) {
        UiRenderUtils.drawHLine(ctx, x, x + w - 2, y + 30, AzyronTheme.BORDER_SUBTLE);

        if (editingModule != null) {
            boolean backHov = UiRenderUtils.isHovered(mx, my, x + 8, y + 7, 54, 18);
            UiRenderUtils.drawRoundedPanel(ctx, x + 8, y + 7, 54, 18, backHov ? AzyronTheme.BG_CARD_HOVER : AzyronTheme.BG_CARD, AzyronTheme.ACCENT_ELECTRIC_BLUE);
            UiRenderUtils.drawCenteredText(ctx, this.textRenderer, "← Back", x + 35, y + 12, AzyronTheme.ACCENT_ELECTRIC_BLUE);
            UiRenderUtils.drawText(ctx, this.textRenderer, editingModule.getName() + "  •  Mod Settings", x + 70, y + 12, AzyronTheme.TEXT_PRIMARY);
        } else if (activeSection.category != null) {
            // Search Box
            int sbW = Math.min(186, w - 92);
            int sbX = x + 8;
            int sbY = y + 7;
            int border = searchFocused ? AzyronTheme.ACCENT_ELECTRIC_BLUE : AzyronTheme.BORDER_SUBTLE;
            UiRenderUtils.drawRoundedPanel(ctx, sbX, sbY, sbW, 18, 0xE6080A16, border);
            String cursor = (searchFocused && (System.currentTimeMillis() / 450L) % 2L == 0L) ? "_" : "";
            String display = searchQuery.isEmpty() && !searchFocused
                    ? "Search mods (FPS, Zoom, CPS...)"
                    : (searchQuery + cursor);
            int col = searchQuery.isEmpty() && !searchFocused ? AzyronTheme.TEXT_MUTED : AzyronTheme.TEXT_PRIMARY;
            UiRenderUtils.drawText(ctx, this.textRenderer, display, sbX + 7, sbY + 5, col);
        } else {
            UiRenderUtils.drawText(ctx, this.textRenderer, activeSection.title, x + 10, y + 12, AzyronTheme.TEXT_PRIMARY);
        }

        // Close button
        int closeX = x + w - 26;
        boolean closeHov = UiRenderUtils.isHovered(mx, my, closeX, y + 7, 18, 18);
        UiRenderUtils.drawRoundedPanel(ctx, closeX, y + 7, 18, 18, closeHov ? 0xCCEF4444 : AzyronTheme.BG_CARD, AzyronTheme.BORDER_SUBTLE);
        UiRenderUtils.drawCenteredText(ctx, this.textRenderer, "✕", closeX + 9, y + 12, AzyronTheme.TEXT_PRIMARY);
    }

    private void renderModuleGrid(DrawContext ctx, int x, int y, int w, int h, int mx, int my) {
        List<AzyronModule> mods = getFilteredModulesCached();
        int cols = w >= 290 ? 2 : 1;
        int gap = 6;
        int cardW = (w - gap * (cols - 1)) / cols;
        int cardH = 44;

        int rows = (mods.size() + cols - 1) / cols;
        int totalH = rows * (cardH + gap);
        maxScroll = Math.max(0, totalH - h);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));

        for (int i = 0; i < mods.size(); i++) {
            AzyronModule mod = mods.get(i);
            int col = i % cols;
            int row = i / cols;
            int cx = x + col * (cardW + gap);
            int cy = y + row * (cardH + gap) - (int) Math.round(scrollOffset);

            if (cy + cardH < y || cy > y + h) {
                continue;
            }

            boolean cardHov = UiRenderUtils.isHovered(mx, my, cx, cy, cardW, cardH);
            UiRenderUtils.drawAccentCard(
                    ctx, cx, cy, cardW, cardH,
                    mod.isEnabled() ? AzyronTheme.BG_CARD_ACTIVE : AzyronTheme.BG_CARD,
                    cardHov, mod.isEnabled()
            );

            UiRenderUtils.drawText(ctx, this.textRenderer, mod.getName(), cx + 8, cy + 6, AzyronTheme.TEXT_PRIMARY);

            String desc = this.textRenderer.trimToWidth(mod.getDescription(), cardW - 16);
            UiRenderUtils.drawScaledText(ctx, this.textRenderer, desc, cx + 8, cy + 18, 0.78f, AzyronTheme.TEXT_MUTED, false);

            int gearX = cx + 8;
            int gearY = cy + 28;
            boolean gearHov = UiRenderUtils.isHovered(mx, my, gearX, gearY, 54, 12);
            UiRenderUtils.drawRoundedPanel(ctx, gearX, gearY, 54, 12, gearHov ? AzyronTheme.BG_CARD_HOVER : 0xAA0B0E1D, AzyronTheme.BORDER_SUBTLE);
            UiRenderUtils.drawScaledText(ctx, this.textRenderer, "⚙ Settings", gearX + 27, gearY + 3, 0.75f, gearHov ? AzyronTheme.ACCENT_ELECTRIC_BLUE : AzyronTheme.TEXT_SECONDARY, true);

            int toggleW = 38;
            int toggleX = cx + cardW - toggleW - 7;
            int toggleY = cy + 27;
            int toggleBg = mod.isEnabled() ? AzyronTheme.ACCENT_PURPLE_BRIGHT : 0xFF1E293B;
            int toggleBorder = mod.isEnabled() ? AzyronTheme.ACCENT_ELECTRIC_BLUE : AzyronTheme.BORDER_SUBTLE;
            UiRenderUtils.drawRoundedPanel(ctx, toggleX, toggleY, toggleW, 13, toggleBg, toggleBorder);
            String stateText = mod.isEnabled() ? "ON" : "OFF";
            UiRenderUtils.drawScaledText(ctx, this.textRenderer, stateText, toggleX + toggleW / 2, toggleY + 3, 0.78f, AzyronTheme.TEXT_PRIMARY, true);
        }
    }

    private void renderModuleSettingsDrawer(DrawContext ctx, int x, int y, int w, int h, int mx, int my) {
        AzyronModule mod = editingModule;
        int curY = y - (int) Math.round(scrollOffset);

        // Master Enable Row
        UiRenderUtils.drawAccentCard(ctx, x, curY, w, 28, AzyronTheme.BG_CARD_ACTIVE, false, mod.isEnabled());
        UiRenderUtils.drawText(ctx, this.textRenderer, "Module Enabled", x + 10, curY + 5, AzyronTheme.TEXT_PRIMARY);
        UiRenderUtils.drawText(ctx, this.textRenderer, mod.getDescription(), x + 10, curY + 16, AzyronTheme.TEXT_MUTED);
        int mToggleX = x + w - 52;
        UiRenderUtils.drawRoundedPanel(ctx, mToggleX, curY + 6, 44, 16, mod.isEnabled() ? AzyronTheme.ACCENT_PURPLE_BRIGHT : 0xFF1E293B, AzyronTheme.ACCENT_ELECTRIC_BLUE);
        UiRenderUtils.drawCenteredText(ctx, this.textRenderer, mod.isEnabled() ? "ENABLED" : "OFF", mToggleX + 22, curY + 10, AzyronTheme.TEXT_PRIMARY);
        curY += 34;

        // HUD Scale Slider if this is a HudModule
        if (mod instanceof HudModule hud) {
            UiRenderUtils.drawRoundedPanel(ctx, x, curY, w, 26, AzyronTheme.BG_CARD, AzyronTheme.BORDER_SUBTLE);
            String scaleLbl = String.format(Locale.ROOT, "HUD Widget Scale: %.2fx", hud.getScale());
            UiRenderUtils.drawText(ctx, this.textRenderer, scaleLbl, x + 10, curY + 9, AzyronTheme.TEXT_PRIMARY);
            int barX = x + w - 124;
            int barW = 112;
            float norm = (hud.getScale() - 0.5f) / 1.5f;
            drawSliderBar(ctx, barX, curY + 10, barW, norm);
            curY += 30;
        }

        // Dynamic Module Settings
        for (Setting<?> setting : mod.getSettings()) {
            UiRenderUtils.drawRoundedPanel(ctx, x, curY, w, 26, AzyronTheme.BG_CARD, AzyronTheme.BORDER_SUBTLE);
            UiRenderUtils.drawText(ctx, this.textRenderer, setting.getLabel(), x + 10, curY + 5, AzyronTheme.TEXT_PRIMARY);
            UiRenderUtils.drawScaledText(ctx, this.textRenderer, setting.getDescription(), x + 10, curY + 16, 0.72f, AzyronTheme.TEXT_MUTED, false);

            if (setting instanceof Setting.BooleanSetting bs) {
                int bx = x + w - 48;
                UiRenderUtils.drawRoundedPanel(ctx, bx, curY + 5, 38, 15, bs.getValue() ? AzyronTheme.ACCENT_PURPLE_BRIGHT : 0xFF1E293B, AzyronTheme.BORDER_SUBTLE);
                UiRenderUtils.drawCenteredText(ctx, this.textRenderer, bs.getValue() ? "ON" : "OFF", bx + 19, curY + 9, AzyronTheme.TEXT_PRIMARY);
            } else if (setting instanceof Setting.ModeSetting ms) {
                String val = "< " + ms.getValue() + " >";
                int bw = Math.max(76, this.textRenderer.getWidth(val) + 12);
                int bx = x + w - bw - 8;
                UiRenderUtils.drawRoundedPanel(ctx, bx, curY + 5, bw, 15, AzyronTheme.BG_CARD_ACTIVE, AzyronTheme.ACCENT_ELECTRIC_BLUE);
                UiRenderUtils.drawCenteredText(ctx, this.textRenderer, val, bx + bw / 2, curY + 9, AzyronTheme.ACCENT_ELECTRIC_BLUE);
            } else if (setting instanceof Setting.NumberSetting ns) {
                String valStr = ns.getFormattedValue();
                UiRenderUtils.drawText(ctx, this.textRenderer, valStr, x + w - 156, curY + 9, AzyronTheme.ACCENT_ELECTRIC_BLUE);
                drawSliderBar(ctx, x + w - 120, curY + 10, 108, ns.getNormalized());
            }
            curY += 30;
        }

        // Reset Module Button
        boolean resetHov = UiRenderUtils.isHovered(mx, my, x, curY, 118, 20);
        UiRenderUtils.drawRoundedPanel(ctx, x, curY, 118, 20, resetHov ? AzyronTheme.BG_CARD_HOVER : AzyronTheme.BG_CARD, AzyronTheme.STATUS_WARN);
        UiRenderUtils.drawCenteredText(ctx, this.textRenderer, "Reset to Defaults", x + 59, curY + 6, AzyronTheme.STATUS_WARN);
        curY += 26;

        int contentTotal = (curY + (int) Math.round(scrollOffset)) - y;
        maxScroll = Math.max(0, contentTotal - h);
    }

    private void drawSliderBar(DrawContext ctx, int x, int y, int w, float norm) {
        float clamped = Math.max(0.0f, Math.min(1.0f, norm));
        ctx.fill(x, y, x + w, y + 5, 0xFF090C18);
        int fillW = Math.round(w * clamped);
        if (fillW > 0) {
            UiRenderUtils.drawHorizontalGradient(ctx, x, y, fillW, 5, AzyronTheme.ACCENT_PURPLE_BRIGHT, AzyronTheme.ACCENT_ELECTRIC_BLUE);
        }
        int knobX = x + fillW;
        ctx.fill(knobX - 2, y - 2, knobX + 2, y + 7, AzyronTheme.TEXT_PRIMARY);
    }

    private void renderProfilesDashboard(DrawContext ctx, int x, int y, int w, int h, int mx, int my) {
        maxScroll = 0;
        PerformanceEngine engine = AzyronClient.getInstance().getPerformanceEngine();
        PerformanceProfile current = engine.getActiveProfile();

        // Live Engine Telemetry Strip
        UiRenderUtils.drawRoundedPanel(ctx, x, y, w, 28, AzyronTheme.BG_CARD_ACTIVE, AzyronTheme.BORDER_SUBTLE);
        int fps = this.client != null ? this.client.getCurrentFps() : 240;
        int low1 = FrameTimeTracker.getOnePercentLowFps(fps);
        String telemetry = String.format(
                Locale.ROOT,
                "FPS: %d (1%%: %d)  |  %s  |  RAM: %d%% (%d/%dMB • %s)",
                fps, low1, FrameTimeTracker.getStabilityRating(),
                MemoryOptimizer.getUsagePercent(), MemoryOptimizer.getUsedMb(), MemoryOptimizer.getMaxMb(),
                MemoryOptimizer.detectRamTier().getLabel()
        );
        UiRenderUtils.drawText(ctx, this.textRenderer, this.textRenderer.trimToWidth(telemetry, w - 92), x + 8, y + 10, AzyronTheme.TEXT_PRIMARY);

        // Manual Trim Memory CTA
        int trimX = x + w - 80;
        boolean trimHov = UiRenderUtils.isHovered(mx, my, trimX, y + 5, 74, 18);
        UiRenderUtils.drawAccentCard(ctx, trimX, y + 5, 74, 18, trimHov ? AzyronTheme.BG_CARD_HOVER : AzyronTheme.BG_CARD, trimHov, false);
        UiRenderUtils.drawCenteredText(ctx, this.textRenderer, "Boost RAM", trimX + 37, y + 10, AzyronTheme.ACCENT_ELECTRIC_BLUE);

        // 3 Performance Profile Cards: Potato Mode, Balanced Mode, Smooth FPS Mode
        PerformanceProfile[] profiles = PerformanceProfile.values();
        int py = y + 34;
        for (PerformanceProfile p : profiles) {
            boolean active = (p == current);
            boolean hov = UiRenderUtils.isHovered(mx, my, x, py, w, 46);
            UiRenderUtils.drawAccentCard(
                    ctx, x, py, w, 46,
                    active ? AzyronTheme.BG_CARD_ACTIVE : AzyronTheme.BG_CARD,
                    hov, active
            );
            UiRenderUtils.drawText(ctx, this.textRenderer, p.getDisplayName(), x + 10, py + 6, active ? AzyronTheme.ACCENT_ELECTRIC_BLUE : AzyronTheme.TEXT_PRIMARY);
            UiRenderUtils.drawScaledText(ctx, this.textRenderer, p.getSummary(), x + 10, py + 18, 0.78f, AzyronTheme.TEXT_SECONDARY, false);

            String specs = String.format(
                    Locale.ROOT,
                    "Chunks: %d/%d  •  Entity Dist: %.0f%%  •  Particle Cap: %d/tick  •  Target FPS: %d",
                    p.getViewDistance(), p.getSimulationDistance(),
                    p.getEntityDistanceScaling() * 100.0, p.getMaxParticlesPerTick(), p.getMaxFps()
            );
            UiRenderUtils.drawScaledText(ctx, this.textRenderer, specs, x + 10, py + 31, 0.74f, AzyronTheme.TEXT_MUTED, false);

            String badge = active ? "ACTIVE" : "APPLY";
            int bw = 52;
            int bx = x + w - bw - 8;
            UiRenderUtils.drawRoundedPanel(ctx, bx, py + 14, bw, 18, active ? AzyronTheme.ACCENT_PURPLE_BRIGHT : 0xFF1E293B, AzyronTheme.ACCENT_ELECTRIC_BLUE);
            UiRenderUtils.drawCenteredText(ctx, this.textRenderer, badge, bx + bw / 2, py + 19, AzyronTheme.TEXT_PRIMARY);

            py += 50;
        }

        // Integrated Optimization Mod Stack Status Bar
        UiRenderUtils.drawRoundedPanel(ctx, x, py + 2, w, 34, 0xCC090C18, AzyronTheme.BORDER_SUBTLE);
        UiRenderUtils.drawText(
                ctx, this.textRenderer,
                "Optimization Mod Stack (" + McVersionBridge.getActiveMcVersionString() + " Fabric):",
                x + 8, py + 7, AzyronTheme.TEXT_SECONDARY
        );

        String[][] modStack = {
                {"sodium", "Sodium"},
                {"lithium", "Lithium"},
                {"ferritecore", "FerriteCore"},
                {"entityculling", "EntityCulling"},
                {"moreculling", "MoreCulling"},
                {"dynamic_fps", "DynamicFPS"}
        };
        int chipX = x + 8;
        for (String[] entry : modStack) {
            boolean loaded = isModLoaded(entry[0]);
            String txt = (loaded ? "● " : "○ ") + entry[1];
            int cw = Math.round(this.textRenderer.getWidth(txt) * 0.76f) + 8;
            int col = loaded ? AzyronTheme.STATUS_GOOD : AzyronTheme.TEXT_MUTED;
            UiRenderUtils.drawScaledText(ctx, this.textRenderer, txt, chipX, py + 21, 0.76f, col, false);
            chipX += cw;
        }
    }

    private void renderClientSettingsTab(DrawContext ctx, int x, int y, int w, int h, int mx, int my) {
        maxScroll = 0;
        AzyronConfig cfg = AzyronClient.getInstance().getConfig();
        boolean[][] rows = {
                {cfg.isCustomMainMenu()},
                {cfg.isCustomLoadingScreen()},
                {cfg.isAutoMemoryOptimization()}
        };
        String[] titles = {
                "AZYRON Custom Main Menu",
                "AZYRON Custom Loading Screen",
                "Smart Memory & Allocation Optimizer"
        };
        String[] descs = {
                "Use dark purple & electric-blue AZYRON main menu with Panorama & Discord",
                "Display 'Welcome to AZYRON CLIENT' animated progress screen during reloads",
                "Monitor heap pressure & smoothly prevent GC lag spikes between worlds"
        };

        for (int i = 0; i < titles.length; i++) {
            int ry = y + i * 33;
            boolean val = rows[i][0];
            UiRenderUtils.drawAccentCard(ctx, x, ry, w, 29, AzyronTheme.BG_CARD, UiRenderUtils.isHovered(mx, my, x, ry, w, 29), val);
            UiRenderUtils.drawText(ctx, this.textRenderer, titles[i], x + 10, ry + 5, AzyronTheme.TEXT_PRIMARY);
            UiRenderUtils.drawScaledText(ctx, this.textRenderer, descs[i], x + 10, ry + 17, 0.75f, AzyronTheme.TEXT_MUTED, false);

            int bx = x + w - 50;
            UiRenderUtils.drawRoundedPanel(ctx, bx, ry + 6, 42, 16, val ? AzyronTheme.ACCENT_PURPLE_BRIGHT : 0xFF1E293B, AzyronTheme.ACCENT_ELECTRIC_BLUE);
            UiRenderUtils.drawCenteredText(ctx, this.textRenderer, val ? "ON" : "OFF", bx + 21, ry + 10, AzyronTheme.TEXT_PRIMARY);
        }

        // Selectable Minecraft Version Profile Row (1.21.11 Fabric / 1.20.1 Fabric)
        int verY = y + 102;
        McVersionBridge.SupportedMcVersion selectedVer = cfg.getSelectedMcVersion();
        boolean verHov = UiRenderUtils.isHovered(mx, my, x, verY, w, 34);
        UiRenderUtils.drawAccentCard(ctx, x, verY, w, 34, AzyronTheme.BG_CARD_ACTIVE, verHov, true);
        UiRenderUtils.drawText(
                ctx, this.textRenderer,
                "Supported Minecraft Profile (Active Runtime: " + McVersionBridge.getActiveMcVersionString() + ")",
                x + 10, verY + 5, AzyronTheme.TEXT_PRIMARY
        );
        UiRenderUtils.drawScaledText(
                ctx, this.textRenderer,
                this.textRenderer.trimToWidth(
                        "Java " + selectedVer.getMinJavaVersion() + "+  •  Fabric API " + selectedVer.getFabricApiVersion()
                        + "  •  " + selectedVer.getModStackSummary(),
                        w - 110
                ),
                x + 10, verY + 19, 0.72f, AzyronTheme.TEXT_SECONDARY, false
        );
        String verBtnLabel = "< " + selectedVer.getVersionId() + " >";
        int verBtnW = 82;
        int verBtnX = x + w - verBtnW - 8;
        UiRenderUtils.drawRoundedPanel(ctx, verBtnX, verY + 8, verBtnW, 18, AzyronTheme.ACCENT_PURPLE_BRIGHT, AzyronTheme.ACCENT_ELECTRIC_BLUE);
        UiRenderUtils.drawCenteredText(ctx, this.textRenderer, verBtnLabel, verBtnX + verBtnW / 2, verY + 13, AzyronTheme.TEXT_PRIMARY);

        int infoY = y + 142;
        UiRenderUtils.drawRoundedPanel(ctx, x, infoY, w, 52, 0xCC090C18, AzyronTheme.BORDER_SUBTLE);
        UiRenderUtils.drawText(
                ctx, this.textRenderer,
                "AZYRON CLIENT  •  v" + AzyronClient.CLIENT_VERSION + "  (Minecraft 1.21.11 & 1.20.1 Fabric)",
                x + 10, infoY + 7, AzyronTheme.ACCENT_ELECTRIC_BLUE
        );
        UiRenderUtils.drawText(ctx, this.textRenderer, "Created by JIGNESH GAMING  |  PojavLauncher & Desktop Ready", x + 10, infoY + 20, AzyronTheme.TEXT_PRIMARY);
        UiRenderUtils.drawText(ctx, this.textRenderer, "Keybinds: [Right Shift] Client Menu  •  [C] Smooth Zoom  •  [H] HUD Editor", x + 10, infoY + 33, AzyronTheme.TEXT_MUTED);
    }

    private static boolean isModLoaded(String modId) {
        return MOD_LOADED_CACHE.computeIfAbsent(modId, id -> {
            try {
                return FabricLoader.getInstance().isModLoaded(id)
                        || ("dynamic_fps".equals(id) && FabricLoader.getInstance().isModLoaded("dynamicfps"));
            } catch (Throwable ignored) {
                return false;
            }
        });
    }

    @Override
    public boolean azyronMouseClicked(double mx, double my, int btn) {
        int winW = Math.min(476, this.width - 16);
        int winH = Math.min(286, this.height - 16);
        int wx = (this.width - winW) / 2;
        int wy = (this.height - winH) / 2;
        int sideW = 108;

        // Close button
        if (btn == 0 && UiRenderUtils.isHovered(mx, my, wx + winW - 26, wy + 7, 18, 18)) {
            close();
            return true;
        }

        // Sidebar clicks
        int navY = wy + 36;
        for (Section sec : Section.values()) {
            if (btn == 0 && UiRenderUtils.isHovered(mx, my, wx + 6, navY, sideW - 12, 20)) {
                activeSection = sec;
                editingModule = null;
                scrollOffset = 0.0;
                searchFocused = false;
                return true;
            }
            navY += 24;
        }

        if (btn == 0 && UiRenderUtils.isHovered(mx, my, wx + 6, wy + winH - 50, sideW - 12, 20) && this.client != null) {
            this.client.setScreen(new HudEditorScreen(this));
            return true;
        }
        if (btn == 0 && UiRenderUtils.isHovered(mx, my, wx + 6, wy + winH - 26, sideW - 12, 18) && this.client != null) {
            this.client.setScreen(new AzyronCosmeticsScreen(this));
            return true;
        }

        // Header Back or Search focus
        int headX = wx + sideW;
        int headW = winW - sideW;
        if (editingModule != null) {
            if (btn == 0 && UiRenderUtils.isHovered(mx, my, headX + 8, wy + 7, 54, 18)) {
                editingModule = null;
                scrollOffset = 0.0;
                return true;
            }
        } else if (activeSection.category != null) {
            int sbW = Math.min(186, headW - 92);
            searchFocused = UiRenderUtils.isHovered(mx, my, headX + 8, wy + 7, sbW, 18);
            if (searchFocused && btn == 1) {
                searchQuery = "";
                return true;
            }
        }

        int contentX = wx + sideW + 8;
        int contentY = wy + 34;
        int contentW = winW - sideW - 16;
        int contentH = winH - 42;

        if (!UiRenderUtils.isHovered(mx, my, contentX, contentY, contentW, contentH)) {
            return false;
        }

        if (editingModule != null) {
            return handleSettingsClick(mx, my, btn, contentX, contentY, contentW);
        }
        if (activeSection == Section.PROFILES) {
            return handleProfilesClick(mx, my, btn, contentX, contentY, contentW);
        }
        if (activeSection == Section.CLIENT_SETTINGS) {
            return handleClientSettingsClick(mx, my, btn, contentX, contentY, contentW);
        }
        return handleModuleGridClick(mx, my, btn, contentX, contentY, contentW);
    }

    private boolean handleModuleGridClick(double mx, double my, int btn, int x, int y, int w) {
        List<AzyronModule> mods = getFilteredModulesCached();
        int cols = w >= 290 ? 2 : 1;
        int gap = 6;
        int cardW = (w - gap * (cols - 1)) / cols;
        int cardH = 44;

        for (int i = 0; i < mods.size(); i++) {
            AzyronModule mod = mods.get(i);
            int cx = x + (i % cols) * (cardW + gap);
            int cy = y + (i / cols) * (cardH + gap) - (int) Math.round(scrollOffset);

            if (UiRenderUtils.isHovered(mx, my, cx, cy, cardW, cardH)) {
                if (btn == 1 || UiRenderUtils.isHovered(mx, my, cx + 8, cy + 28, 54, 12)) {
                    openModuleSettings(mod);
                } else if (btn == 0) {
                    mod.toggle();
                    AzyronClient.getInstance().getConfig().save();
                }
                return true;
            }
        }
        return false;
    }

    private boolean handleSettingsClick(double mx, double my, int btn, int x, int y, int w) {
        AzyronModule mod = editingModule;
        int curY = y - (int) Math.round(scrollOffset);

        if (UiRenderUtils.isHovered(mx, my, x, curY, w, 28)) {
            mod.toggle();
            AzyronClient.getInstance().getConfig().save();
            return true;
        }
        curY += 34;

        if (mod instanceof HudModule hud) {
            int barX = x + w - 124;
            int barW = 112;
            if (UiRenderUtils.isHovered(mx, my, barX - 4, curY + 4, barW + 8, 18)) {
                draggingHudScale = true;
                sliderTrackX = barX;
                sliderTrackW = barW;
                updateHudScaleFromMouse(hud, mx);
                return true;
            }
            curY += 30;
        }

        for (Setting<?> setting : mod.getSettings()) {
            if (UiRenderUtils.isHovered(mx, my, x, curY, w, 26)) {
                if (setting instanceof Setting.BooleanSetting bs) {
                    bs.toggle();
                    AzyronClient.getInstance().getConfig().save();
                    return true;
                } else if (setting instanceof Setting.ModeSetting ms) {
                    if (btn == 1) ms.cyclePrev(); else ms.cycleNext();
                    AzyronClient.getInstance().getConfig().save();
                    return true;
                } else if (setting instanceof Setting.NumberSetting ns) {
                    int barX = x + w - 120;
                    int barW = 108;
                    if (UiRenderUtils.isHovered(mx, my, barX - 6, curY + 3, barW + 12, 20)) {
                        draggingSlider = ns;
                        sliderTrackX = barX;
                        sliderTrackW = barW;
                        ns.setFromNormalized((float) ((mx - barX) / barW));
                        return true;
                    }
                }
            }
            curY += 30;
        }

        if (UiRenderUtils.isHovered(mx, my, x, curY, 118, 20)) {
            mod.resetSettings();
            AzyronClient.getInstance().getConfig().save();
            return true;
        }
        return false;
    }

    private boolean handleProfilesClick(double mx, double my, int btn, int x, int y, int w) {
        if (btn != 0) return false;
        if (UiRenderUtils.isHovered(mx, my, x + w - 80, y + 5, 74, 18)) {
            MemoryOptimizer.requestSafeMemoryTrim(true);
            return true;
        }
        int py = y + 34;
        for (PerformanceProfile p : PerformanceProfile.values()) {
            if (UiRenderUtils.isHovered(mx, my, x, py, w, 46)) {
                AzyronClient.getInstance().getPerformanceEngine().applyProfile(p, this.client, true);
                return true;
            }
            py += 50;
        }
        return false;
    }

    private boolean handleClientSettingsClick(double mx, double my, int btn, int x, int y, int w) {
        if (btn != 0 && btn != 1) return false;
        AzyronConfig cfg = AzyronClient.getInstance().getConfig();
        if (btn == 0 && UiRenderUtils.isHovered(mx, my, x, y, w, 29)) {
            cfg.setCustomMainMenu(!cfg.isCustomMainMenu());
            cfg.save();
            return true;
        }
        if (btn == 0 && UiRenderUtils.isHovered(mx, my, x, y + 33, w, 29)) {
            cfg.setCustomLoadingScreen(!cfg.isCustomLoadingScreen());
            cfg.save();
            return true;
        }
        if (btn == 0 && UiRenderUtils.isHovered(mx, my, x, y + 66, w, 29)) {
            cfg.setAutoMemoryOptimization(!cfg.isAutoMemoryOptimization());
            cfg.save();
            return true;
        }
        if (UiRenderUtils.isHovered(mx, my, x, y + 102, w, 34)) {
            cfg.cycleSelectedMcVersion();
            cfg.save();
            return true;
        }
        return false;
    }

    private void updateHudScaleFromMouse(HudModule hud, double mx) {
        float t = (float) Math.max(0.0, Math.min(1.0, (mx - sliderTrackX) / sliderTrackW));
        hud.setScale(0.5f + t * 1.5f);
    }

    @Override
    public boolean azyronMouseDragged(double mx, double my, int button, double offsetX, double offsetY) {
        if (draggingSlider != null) {
            draggingSlider.setFromNormalized((float) ((mx - sliderTrackX) / sliderTrackW));
            return true;
        }
        if (draggingHudScale && editingModule instanceof HudModule hud) {
            updateHudScaleFromMouse(hud, mx);
            return true;
        }
        return false;
    }

    @Override
    public boolean azyronMouseReleased(double mx, double my, int button) {
        if (draggingSlider != null || draggingHudScale) {
            draggingSlider = null;
            draggingHudScale = false;
            AzyronClient.getInstance().getConfig().save();
            return true;
        }
        return false;
    }

    @Override
    public boolean azyronMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (verticalAmount != 0.0 && maxScroll > 0) {
            scrollOffset = Math.max(0.0, Math.min(maxScroll, scrollOffset - verticalAmount * 18.0));
            return true;
        }
        return false;
    }

    @Override
    public boolean azyronKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !searchQuery.isEmpty()) {
                searchQuery = searchQuery.substring(0, searchQuery.length() - 1);
                scrollOffset = 0.0;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                searchFocused = false;
                return true;
            }
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            if (editingModule != null) {
                editingModule = null;
                return true;
            }
            close();
            return true;
        }
        return super.azyronKeyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean azyronCharTyped(char chr, int modifiers) {
        if (searchFocused && chr >= 32 && chr != 127 && searchQuery.length() < 26) {
            searchQuery += chr;
            scrollOffset = 0.0;
            return true;
        }
        return false;
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
