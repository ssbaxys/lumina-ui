package io.github.ssbaxys.lumina.style;

import io.github.ssbaxys.lumina.anim.Interpolator;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.Shadow;

/** Встроенные свойства стиля. */
public final class Styles {
    public static final StyleKey<Brush> BACKGROUND = StyleKey.builder("background", Brush.class)
            .animated(Interpolator.BRUSH).register();
    public static final StyleKey<Integer> BORDER_COLOR = StyleKey.builder("border-color", Integer.class)
            .defaultValue(Color.TRANSPARENT).animated(Interpolator.COLOR).register();
    public static final StyleKey<Float> BORDER_WIDTH = StyleKey.builder("border-width", Float.class)
            .defaultValue(0f).animated(Interpolator.FLOAT).register();
    public static final StyleKey<Radii> RADIUS = StyleKey.builder("radius", Radii.class)
            .defaultValue(Radii.ZERO).animated(Interpolator.RADII).register();
    public static final StyleKey<Shadow> SHADOW = StyleKey.builder("shadow", Shadow.class)
            .defaultValue(Shadow.NONE).animated(Interpolator.SHADOW).register();
    public static final StyleKey<Integer> TEXT_COLOR = StyleKey.builder("text-color", Integer.class)
            .defaultValue(Color.WHITE).inherited().animated(Interpolator.COLOR).register();
    /** Акцентный цвет (наследуется): заливка переключателей, ползунков, индикаторов. */
    public static final StyleKey<Integer> ACCENT_COLOR = StyleKey.builder("accent-color", Integer.class)
            .defaultValue(0xFF5B8CFF).inherited().animated(Interpolator.COLOR).register();
    public static final StyleKey<Boolean> TEXT_SHADOW = StyleKey.builder("text-shadow", Boolean.class)
            .defaultValue(Boolean.FALSE).inherited().register();
    public static final StyleKey<Float> OPACITY = StyleKey.builder("opacity", Float.class)
            .defaultValue(1f).animated(Interpolator.FLOAT).register();
    public static final StyleKey<Float> SCALE = StyleKey.builder("scale", Float.class)
            .defaultValue(1f).animated(Interpolator.FLOAT).register();
    public static final StyleKey<Insets> PADDING = StyleKey.builder("padding", Insets.class)
            .defaultValue(Insets.ZERO).affectsLayout().animated(Interpolator.INSETS).register();
    public static final StyleKey<Transition> TRANSITION = StyleKey.builder("transition", Transition.class)
            .defaultValue(Transition.NONE).register();

    private Styles() {
    }

    /** Гарантирует регистрацию встроенных ключей до построения стилей. */
    static void bootstrap() {
    }
}
