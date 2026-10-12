package io.github.ssbaxys.lumina.render;

/** Радиусы скругления углов: верх-лево, верх-право, низ-право, низ-лево. */
public record Radii(float topLeft, float topRight, float bottomRight, float bottomLeft) {
    public static final Radii ZERO = new Radii(0, 0, 0, 0);

    public static Radii all(float r) {
        return r == 0f ? ZERO : new Radii(r, r, r, r);
    }

    public static Radii top(float r) {
        return new Radii(r, r, 0, 0);
    }

    public static Radii bottom(float r) {
        return new Radii(0, 0, r, r);
    }

    public boolean isZero() {
        return topLeft <= 0f && topRight <= 0f && bottomRight <= 0f && bottomLeft <= 0f;
    }

    public Radii expand(float d) {
        return new Radii(Math.max(0, topLeft + d), Math.max(0, topRight + d),
                Math.max(0, bottomRight + d), Math.max(0, bottomLeft + d));
    }

    public static Radii lerp(Radii a, Radii b, float t) {
        return new Radii(
                a.topLeft + (b.topLeft - a.topLeft) * t,
                a.topRight + (b.topRight - a.topRight) * t,
                a.bottomRight + (b.bottomRight - a.bottomRight) * t,
                a.bottomLeft + (b.bottomLeft - a.bottomLeft) * t);
    }
}
