package io.github.ssbaxys.lumina.render;

/** Мягкая тень под элементом (рисуется SDF-шейдером, без текстур). */
public record Shadow(float offsetX, float offsetY, float blur, float spread, int color) {
    public static final Shadow NONE = new Shadow(0, 0, 0, 0, Color.TRANSPARENT);

    public static Shadow of(float offsetY, float blur, int color) {
        return new Shadow(0, offsetY, blur, 0, color);
    }

    public boolean isNone() {
        return Color.alpha(color) == 0;
    }

    public static Shadow lerp(Shadow a, Shadow b, float t) {
        return new Shadow(
                a.offsetX + (b.offsetX - a.offsetX) * t,
                a.offsetY + (b.offsetY - a.offsetY) * t,
                a.blur + (b.blur - a.blur) * t,
                a.spread + (b.spread - a.spread) * t,
                Color.lerp(a.color, b.color, t));
    }
}
