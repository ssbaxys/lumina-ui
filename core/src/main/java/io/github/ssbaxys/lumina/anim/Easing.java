package io.github.ssbaxys.lumina.anim;

/** Функция плавности: t в [0, 1] → прогресс (может выходить за [0, 1], например у OUT_BACK). */
@FunctionalInterface
public interface Easing {
    float apply(float t);

    Easing LINEAR = t -> t;
    Easing IN_QUAD = t -> t * t;
    Easing OUT_QUAD = t -> t * (2f - t);
    Easing IN_OUT_QUAD = t -> t < 0.5f ? 2f * t * t : -1f + (4f - 2f * t) * t;
    Easing IN_CUBIC = t -> t * t * t;
    Easing OUT_CUBIC = t -> {
        float u = 1f - t;
        return 1f - u * u * u;
    };
    Easing IN_OUT_CUBIC = t -> {
        if (t < 0.5f) {
            return 4f * t * t * t;
        }
        float u = -2f * t + 2f;
        return 1f - u * u * u / 2f;
    };
    Easing OUT_EXPO = t -> t >= 1f ? 1f : 1f - (float) Math.pow(2, -10 * t);
    Easing OUT_BACK = t -> {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    };
    /** Стандартная кривая Material "emphasized decelerate". */
    Easing EMPHASIZED = cubicBezier(0.05f, 0.7f, 0.1f, 1f);

    /** CSS-совместимая cubic-bezier(x1, y1, x2, y2). */
    static Easing cubicBezier(float x1, float y1, float x2, float y2) {
        return new CubicBezier(x1, y1, x2, y2);
    }
}
