package com.jigneshgaming.azyron.module;

import com.jigneshgaming.azyron.AzyronClient;
import com.jigneshgaming.azyron.cosmetics.CosmeticsManager;
import com.jigneshgaming.azyron.module.impl.ArmorHudModule;
import com.jigneshgaming.azyron.module.impl.CoordinatesModule;
import com.jigneshgaming.azyron.module.impl.CpsModule;
import com.jigneshgaming.azyron.module.impl.CrosshairModule;
import com.jigneshgaming.azyron.module.impl.FpsModule;
import com.jigneshgaming.azyron.module.impl.KeystrokesModule;
import com.jigneshgaming.azyron.module.impl.MemoryHudModule;
import com.jigneshgaming.azyron.module.impl.PingModule;
import com.jigneshgaming.azyron.module.impl.PotionHudModule;
import com.jigneshgaming.azyron.module.impl.ToggleSprintModule;
import com.jigneshgaming.azyron.module.impl.ZoomModule;
import com.jigneshgaming.azyron.profile.FrameTimeTracker;
import com.jigneshgaming.azyron.ui.HudEditorScreen;
import com.jigneshgaming.azyron.util.AzyronTheme;
import com.jigneshgaming.azyron.util.UiRenderUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Registry and lifecycle dispatcher for all AZYRON CLIENT modules.
 */
public final class ModuleManager {
    private final List<AzyronModule> modules = new ArrayList<>();
    private final List<HudModule> hudModules = new ArrayList<>();

    private final FpsModule fpsModule;
    private final CpsModule cpsModule;
    private final PingModule pingModule;
    private final CoordinatesModule coordinatesModule;
    private final KeystrokesModule keystrokesModule;
    private final ArmorHudModule armorHudModule;
    private final PotionHudModule potionHudModule;
    private final ToggleSprintModule toggleSprintModule;
    private final ZoomModule zoomModule;
    private final CrosshairModule crosshairModule;
    private final MemoryHudModule memoryHudModule;

    public ModuleManager() {
        this.fpsModule = register(new FpsModule());
        this.cpsModule = register(new CpsModule());
        this.pingModule = register(new PingModule());
        this.coordinatesModule = register(new CoordinatesModule());
        this.keystrokesModule = register(new KeystrokesModule());
        this.armorHudModule = register(new ArmorHudModule());
        this.potionHudModule = register(new PotionHudModule());
        this.toggleSprintModule = register(new ToggleSprintModule());
        this.zoomModule = register(new ZoomModule());
        this.crosshairModule = register(new CrosshairModule());
        this.memoryHudModule = register(new MemoryHudModule());
    }

    private <M extends AzyronModule> M register(M module) {
        modules.add(module);
        if (module instanceof HudModule hud) {
            hudModules.add(hud);
        }
        return module;
    }

    public List<AzyronModule> getModules() {
        return Collections.unmodifiableList(modules);
    }

    public List<HudModule> getHudModules() {
        return Collections.unmodifiableList(hudModules);
    }

    public List<AzyronModule> filterModules(ModuleCategory category, String searchQuery) {
        String q = searchQuery == null ? "" : searchQuery.trim().toLowerCase(Locale.ROOT);
        List<AzyronModule> out = new ArrayList<>();
        for (AzyronModule m : modules) {
            boolean catMatch = (category == null || category == ModuleCategory.ALL || m.getCategory() == category);
            boolean textMatch = q.isEmpty()
                    || m.getName().toLowerCase(Locale.ROOT).contains(q)
                    || m.getDescription().toLowerCase(Locale.ROOT).contains(q);
            if (catMatch && textMatch) {
                out.add(m);
            }
        }
        return out;
    }

    public AzyronModule getById(String id) {
        for (AzyronModule m : modules) {
            if (m.getId().equalsIgnoreCase(id)) {
                return m;
            }
        }
        return null;
    }

    public void onTick(MinecraftClient client) {
        for (AzyronModule module : modules) {
            module.onTick(client);
        }
    }

    public void renderHudOverlay(DrawContext context, MinecraftClient client) {
        FrameTimeTracker.onFrameRendered();
        if (client == null || client.options == null || client.options.hudHidden) {
            return;
        }
        if (client.currentScreen instanceof HudEditorScreen) {
            // HudEditorScreen renders widgets directly with selection/snap overlays
            return;
        }

        // Top-left subtle AZYRON CLIENT watermark + equipped cosmetic badge
        renderClientWatermark(context, client);

        for (HudModule hud : hudModules) {
            if (hud.isEnabled()) {
                hud.renderOnHud(context, client, false);
            }
        }

        // Render active profile switch notification toast if present
        if (AzyronClient.getInstance() != null && AzyronClient.getInstance().getPerformanceEngine() != null) {
            String banner = AzyronClient.getInstance().getPerformanceEngine().getActiveBannerMessage();
            if (!banner.isEmpty()) {
                int bw = client.textRenderer.getWidth(banner) + 24;
                int bx = (context.getScaledWindowWidth() - bw) / 2;
                int by = 14;
                UiRenderUtils.drawAccentCard(context, bx, by, bw, 20, AzyronTheme.BG_PANEL, false, true);
                context.drawCenteredTextWithShadow(client.textRenderer, banner, bx + bw / 2, by + 6, AzyronTheme.TEXT_PRIMARY);
            }
        }
    }

    private void renderClientWatermark(DrawContext context, MinecraftClient client) {
        AzyronClient inst = AzyronClient.getInstance();
        String badge = "";
        if (inst != null && inst.getCosmeticsManager() != null) {
            CosmeticsManager.BadgeStyle bs = inst.getCosmeticsManager().getActiveBadge();
            if (bs != CosmeticsManager.BadgeStyle.NONE) {
                badge = " " + bs.getTag();
            }
        }
        String profileTag = inst != null && inst.getPerformanceEngine() != null
                ? inst.getPerformanceEngine().getActiveProfile().name()
                : "BALANCED";
        String label = "AZYRON 1.21.11 • " + profileTag + badge;
        int w = client.textRenderer.getWidth(label) + 14;
        int x = context.getScaledWindowWidth() - w - 6;
        int y = 6;
        UiRenderUtils.drawRoundedPanel(context, x, y, w, 16, 0xB80B0D1A, AzyronTheme.getAnimatedAccent(0.15f));
        UiRenderUtils.drawHorizontalGradient(context, x + 2, y, w - 4, 2, AzyronTheme.ACCENT_PURPLE_BRIGHT, AzyronTheme.ACCENT_ELECTRIC_BLUE);
        context.drawTextWithShadow(client.textRenderer, label, x + 7, y + 4, AzyronTheme.TEXT_PRIMARY);
    }

    public void resetAllHudPositions() {
        for (HudModule hud : hudModules) {
            hud.resetPosition();
        }
    }

    public FpsModule getFpsModule() { return fpsModule; }
    public CpsModule getCpsModule() { return cpsModule; }
    public PingModule getPingModule() { return pingModule; }
    public CoordinatesModule getCoordinatesModule() { return coordinatesModule; }
    public KeystrokesModule getKeystrokesModule() { return keystrokesModule; }
    public ArmorHudModule getArmorHudModule() { return armorHudModule; }
    public PotionHudModule getPotionHudModule() { return potionHudModule; }
    public ToggleSprintModule getToggleSprintModule() { return toggleSprintModule; }
    public ZoomModule getZoomModule() { return zoomModule; }
    public CrosshairModule getCrosshairModule() { return crosshairModule; }
    public MemoryHudModule getMemoryHudModule() { return memoryHudModule; }
}
