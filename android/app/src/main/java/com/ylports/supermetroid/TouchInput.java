package com.ylports.supermetroid;

import java.util.HashMap;
import java.util.Map;

/** Android-free pointer aggregation: one finger cannot release another's key. */
public final class TouchInput {
    public static final int UP = 1, DOWN = 2, LEFT = 4, RIGHT = 8;
    public static final int A = 16, B = 32, X = 64, Y = 128, L = 256, R = 512;
    public static final int START = 1024, SELECT = 2048;
    public interface Listener { void changed(int pressed, int released); }
    private final Map<Integer, Integer> pointers = new HashMap<>();
    private final Listener listener;
    private int held;

    public TouchInput(Listener listener) { this.listener = listener; }
    public int held() { return held; }
    public void pointer(int id, int mask) {
        if (mask == 0) pointers.remove(id); else pointers.put(id, mask);
        publish();
    }
    public void release(int id) { pointers.remove(id); publish(); }
    public void clear() { pointers.clear(); publish(); }
    private void publish() {
        int next = 0;
        for (int mask : pointers.values()) next |= mask;
        int pressed = next & ~held, released = held & ~next;
        held = next;
        if ((pressed | released) != 0) listener.changed(pressed, released);
    }

    public static int direction(float x, float y, float radius) {
        if (radius <= 0 || x*x + y*y < radius*radius*0.045f) return 0;
        float ax = Math.abs(x), ay = Math.abs(y);
        int mask = 0;
        if (ax > ay * 0.414214f) mask |= x < 0 ? LEFT : RIGHT;
        if (ay > ax * 0.414214f) mask |= y < 0 ? UP : DOWN;
        return mask;
    }
}
