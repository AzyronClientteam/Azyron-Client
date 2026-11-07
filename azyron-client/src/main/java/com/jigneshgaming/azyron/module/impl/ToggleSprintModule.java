package com.jigneshgaming.azyron.module.impl;

import com.jigneshgaming.azyron.module.HudModule;
import com.jigneshgaming.azyron.module.Setting;
import com.jigneshgaming.azyron.util.AzyronTheme;
import com.jigneshgaming.azyron.util.UiRenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Toggle Sprint gameplay mechanic + optional movable HUD status badge.
 */
public final class ToggleSprintModule extends HudModule {
    private final Setting.BooleanSetting showStatusHud;
    private boolean sprintToggled = true;

    public ToggleSprintModule() {
        super("toggle_sprint", "Toggle Sprint", "Automatically maintains sprint without holding Sprint key", true, 0.012f, 0.92f);
        this.showStatusHud = addSetting(new Setting.BooleanSetting(
                "show_hud", "Show Status Text", "Display [Sprinting (Toggled)] indicator on HUD", true
        ));
    }

    public boolean isSprintToggled() {
        return sprintToggled;
    }

    public void toggleSprintState() {
        this.sprintToggled = !this.sprintToggled;
        if (!this.sprintToggled) {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null && mc.options != null) {
                mc.options.sprintKey.setPressed(false);
            }
        }
    }

    @Override
    protected void onDisable() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.options != null) {
            mc.options.sprintKey.setPressed(false);
        }
    }

    @Override
    public void onTick(MinecraftClient client) {
        if (!isEnabled() || client == null || client.options == null || client.player == null) {
            return;
        }
        if (sprintToggled && client.currentScreen == null && !client.options.sneakKey.isPressed()) {
            client.options.sprintKey.setPressed(true);
        }
    }

    @Override
    protected void renderWidget(DrawContext context, MinecraftClient client, boolean editorPreview) {
        if (!showStatusHud.getValue() && !editorPreview) {
            return;
        }
        String status;
        if (client != null && client.options != null && client.options.sneakKey.isPressed()) {
            status = "[Sneaking (Key Held)]";
        } else if (sprintToggled) {
            status = "[Sprinting (Toggled)]";
        } else if (client != null && client.options != null && client.options.sprintKey.isPressed()) {
            status = "[Sprinting (Key Held)]";
        } else {
            status = "[Walking]";
        }

        if (!editorPreview && "[Walking]".equals(status)) {
            return;
        }

        this.width = Math.max(96, client.textRenderer.getWidth(status) + 14);
        this.height = 18;
        UiRenderUtils.drawHudCard(context, 0, 0, width, height, showBackground.getValue(), bgOpacity.getIntValue(), resolveAccentColor());
        context.drawTextWithShadow(client.textRenderer, status, 7, 5, AzyronTheme.TEXT_PRIMARY);
    }
}
