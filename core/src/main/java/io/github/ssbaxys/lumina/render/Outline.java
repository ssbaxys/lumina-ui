package io.github.ssbaxys.lumina.render;

/** Обводка снаружи элемента на расстоянии {@code offset} (кольцо фокуса), не влияет на раскладку. */
public record Outline(float width, float offset, int color) {
    public static final Outline NONE = new Outline(0f, 0f, Color.TRANSPARENT);

    public static Outline of(float width, float offset, int color) {
        return new Outline(width, offset, color);
    }

    public boolean isNone() {
        return width <= 0f || Color.alpha(color) == 0;
    }

    public static Outline lerp(Outline a, Outline b, float t) {
        return new Outline(a.width + (b.width - a.width) * t, a.offset + (b.offset - a.offset) * t,
                Color.lerp(a.color, b.color, t));
    }
}
