package io.github.ssbaxys.lumina.anim;

/** Решение кривой Безье по x методом Ньютона с запасным делением пополам (как в браузерах). */
final class CubicBezier implements Easing {
    private final float ax, bx, cx, ay, by, cy;

    CubicBezier(float x1, float y1, float x2, float y2) {
        cx = 3f * x1;
        bx = 3f * (x2 - x1) - cx;
        ax = 1f - cx - bx;
        cy = 3f * y1;
        by = 3f * (y2 - y1) - cy;
        ay = 1f - cy - by;
    }

    @Override
    public float apply(float t) {
        if (t <= 0f) {
            return 0f;
        }
        if (t >= 1f) {
            return 1f;
        }
        float s = solveX(t);
        return ((ay * s + by) * s + cy) * s;
    }

    private float sampleX(float s) {
        return ((ax * s + bx) * s + cx) * s;
    }

    private float solveX(float x) {
        float s = x;
        for (int i = 0; i < 8; i++) {
            float err = sampleX(s) - x;
            if (Math.abs(err) < 1e-5f) {
                return s;
            }
            float d = (3f * ax * s + 2f * bx) * s + cx;
            if (Math.abs(d) < 1e-6f) {
                break;
            }
            s -= err / d;
        }
        float lo = 0f;
        float hi = 1f;
        s = x;
        for (int i = 0; i < 32; i++) {
            float v = sampleX(s);
            if (Math.abs(v - x) < 1e-5f) {
                break;
            }
            if (v < x) {
                lo = s;
            } else {
                hi = s;
            }
            s = (lo + hi) * 0.5f;
        }
        return s;
    }
}
