package com.jigneshgaming.azyron.module.impl;

import com.jigneshgaming.azyron.module.HudModule;
import com.jigneshgaming.azyron.module.Setting;
import com.jigneshgaming.azyron.util.AzyronTheme;
import com.jigneshgaming.azyron.util.UiRenderUtils;
import java.util.Locale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.Entity;

/**
 * Rich Coordinates HUD showing X/Y/Z, Cardinal & Axis Facing, Chunk XY, and Nether/Overworld conversion.
 */
public final class CoordinatesModule extends HudModule {
    private final Setting.BooleanSetting showDirection;
    private final Setting.BooleanSetting showChunkCoords;
    private final Setting.BooleanSetting showNetherCoords;
    private final Setting.ModeSetting layoutMode;

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
            if (client.world != null && client.world.getRegistryKey() != null) {
                inNether = "the_nether".equals(client.world.getRegistryKey().getValue().getPath());
            }
        }

        int bx = (int) Math.floor(x);
        int by = (int) Math.floor(y);
        int bz = (int) Math.floor(z);
        String facing = formatFacing(yaw);

        if ("Compact".equalsIgnoreCase(layoutMode.getValue())) {
            String line = String.format(Locale.ROOT, "XYZ: %d, %d, %d", bx, by, bz)
                    + (showDirection.getValue() ? ("  [" + facing + "]") : "");
            this.width = Math.max(120, client.textRenderer.getWidth(line) + 14);
            this.height = 18;
            UiRenderUtils.drawHudCard(context, 0, 0, width, height, showBackground.getValue(), bgOpacity.getIntValue(), resolveAccentColor());
            context.drawTextWithShadow(client.textRenderer, line, 7, 5, AzyronTheme.TEXT_PRIMARY);
            return;
        }

        int lines = 3;
        if (showChunkCoords.getValue()) lines++;
        if (showNetherCoords.getValue()) lines++;

        String lineX = "X: " + bx + (showDirection.getValue() ? ("   " + facing) : "");
        String lineY = "Y: " + by;
        String lineZ = "Z: " + bz;
        String lineChunk = "Chunk: " + (bx >> 4) + ", " + (bz >> 4);
        String lineDim = inNether
                ? ("OW: " + (bx * 8) + ", " + (bz * 8))
                : ("Nether: " + (bx / 8) + ", " + (bz / 8));

        int maxW = Math.max(client.textRenderer.getWidth(lineX), client.textRenderer.getWidth(lineZ));
        if (showChunkCoords.getValue()) maxW = Math.max(maxW, client.textRenderer.getWidth(lineChunk));
        if (showNetherCoords.getValue()) maxW = Math.max(maxW, client.textRenderer.getWidth(lineDim));

        this.width = Math.max(114, maxW + 16);
        this.height = 8 + lines * 11;

        int accent = resolveAccentColor();
        UiRenderUtils.drawHudCard(context, 0, 0, width, height, showBackground.getValue(), bgOpacity.getIntValue(), accent);

        int ty = 5;
        context.drawTextWithShadow(client.textRenderer, lineX, 7, ty, AzyronTheme.TEXT_PRIMARY);
        ty += 11;
        context.drawTextWithShadow(client.textRenderer, lineY, 7, ty, AzyronTheme.TEXT_PRIMARY);
        ty += 11;
        context.drawTextWithShadow(client.textRenderer, lineZ, 7, ty, AzyronTheme.TEXT_PRIMARY);
        ty += 11;
        if (showChunkCoords.getValue()) {
            context.drawTextWithShadow(client.textRenderer, lineChunk, 7, ty, AzyronTheme.TEXT_SECONDARY);
            ty += 11;
        }
        if (showNetherCoords.getValue()) {
            context.drawTextWithShadow(client.textRenderer, lineDim, 7, ty, AzyronTheme.ACCENT_ELECTRIC_BLUE);
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
