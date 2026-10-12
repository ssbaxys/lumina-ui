package io.github.ssbaxys.lumina.geom;

/** Отступы по четырём сторонам. Неизменяемые, безопасно шарить между стилями. */
public record Insets(float top, float right, float bottom, float left) {
    public static final Insets ZERO = new Insets(0, 0, 0, 0);

    public static Insets all(float v) {
        return new Insets(v, v, v, v);
    }

    public static Insets of(float vertical, float horizontal) {
        return new Insets(vertical, horizontal, vertical, horizontal);
    }

    public float horizontal() {
        return left + right;
    }

    public float vertical() {
        return top + bottom;
    }

    public static Insets lerp(Insets a, Insets b, float t) {
        return new Insets(
                a.top + (b.top - a.top) * t,
                a.right + (b.right - a.right) * t,
                a.bottom + (b.bottom - a.bottom) * t,
                a.left + (b.left - a.left) * t);
    }
}
