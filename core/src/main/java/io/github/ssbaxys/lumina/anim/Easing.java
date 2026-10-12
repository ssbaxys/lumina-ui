package io.github.ssbaxys.lumina.anim;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
    Easing IN_SINE = t -> 1f - (float) Math.cos(t * Math.PI / 2);
    Easing OUT_SINE = t -> (float) Math.sin(t * Math.PI / 2);
    Easing IN_OUT_SINE = t -> -((float) Math.cos(Math.PI * t) - 1f) / 2f;
    Easing IN_EXPO = t -> t <= 0f ? 0f : (float) Math.pow(2, 10 * t - 10);
    Easing OUT_EXPO = t -> t >= 1f ? 1f : 1f - (float) Math.pow(2, -10 * t);
    Easing IN_OUT_EXPO = t -> {
        if (t <= 0f || t >= 1f) {
            return t <= 0f ? 0f : 1f;
        }
        return t < 0.5f ? (float) Math.pow(2, 20 * t - 10) / 2f : (2f - (float) Math.pow(2, -20 * t + 10)) / 2f;
    };
    Easing IN_CIRC = t -> 1f - (float) Math.sqrt(1f - t * t);
    Easing OUT_CIRC = t -> (float) Math.sqrt(1f - (t - 1f) * (t - 1f));
    Easing IN_BACK = t -> {
        float c1 = 1.70158f;
        return (c1 + 1f) * t * t * t - c1 * t * t;
    };
    Easing OUT_BACK = t -> {
        float c1 = 1.70158f;
        float c3 = c1 + 1f;
        float u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    };
    Easing IN_OUT_BACK = t -> {
        float c2 = 1.70158f * 1.525f;
        return t < 0.5f
                ? (2f * t) * (2f * t) * ((c2 + 1f) * 2f * t - c2) / 2f
                : ((2f * t - 2f) * (2f * t - 2f) * ((c2 + 1f) * (t * 2f - 2f) + c2) + 2f) / 2f;
    };
    Easing OUT_ELASTIC = t -> {
        if (t <= 0f || t >= 1f) {
            return t <= 0f ? 0f : 1f;
        }
        return (float) (Math.pow(2, -10 * t) * Math.sin((t * 10 - 0.75) * (2 * Math.PI / 3)) + 1);
    };
    Easing IN_ELASTIC = t -> 1f - OUT_ELASTIC.apply(1f - t);
    Easing OUT_BOUNCE = t -> {
        float n1 = 7.5625f;
        float d1 = 2.75f;
        if (t < 1f / d1) {
            return n1 * t * t;
        } else if (t < 2f / d1) {
            t -= 1.5f / d1;
            return n1 * t * t + 0.75f;
        } else if (t < 2.5f / d1) {
            t -= 2.25f / d1;
            return n1 * t * t + 0.9375f;
        }
        t -= 2.625f / d1;
        return n1 * t * t + 0.984375f;
    };
    Easing IN_BOUNCE = t -> 1f - OUT_BOUNCE.apply(1f - t);
    /** Стандартная кривая Material "emphasized decelerate". */
    Easing EMPHASIZED = cubicBezier(0.05f, 0.7f, 0.1f, 1f);

    /** CSS-совместимая cubic-bezier(x1, y1, x2, y2). */
    static Easing cubicBezier(float x1, float y1, float x2, float y2) {
        return new CubicBezier(x1, y1, x2, y2);
    }

    /** Ступеньки, как CSS steps(n): для покадровых спрайтов и «механических» движений. */
    static Easing steps(int count) {
        int n = Math.max(1, count);
        return t -> t >= 1f ? 1f : (float) Math.floor(t * n) / n;
    }

    /**
     * Пружина как кривая: {@code dampingRatio} < 1 — колебания вокруг цели (0.3 — сильно, 0.7 — едва),
     * {@code oscillations} — сколько полупериодов уложить в длительность.
     */
    static Easing spring(float dampingRatio, float oscillations) {
        double zeta = Math.max(0.01, Math.min(dampingRatio, 0.999));
        double omega = Math.PI * Math.max(oscillations, 0.1) / Math.sqrt(1 - zeta * zeta);
        double wd = omega * Math.sqrt(1 - zeta * zeta);
        return t -> {
            if (t >= 1f) {
                return 1f;
            }
            double decay = Math.exp(-zeta * omega * t);
            return (float) (1 - decay * (Math.cos(wd * t) + zeta * omega / wd * Math.sin(wd * t)));
        };
    }

    /** Та же кривая, проигранная задом наперёд (начало быстрое ↔ медленное). */
    default Easing reversed() {
        return t -> 1f - apply(1f - t);
    }

    /** Туда и обратно за одну длительность. */
    default Easing mirrored() {
        return t -> t < 0.5f ? apply(t * 2f) : apply(2f - t * 2f);
    }

    /** Все встроенные кривые по именам (для галерей и настроек). */
    static Map<String, Easing> presets() {
        return Presets.ALL;
    }

    final class Presets {
        static final Map<String, Easing> ALL;

        static {
            Map<String, Easing> m = new LinkedHashMap<>();
            m.put("LINEAR", LINEAR);
            m.put("IN_QUAD", IN_QUAD);
            m.put("OUT_QUAD", OUT_QUAD);
            m.put("IN_OUT_QUAD", IN_OUT_QUAD);
            m.put("IN_CUBIC", IN_CUBIC);
            m.put("OUT_CUBIC", OUT_CUBIC);
            m.put("IN_OUT_CUBIC", IN_OUT_CUBIC);
            m.put("IN_SINE", IN_SINE);
            m.put("OUT_SINE", OUT_SINE);
            m.put("IN_OUT_SINE", IN_OUT_SINE);
            m.put("IN_EXPO", IN_EXPO);
            m.put("OUT_EXPO", OUT_EXPO);
            m.put("IN_OUT_EXPO", IN_OUT_EXPO);
            m.put("IN_CIRC", IN_CIRC);
            m.put("OUT_CIRC", OUT_CIRC);
            m.put("IN_BACK", IN_BACK);
            m.put("OUT_BACK", OUT_BACK);
            m.put("IN_OUT_BACK", IN_OUT_BACK);
            m.put("IN_ELASTIC", IN_ELASTIC);
            m.put("OUT_ELASTIC", OUT_ELASTIC);
            m.put("IN_BOUNCE", IN_BOUNCE);
            m.put("OUT_BOUNCE", OUT_BOUNCE);
            m.put("EMPHASIZED", EMPHASIZED);
            ALL = Collections.unmodifiableMap(m);
        }

        private Presets() {
        }
    }
}
