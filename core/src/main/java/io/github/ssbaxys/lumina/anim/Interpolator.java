package io.github.ssbaxys.lumina.anim;

import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.ColorSpace;
import io.github.ssbaxys.lumina.render.Outline;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.Shadow;

/** Интерполяция значения стиля для плавных переходов и анимаций. */
@FunctionalInterface
public interface Interpolator<T> {
    T lerp(T a, T b, float t);

    /** Без плавности: значение переключается на середине. Для перечислений, флагов, строк. */
    static <T> Interpolator<T> step() {
        return (a, b, t) -> t < 0.5f ? a : b;
    }

    /** Цвета в выбранном пространстве (например, по кругу оттенков в {@link ColorSpace#OKLCH}). */
    static Interpolator<Integer> color(ColorSpace space) {
        return (a, b, t) -> space.lerp(a, b, t);
    }

    Interpolator<Float> FLOAT = (a, b, t) -> a + (b - a) * t;
    Interpolator<Integer> COLOR = (a, b, t) -> Color.lerp(a, b, t);
    Interpolator<Brush> BRUSH = Brush::lerp;
    Interpolator<Radii> RADII = Radii::lerp;
    Interpolator<Insets> INSETS = Insets::lerp;
    Interpolator<Shadow> SHADOW = Shadow::lerp;
    Interpolator<Outline> OUTLINE = Outline::lerp;
}
