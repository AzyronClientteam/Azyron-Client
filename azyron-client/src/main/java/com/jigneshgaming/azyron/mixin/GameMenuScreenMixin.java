package com.jigneshgaming.azyron.mixin;

import com.jigneshgaming.azyron.ui.AzyronModsScreen;
import com.jigneshgaming.azyron.ui.HudEditorScreen;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds quick-access AZYRON Client & HUD Editor buttons to the Escape Pause Menu
 * so both PC and PojavLauncher touch players can open AZYRON anytime in-game.
 */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "initWidgets", at = @At("TAIL"), require = 0)
    private void azyron$addPauseButtons(CallbackInfo ci) {
        try {
            this.addDrawableChild(
                    ButtonWidget.builder(Text.literal("✦ AZYRON Client"), btn -> {
                        if (this.client != null) {
                            this.client.setScreen(new AzyronModsScreen((GameMenuScreen) (Object) this));
                        }
                    }).dimensions(8, 8, 108, 20).build()
            );
            this.addDrawableChild(
                    ButtonWidget.builder(Text.literal("✦ HUD Editor"), btn -> {
                        if (this.client != null) {
                            this.client.setScreen(new HudEditorScreen((GameMenuScreen) (Object) this));
                        }
                    }).dimensions(120, 8, 88, 20).build()
            );
        } catch (Throwable ignored) {
        }
    }
}
