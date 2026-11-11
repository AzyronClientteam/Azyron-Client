package com.jigneshgaming.azyron.ui;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Shared dual-version base Screen for AZYRON CLIENT (Minecraft 1.20.1 + 1.21.11).
 *
 *  - Directly implements Minecraft 1.20.1 Element input methods (remapped to method_25400..25406
 *    with 100% primitive JVM descriptors) so zero 1.21-only classes are referenced on 1.20.1.
 *  - Directly implements Minecraft 1.21.11 Element.mouseScrolled(double, double, double, double).
 *  - Paired with Screen121Mixin on Minecraft 1.21.11 (selected by AzyronMixinPlugin) to forward
 *    Click, KeyInput, and CharInput events into AzyronInteractiveScreen.
 */
public abstract class AzyronBaseScreen extends Screen implements AzyronInteractiveScreen {

    protected AzyronBaseScreen(Text title) {
        super(title);
    }

    @Override
    public boolean azyronKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && this.shouldCloseOnEsc()) {
            this.close();
            return true;
        }
        return false;
    }

    /** Minecraft 1.20.1 Element.mouseClicked(double, double, int) -> method_25402(DDI)Z */
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return azyronMouseClicked(mouseX, mouseY, button);
    }

    /** Minecraft 1.20.1 Element.mouseDragged(double, double, int, double, double) -> method_25403(DDIDD)Z */
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return azyronMouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    /** Minecraft 1.20.1 Element.mouseReleased(double, double, int) -> method_25406(DDI)Z */
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return azyronMouseReleased(mouseX, mouseY, button);
    }

    /** Minecraft 1.20.1 Element.mouseScrolled(double, double, double) -> method_25401(DDD)Z */
    public boolean mouseScrolled(double mouseX, double mouseY, double verticalAmount) {
        return azyronMouseScrolled(mouseX, mouseY, 0.0, verticalAmount);
    }

    /** Minecraft 1.21.11 Element.mouseScrolled(double, double, double, double) -> method_25401(DDDD)Z */
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return azyronMouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    /** Minecraft 1.20.1 Element.keyPressed(int, int, int) -> method_25404(III)Z */
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return azyronKeyPressed(keyCode, scanCode, modifiers);
    }

    /** Minecraft 1.20.1 Element.charTyped(char, int) -> method_25400(CI)Z */
    public boolean charTyped(char chr, int modifiers) {
        return azyronCharTyped(chr, modifiers);
    }
}
