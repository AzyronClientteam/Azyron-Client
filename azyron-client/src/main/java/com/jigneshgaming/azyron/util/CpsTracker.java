package com.jigneshgaming.azyron.util;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Rolling 1000ms CPS (Clicks Per Second) tracker for Left & Right Mouse / PojavLauncher Touch inputs.
 */
public final class CpsTracker {
    private static final long WINDOW_MS = 1000L;
    private static final Deque<Long> LEFT_CLICKS = new ArrayDeque<>(64);
    private static final Deque<Long> RIGHT_CLICKS = new ArrayDeque<>(64);

    private static boolean prevAttackKeyDown = false;
    private static boolean prevUseKeyDown = false;
    private static long lastHardwareLeftMs = 0L;
    private static long lastHardwareRightMs = 0L;

    private CpsTracker() {
    }

    public static synchronized void recordLeftClick() {
        long now = System.currentTimeMillis();
        lastHardwareLeftMs = now;
        prune(LEFT_CLICKS, now);
        LEFT_CLICKS.addLast(now);
    }

    public static synchronized void recordRightClick() {
        long now = System.currentTimeMillis();
        lastHardwareRightMs = now;
        prune(RIGHT_CLICKS, now);
        RIGHT_CLICKS.addLast(now);
    }

    /**
     * Captures PojavLauncher touch/controller keybind presses when not already emitted via Mouse.onMouseButton.
     */
    public static synchronized void pollKeybindEdges(boolean attackDown, boolean useDown) {
        long now = System.currentTimeMillis();
        if (attackDown && !prevAttackKeyDown && (now - lastHardwareLeftMs) > 25L) {
            prune(LEFT_CLICKS, now);
            LEFT_CLICKS.addLast(now);
        }
        if (useDown && !prevUseKeyDown && (now - lastHardwareRightMs) > 25L) {
            prune(RIGHT_CLICKS, now);
            RIGHT_CLICKS.addLast(now);
        }
        prevAttackKeyDown = attackDown;
        prevUseKeyDown = useDown;
    }

    public static synchronized int getLeftCps() {
        long now = System.currentTimeMillis();
        prune(LEFT_CLICKS, now);
        return LEFT_CLICKS.size();
    }

    public static synchronized int getRightCps() {
        long now = System.currentTimeMillis();
        prune(RIGHT_CLICKS, now);
        return RIGHT_CLICKS.size();
    }

    public static synchronized void reset() {
        LEFT_CLICKS.clear();
        RIGHT_CLICKS.clear();
        prevAttackKeyDown = false;
        prevUseKeyDown = false;
    }

    private static void prune(Deque<Long> deque, long now) {
        while (!deque.isEmpty() && (now - deque.peekFirst()) > WINDOW_MS) {
            deque.pollFirst();
        }
    }
}
