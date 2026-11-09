package com.jigneshgaming.azyron.module.impl;

import com.jigneshgaming.azyron.module.HudModule;
import com.jigneshgaming.azyron.module.Setting;
import com.jigneshgaming.azyron.util.AzyronTheme;
import com.jigneshgaming.azyron.util.UiRenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;

/**
 * Rich Coordinates HUD showing X/Y/Z, Cardinal & Axis Facing, Chunk XY, and Nether/Overworld conversion.
 * Caches formatted strings & text widths whenever the player remains within the same integer block & facing octant.
 */
public final class CoordinatesModule extends HudModule {
    private final Setting.BooleanSetting showDirection;
    private final Setting.BooleanSetting showChunkCoords;
    private final Setting.BooleanSetting showNetherCoords;
    private final Setting.ModeSetting layoutMode;

    private int lastBx = Integer.MIN_VALUE;
    private int lastBy = Integer.MIN_VALUE;
    private int lastBz = Integer.MIN_VALUE;
    private String lastFacing = "";
    private boolean lastInNether = false;
    private boolean lastShowDir = true;
    private boolean lastShowChunk = true;
    private boolean lastShowNether = false;
    private boolean lastCompact = false;

    private String cachedCompact = "";
    private String cachedLineX = "";
    private String cachedLineY = "";
    private String cachedLineZ = "";
    private String cachedLineChunk = "";
    private String cachedLineDim = "";
    private int cachedWidth = 114;
    private int cachedHeight = 41;

    public CoordinatesModule() {
        super("coordinates_hud", "Coordinates HUD", "Displays XYZ, facing direction, chunk & Nether coords", true, 0.012f, 0.178f);
        this.showDirection = addSetting(new Setting.BooleanSetting(
                "show_direction", "Show Direction", "Display cardinal compass & +/- axis hint", true
        ));
        this.showChunkCoords = addSetting(new Setting.BooleanSetting(
                "show_chunk", "Show Chunk XY", "Display current chunk coordinates", true
        ));
        this.showNetherCoords = addSetting(new Setting.BooleanSetting(
                "show_nether", "Nether/OW Ratio", "Show converted x8 or /8 dimension coordinates", false
        ));
        this.layoutMode = addSetting(new Setting.ModeSetting(
                "layout", "Layout Style", "Multi-line card or compact single line",
                "Multi-Line", "Multi-Line", "Compact"
        ));
    }

    public void clearWorldCache() {
        lastBx = Integer.MIN_VALUE;
        lastBy = Integer.MIN_VALUE;
        lastBz = Integer.MIN_VALUE;
        lastFacing = "";
    }

    @Override
    protected void renderWidget(DrawContext context, MinecraftClient client, boolean editorPreview) {
        double x = 128.0, y = 64.0, z = -256.0;
        float yaw = 180.0f;
        boolean inNether = false;

        if (client != null && client.player != null) {
            Entity p = client.player;
            x = p.getX();
            y = p.getY();
            z = p.getZ();
            yaw = p.getYaw();
            if (client.world != null && client.world.getRegistryKey() != null
                    && client.world.getRegistryKey().getValue() != null) {
                inNether = "the_nether".equals(client.world.getRegistryKey().getValue().getPath());
            }
        }

        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bz = (int) Math.floor(z);
        String facing = formatFacing(yaw);
        boolean showDir = showDirection.getValue();
        boolean showChunk = showChunkCoords.getValue();
        boolean showNether = showNetherCoords.getValue();
        boolean compact = "Compact".equalsIgnoreCase(layoutMode.getValue());

        if (bx != lastBx || by != lastBy || bz != lastBz || !facing.equals(lastFacing)
                || inNether != lastInNether || showDir != lastShowDir
                || showChunk != lastShowChunk || showNether != lastShowNether
                || compact != lastCompact) {
            lastBx = bx;
            lastBy = by;
            lastBz = bz;
            lastFacing = facing;
            lastInNether = inNether;
            lastShowDir = showDir;
            lastShowChunk = showChunk;
            lastShowNether = showNether;
            lastCompact = compact;

            if (compact) {
                StringBuilder sb = new StringBuilder(36);
                sb.append("XYZ: ").append(bx).append(", ").append(by).append(", ").append(bz);
                if (showDir) {
                    sb.append("  [").append(facing).append(']');
                }
                cachedCompact = sb.toString();
                cachedWidth = Math.max(120, client.textRenderer.getWidth(cachedCompact) + 14);
                cachedHeight = 18;
            } else {
                int lines = 3 + (showChunk ? 1 : 0) + (showNether ? 1 : 0);
                cachedLineX = showDir ? ("X: " + bx + "   " + facing) : ("X: " + bx);
                cachedLineY = "Y: " + by;
                cachedLineZ = "Z: " + bz;
                int maxW = Math.max(client.textRenderer.getWidth(cachedLineX), client.textRenderer.getWidth(cachedLineZ));
                if (showChunk) {
                    cachedLineChunk = "Chunk: " + (bx >> 4) + ", " + (bz >> 4);
                    maxW = Math.max(maxW, client.textRenderer.getWidth(cachedLineChunk));
                }
                if (showNether) {
                    cachedLineDim = inNether
                            ? ("OW: " + (bx * 8) + ", " + (bz * 8))
                            : ("Nether: " + (bx / 8) + ", " + (bz / 8));
                    maxW = Math.max(maxW, client.textRenderer.getWidth(cachedLineDim));
                }
                cachedWidth = Math.max(114, maxW + 16);
                cachedHeight = 8 + lines * 11;
            }
        }

        this.width = cachedWidth;
        this.height = cachedHeight;
        int accent = resolveAccentColor();
        UiRenderUtils.drawHudCard(context, 0, 0, width, height, showBackground.getValue(), bgOpacity.getIntValue(), accent);

        if (compact) {
            context.drawTextWithShadow(client.textRenderer, cachedCompact, 7, 5, AzyronTheme.TEXT_PRIMARY);
            return;
        }

        int ty = 5;
        context.drawTextWithShadow(client.textRenderer, cachedLineX, 7, ty, AzyronTheme.TEXT_PRIMARY);
        ty += 11;
        context.drawTextWithShadow(client.textRenderer, cachedLineY, 7, ty, AzyronTheme.TEXT_PRIMARY);
        ty += 11;
        context.drawTextWithShadow(client.textRenderer, cachedLineZ, 7, ty, AzyronTheme.TEXT_PRIMARY);
        ty += 11;
        if (showChunk) {
            context.drawTextWithShadow(client.textRenderer, cachedLineChunk, 7, ty, AzyronTheme.TEXT_SECONDARY);
            ty += 11;
        }
        if (showNether) {
            context.drawTextWithShadow(client.textRenderer, cachedLineDim, 7, ty, AzyronTheme.ACCENT_ELECTRIC_BLUE);
        }
    }

    public static String formatFacing(float yaw) {
        float deg = yaw % 360.0f;
        if (deg < 0.0f) {
            deg += 360.0f;
        }
        // Minecraft yaw: 0 = South (+Z), 90 = West (-X), 180 = North (-Z), 270 = East (+X)
        if (deg >= 337.5f || deg < 22.5f) return "S (+Z)";
        if (deg < 67.5f) return "SW (-X/+Z)";
        if (deg < 112.5f) return "W (-X)";
        if (deg < 157.5f) return "NW (-X/-Z)";
        if (deg < 202.5f) return "N (-Z)";
        if (deg < 247.5f) return "NE (+X/-Z)";
        if (deg < 292.5f) return "E (+X)";
        return "SE (+X/+Z)";
    }
}
