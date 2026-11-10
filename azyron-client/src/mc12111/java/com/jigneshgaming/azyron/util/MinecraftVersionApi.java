package com.jigneshgaming.azyron.util;

import com.jigneshgaming.azyron.profile.PerformanceProfile.ParticleQuality;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.option.GameOptions;
import net.minecraft.item.ItemStack;
import net.minecraft.client.session.Session;
import net.minecraft.particle.ParticlesMode;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.world.LevelLoadingScreen;

/** Native API adapter compiled and remapped against this exact Minecraft target.
 * No reflection, fake Minecraft classes, or per-frame allocation is needed here.
 */
public final class MinecraftVersionApi {
    private MinecraftVersionApi() { }
    public static void setParticles(GameOptions options, ParticleQuality quality) {
        options.getParticles().setValue(switch (quality) {
            case ALL -> ParticlesMode.ALL;
            case DECREASED -> ParticlesMode.DECREASED;
            case MINIMAL -> ParticlesMode.MINIMAL;
        });
    }
    public static boolean isLevelLoadingScreen(Screen screen) {
        return screen instanceof LevelLoadingScreen;
    }
    public static boolean isLoadingOrConnectingScreen(Screen screen) {
        return screen instanceof LevelLoadingScreen || screen instanceof ConnectScreen;
    }
    public static void drawStackOverlay(DrawContext context, TextRenderer font, ItemStack stack, int x, int y) {
        context.drawStackOverlay(font, stack, x, y);
    }
    public static boolean isOfflineOrLocalSession(Object candidate) {
        if (candidate == null) return true;
        // Unknown session objects must not suppress online authentication.
        if (!(candidate instanceof Session session)) return false;
        return PlatformCompat.isOfflineOrLocalToken(session.getAccessToken(), session.getXuid());
    }
}
