package io.github.ssbaxys.lumina.anim;

/**
 * Планировщик анимаций одного UI-корня. Держит только активные анимации,
 * поэтому в покое не тратит ни времени, ни памяти.
 */
public final class Animator {
    private Animation[] active = new Animation[16];
    private int size;
    private Animation[] pending = new Animation[8];
    private int pendingSize;
    private boolean ticking;

    public void start(Animation animation) {
        if (ticking) {
            if (pendingSize == pending.length) {
                pending = grow(pending);
            }
            pending[pendingSize++] = animation;
            return;
        }
        if (size == active.length) {
            active = grow(active);
        }
        active[size++] = animation;
    }

    /** @return есть ли ещё активные анимации (то есть нужен следующий кадр) */
    public boolean tick(float dt) {
        ticking = true;
        int write = 0;
        try {
            for (int i = 0; i < size; i++) {
                Animation a = active[i];
                if (a.tick(dt)) {
                    active[write++] = a;
                }
            }
        } finally {
            for (int i = write; i < size; i++) {
                active[i] = null;
            }
            size = write;
            ticking = false;
        }
        for (int i = 0; i < pendingSize; i++) {
            start(pending[i]);
            pending[i] = null;
        }
        pendingSize = 0;
        return size > 0;
    }

    public boolean isIdle() {
        return size == 0 && pendingSize == 0;
    }

    public int activeCount() {
        return size + pendingSize;
    }

    public void clear() {
        java.util.Arrays.fill(active, 0, size, null);
        java.util.Arrays.fill(pending, 0, pendingSize, null);
        size = 0;
        pendingSize = 0;
    }

    private static Animation[] grow(Animation[] a) {
        Animation[] b = new Animation[a.length * 2];
        System.arraycopy(a, 0, b, 0, a.length);
        return b;
    }
}
