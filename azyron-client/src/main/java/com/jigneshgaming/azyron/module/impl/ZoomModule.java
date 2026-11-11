package com.jigneshgaming.azyron.module.impl;

import com.jigneshgaming.azyron.module.AzyronModule;
import com.jigneshgaming.azyron.module.ModuleCategory;
import com.jigneshgaming.azyron.module.Setting;

/**
 * Smooth animated Optic Zoom with Scroll-Wheel dynamic magnification (1.5x .. 25x).
 */
public final class ZoomModule extends AzyronModule {
    private final Setting.NumberSetting defaultZoomFactor;
    private final Setting.BooleanSetting smoothAnimation;
    private final Setting.BooleanSetting scrollToZoom;

    private boolean zooming = false;
    private double scrollMultiplier = 1.0;
    private float animatedDivisor = 1.0f;

    public ZoomModule() {
        super("zoom", "Zoom", "Smooth optical zoom (Hold C) with scroll-wheel magnification", ModuleCategory.GAMEPLAY, true);
        this.defaultZoomFactor = addSetting(new Setting.NumberSetting(
                "zoom_factor", "Zoom Level", "Base optical magnification factor", 4.0, 2.0, 12.0, 0.5
        ));
        this.smoothAnimation = addSetting(new Setting.BooleanSetting(
                "smooth_zoom", "Smooth Animation", "Interpolate FOV smoothly instead of snapping", true
        ));
        this.scrollToZoom = addSetting(new Setting.BooleanSetting(
                "scroll_zoom", "Scroll Magnification", "Adjust zoom magnification with mouse scroll wheel", true
        ));
    }

    public void setZooming(boolean active) {
        if (this.zooming != active) {
            this.zooming = active;
            if (!active) {
                this.scrollMultiplier = 1.0;
            }
        }
    }

    public boolean isActivelyZooming() {
        return isEnabled() && zooming;
    }

    public boolean onScroll(double verticalDelta) {
        if (!isActivelyZooming() || !scrollToZoom.getValue() || verticalDelta == 0.0) {
            return false;
        }
        if (verticalDelta > 0) {
            scrollMultiplier = Math.min(6.0, scrollMultiplier * 1.15);
        } else {
            scrollMultiplier = Math.max(0.4, scrollMultiplier / 1.15);
        }
        return true;
    }

    public float applyFov(float baseFov) {
        float targetDivisor = isActivelyZooming()
                ? (float) Math.max(1.5, Math.min(25.0, defaultZoomFactor.getValue() * scrollMultiplier))
                : 1.0f;

        if (!smoothAnimation.getValue()) {
            animatedDivisor = targetDivisor;
        } else {
            animatedDivisor += (targetDivisor - animatedDivisor) * 0.24f;
            if (Math.abs(targetDivisor - animatedDivisor) < 0.005f) {
                animatedDivisor = targetDivisor;
            }
        }

        if (animatedDivisor <= 1.005f) {
            return baseFov;
        }
        return Math.max(3.0f, baseFov / animatedDivisor);
    }
}
