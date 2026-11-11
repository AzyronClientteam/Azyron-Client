package com.jigneshgaming.azyron.cosmetics;

import com.jigneshgaming.azyron.util.AzyronTheme;

/**
 * Manages AZYRON CLIENT cosmetic Capes, Animated Halos/Auras, and Nametag/HUD Badges.
 */
public final class CosmeticsManager {
    public enum CapeStyle {
        AZYRON_NEBULA("Azyron Nebula", "Dual-tone purple & electric-blue cosmic cloak", AzyronTheme.ACCENT_PURPLE_BRIGHT, AzyronTheme.ACCENT_ELECTRIC_BLUE),
        CYBER_GRID("Electric Cyber", "Neon cyan & obsidian competitive grid cloak", AzyronTheme.ACCENT_ELECTRIC_BLUE, 0xFF0F172A),
        OBSIDIAN_CROWN("Obsidian Crown", "Royal violet & gold Jignesh Gaming prestige cloak", 0xFF7E22CE, 0xFFF59E0B),
        CRIMSON_PULSE("Crimson Pulse", "High-contrast dark crimson & magenta PvP cloak", 0xFFEF4444, AzyronTheme.ACCENT_MAGENTA),
        NONE("None", "Disable custom client cape", 0xFF334155, 0xFF1E293B);

        private final String displayName;
        private final String description;
        private final int primaryColor;
        private final int secondaryColor;

        CapeStyle(String displayName, String description, int primaryColor, int secondaryColor) {
            this.displayName = displayName;
            this.description = description;
            this.primaryColor = primaryColor;
            this.secondaryColor = secondaryColor;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public int getPrimaryColor() { return primaryColor; }
        public int getSecondaryColor() { return secondaryColor; }
    }

    public enum HaloStyle {
        AZYRON_HALO("Azyron Halo", "Floating purple-cyan energy ring", AzyronTheme.ACCENT_ELECTRIC_BLUE),
        STARLIGHT_ORBIT("Starlight Orbit", "Dual orbiting electric particles", AzyronTheme.ACCENT_PURPLE_BRIGHT),
        CROWN_AURA("Champion Crown", "Golden prestige aura", 0xFFF59E0B),
        NONE("None", "Disable halo cosmetic", AzyronTheme.TEXT_MUTED);

        private final String displayName;
        private final String description;
        private final int color;

        HaloStyle(String displayName, String description, int color) {
            this.displayName = displayName;
            this.description = description;
            this.color = color;
        }

        public String getDisplayName() { return displayName; }
        public String getDescription() { return description; }
        public int getColor() { return color; }
    }

    public enum BadgeStyle {
        AZYRON_BOLT("[AZYRON]", "Signature Azyron Client lightning tag"),
        PRO_BOOST("[FPS+]", "Performance Boost verified tag"),
        JIGNESH_VIP("[JG VIP]", "Jignesh Gaming supporter tag"),
        NONE("", "Hide client badge");

        private final String tag;
        private final String description;

        BadgeStyle(String tag, String description) {
            this.tag = tag;
            this.description = description;
        }

        public String getTag() { return tag; }
        public String getDescription() { return description; }
    }

    private CapeStyle activeCape = CapeStyle.AZYRON_NEBULA;
    private HaloStyle activeHalo = HaloStyle.AZYRON_HALO;
    private BadgeStyle activeBadge = BadgeStyle.AZYRON_BOLT;

    public CapeStyle getActiveCape() { return activeCape; }
    public void setActiveCape(CapeStyle activeCape) { if (activeCape != null) this.activeCape = activeCape; }

    public HaloStyle getActiveHalo() { return activeHalo; }
    public void setActiveHalo(HaloStyle activeHalo) { if (activeHalo != null) this.activeHalo = activeHalo; }

    public BadgeStyle getActiveBadge() { return activeBadge; }
    public void setActiveBadge(BadgeStyle activeBadge) { if (activeBadge != null) this.activeBadge = activeBadge; }
}
