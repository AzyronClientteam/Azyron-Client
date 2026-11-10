package com.jigneshgaming.azyron.mixin;

import com.jigneshgaming.azyron.ui.AzyronBaseScreen;
import com.jigneshgaming.azyron.ui.AzyronInteractiveScreen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Minecraft 1.21.11 input adapter merged into AzyronBaseScreen by AzyronMixinPlugin.
 * Forwards 1.21.11 Click, KeyInput, and CharInput records into AzyronInteractiveScreen
 * while keeping AzyronBaseScreen 100% free of 1.21-only class references on Minecraft 1.20.1.
 */
@Mixin(AzyronBaseScreen.class)
public abstract class Screen121Mixin extends Screen implements AzyronInteractiveScreen {

    protected Screen121Mixin(Text title) {
        super(title);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (click != null && this.azyronMouseClicked(click.x(), click.y(), click.button())) {
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (click != null && this.azyronMouseDragged(click.x(), click.y(), click.button(), offsetX, offsetY)) {
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (click != null && this.azyronMouseReleased(click.x(), click.y(), click.button())) {
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input != null && this.azyronKeyPressed(input.getKeycode(), 0, 0)) {
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (input != null && input.isValidChar()) {
            String str = input.asString();
            if (str != null && !str.isEmpty() && this.azyronCharTyped(str.charAt(0), 0)) {
                return true;
            }
        }
        return super.charTyped(input);
    }
}
