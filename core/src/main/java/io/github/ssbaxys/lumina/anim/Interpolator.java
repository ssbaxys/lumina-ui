package io.github.ssbaxys.lumina.anim;

import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.Shadow;

/** Интерполяция значения стиля для плавных переходов. */
@FunctionalInterface
public interface Interpolator<T> {
    T lerp(T a, T b, float t);

    Interpolator<Float> FLOAT = (a, b, t) -> a + (b - a) * t;
    Interpolator<Integer> COLOR = (a, b, t) -> Color.lerp(a, b, t);
    Interpolator<Brush> BRUSH = Brush::lerp;
    Interpolator<Radii> RADII = Radii::lerp;
    Interpolator<Insets> INSETS = Insets::lerp;
    Interpolator<Shadow> SHADOW = Shadow::lerp;
}
