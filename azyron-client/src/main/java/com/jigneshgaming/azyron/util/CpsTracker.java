package com.jigneshgaming.azyron.util;

/**
 * Zero-allocation rolling 1000ms CPS (Clicks Per Second) tracker using primitive long[] ring buffers.
 * Eliminates Long autoboxing during rapid PvP clicking while supporting both MouseMixin hardware events
 * and non-duplicating Mouse/KeyBinding edge fallbacks on desktop and PojavLauncher.
 */
public final class CpsTracker {
    private static final long WINDOW_MS = 1000L;
    private static final int RING_CAPACITY = 256;
    private static final int RING_MASK = RING_CAPACITY - 1;

    private static final long[] LEFT_RING = new long[RING_CAPACITY];
    private static int leftHead = 0;
    private static int leftTail = 0;

    private static final long[] RIGHT_RING = new long[RING_CAPACITY];
    private static int rightHead = 0;
    private static int rightTail = 0;

    private static volatile boolean mixinHookActive = false;
    private static boolean prevMouseLeftDown = false;
    private static boolean prevMouseRightDown = false;
    private static boolean prevAttackKeyDown = false;
    private static boolean prevUseKeyDown = false;
    private static long lastHardwareLeftMs = 0L;
    private static long lastHardwareRightMs = 0L;

    private CpsTracker() {
    }

    public static void markMixinHookActive() {
        mixinHookActive = true;
    }

    public static boolean isMixinHookActive() {
        return mixinHookActive;
    }

    public static synchronized void recordLeftClick() {
        long now = System.currentTimeMillis();
        lastHardwareLeftMs = now;
        pruneLeft(now);
        pushLeft(now);
    }

    public static synchronized void recordRightClick() {
        long now = System.currentTimeMillis();
        lastHardwareRightMs = now;
        pruneRight(now);
        pushRight(now);
    }

    /**
     * Captures PojavLauncher touch/controller keybind presses when not already emitted via Mouse.onMouseButton.
     */
    public static synchronized void pollKeybindEdges(boolean attackDown, boolean useDown) {
        long now = System.currentTimeMillis();
        if (attackDown && !prevAttackKeyDown && (now - lastHardwareLeftMs) > 25L) {
            pruneLeft(now);
            pushLeft(now);
        }
        if (useDown && !prevUseKeyDown && (now - lastHardwareRightMs) > 25L) {
            pruneRight(now);
            pushRight(now);
        }
        prevAttackKeyDown = attackDown;
        prevUseKeyDown = useDown;
    }

    /**
     * Safe fallback polling for Mouse button state + KeyBindings when MouseMixin is unavailable.
     */
    public static synchronized void pollFallbackInputs(
            boolean mouseLeftDown,
            boolean mouseRightDown,
            boolean attackDown,
            boolean useDown
    ) {
        if (!mixinHookActive) {
            long now = System.currentTimeMillis();
            if (mouseLeftDown && !prevMouseLeftDown && (now - lastHardwareLeftMs) > 25L) {
                lastHardwareLeftMs = now;
                pruneLeft(now);
                pushLeft(now);
            }
            if (mouseRightDown && !prevMouseRightDown && (now - lastHardwareRightMs) > 25L) {
                lastHardwareRightMs = now;
                pruneRight(now);
                pushRight(now);
            }
        }
        prevMouseLeftDown = mouseLeftDown;
        prevMouseRightDown = mouseRightDown;
        pollKeybindEdges(attackDown, useDown);
    }

    public static synchronized int getLeftCps() {
        pruneLeft(System.currentTimeMillis());
        return (leftTail - leftHead) & RING_MASK;
    }

    public static synchronized int getRightCps() {
        pruneRight(System.currentTimeMillis());
        return (rightTail - rightHead) & RING_MASK;
    }

    public static synchronized void reset() {
        leftHead = 0;
        leftTail = 0;
        rightHead = 0;
        rightTail = 0;
        mixinHookActive = false;
        prevMouseLeftDown = false;
        prevMouseRightDown = false;
        prevAttackKeyDown = false;
        prevUseKeyDown = false;
        lastHardwareLeftMs = 0L;
        lastHardwareRightMs = 0L;
    }

    private static void pushLeft(long now) {
        int nextTail = (leftTail + 1) & RING_MASK;
        if (nextTail == leftHead) {
            leftHead = (leftHead + 1) & RING_MASK;
        }
        LEFT_RING[leftTail] = now;
        leftTail = nextTail;
    }

    private static void pushRight(long now) {
        int nextTail = (rightTail + 1) & RING_MASK;
        if (nextTail == rightHead) {
            rightHead = (rightHead + 1) & RING_MASK;
        }
        RIGHT_RING[rightTail] = now;
        rightTail = nextTail;
    }

    private static void pruneLeft(long now) {
        while (leftHead != leftTail && (now - LEFT_RING[leftHead]) > WINDOW_MS) {
            leftHead = (leftHead + 1) & RING_MASK;
        }
    }

    private static void pruneRight(long now) {
        while (rightHead != rightTail && (now - RIGHT_RING[rightHead]) > WINDOW_MS) {
            rightHead = (rightHead + 1) & RING_MASK;
        }
    }
}
