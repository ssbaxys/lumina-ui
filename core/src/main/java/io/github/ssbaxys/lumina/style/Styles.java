package io.github.ssbaxys.lumina.style;

import io.github.ssbaxys.lumina.anim.Interpolator;
import io.github.ssbaxys.lumina.geom.Insets;
import io.github.ssbaxys.lumina.render.BlendMode;
import io.github.ssbaxys.lumina.render.Brush;
import io.github.ssbaxys.lumina.render.Color;
import io.github.ssbaxys.lumina.render.NineSlice;
import io.github.ssbaxys.lumina.render.Outline;
import io.github.ssbaxys.lumina.render.Radii;
import io.github.ssbaxys.lumina.render.Shadow;
import io.github.ssbaxys.lumina.sound.UiSound;

/**
 * Встроенные свойства стиля. Все визуальные свойства анимируются переходами ({@link Transition})
 * и анимациями элементов ({@code UiElement.animate}).
 */
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
    /** Обводка градиентом; если задана, заменяет {@link #BORDER_COLOR}. */
    public static final StyleKey<Brush> BORDER_BRUSH = StyleKey.builder("border-brush", Brush.class)
            .animated(Interpolator.BRUSH).register();
    /** Свечение: тень с аддитивным смешиванием, подсвечивает фон вокруг элемента. */
    public static final StyleKey<Shadow> GLOW = StyleKey.builder("glow", Shadow.class)
            .defaultValue(Shadow.NONE).animated(Interpolator.SHADOW).register();
    /** Внешняя обводка с отступом (кольцо фокуса), не влияет на раскладку. */
    public static final StyleKey<Outline> OUTLINE = StyleKey.builder("outline", Outline.class)
            .defaultValue(Outline.NONE).animated(Interpolator.OUTLINE).register();
    /** Фон из текстуры «9 частей» (рамка окна, свиток); рисуется над {@link #BACKGROUND}. */
    public static final StyleKey<NineSlice> NINE_SLICE = StyleKey.builder("nine-slice", NineSlice.class).register();
    /** Заливка поверх содержимого и детей в границах элемента: тонировка, блик, затемнение. */
    public static final StyleKey<Brush> FOREGROUND = StyleKey.builder("foreground", Brush.class)
            .animated(Interpolator.BRUSH).register();
    /** Сдвиг при отрисовке (и попадании мыши), без влияния на раскладку. */
    public static final StyleKey<Float> TRANSLATE_X = StyleKey.builder("translate-x", Float.class)
            .defaultValue(0f).animated(Interpolator.FLOAT).register();
    public static final StyleKey<Float> TRANSLATE_Y = StyleKey.builder("translate-y", Float.class)
            .defaultValue(0f).animated(Interpolator.FLOAT).register();
    /** Масштаб по осям (умножается на {@link #SCALE}), от центра элемента. */
    public static final StyleKey<Float> SCALE_X = StyleKey.builder("scale-x", Float.class)
            .defaultValue(1f).animated(Interpolator.FLOAT).register();
    public static final StyleKey<Float> SCALE_Y = StyleKey.builder("scale-y", Float.class)
            .defaultValue(1f).animated(Interpolator.FLOAT).register();
    /** Масштаб текста (наследуется, влияет на раскладку). */
    public static final StyleKey<Float> FONT_SCALE = StyleKey.builder("font-scale", Float.class)
            .defaultValue(1f).inherited().affectsLayout().animated(Interpolator.FLOAT).register();
    /** Режим смешивания элемента и его детей. */
    public static final StyleKey<BlendMode> BLEND = StyleKey.builder("blend", BlendMode.class)
            .defaultValue(BlendMode.NORMAL).register();
    public static final StyleKey<Transition> TRANSITION = StyleKey.builder("transition", Transition.class)
            .defaultValue(Transition.NONE).register();
    /** Поле ввода с линией снизу вместо рамки; при фокусе акцентная линия растёт от центра. */
    public static final StyleKey<Boolean> UNDERLINE = StyleKey.builder("field-underline", Boolean.class)
            .defaultValue(Boolean.FALSE).register();
    /**
     * Звук при наведении мыши: играет, когда мышь входит в элемент (не повторяется при движении внутри),
     * не чаще раза в {@code UiRoot.hoverSoundInterval}. Не наследуется; {@link UiSound#NONE} — тишина.
     */
    public static final StyleKey<UiSound> HOVER_SOUND = StyleKey.builder("hover-sound", UiSound.class).register();
    /** Звук нажатия (кнопки, переключатели, пункты меню); не задан — стандартный щелчок игры у кнопок. */
    public static final StyleKey<UiSound> CLICK_SOUND = StyleKey.builder("click-sound", UiSound.class).register();

    /**
     * Размытие того, что под элементом («матовое стекло», CSS {@code backdrop-filter: blur(8px)} или
     * {@code backdrop-blur: 8}), в пикселях; 0 — выключено. Рисуется под фоном элемента, поэтому фон лучше
     * делать полупрозрачным. Бэкенд без размытия рисует матовую полупрозрачную подложку.
     */
    public static final StyleKey<Float> BACKDROP_BLUR = StyleKey.builder("backdrop-blur", Float.class)
            .defaultValue(0f).animated(Interpolator.FLOAT).register();
    /**
     * Обрезать детей по скруглению {@link #RADIUS}, а не по прямоугольнику (у элементов, которые обрезают детей:
     * {@code ScrollPanel}, {@code Panel.clip(true)}; у {@code Image} — точная маска вместо полос).
     * Если бэкенд не умеет точную маску, остаётся прямоугольный клип.
     */
    public static final StyleKey<Boolean> CLIP_ROUNDED = StyleKey.builder("clip-rounded", Boolean.class)
            .defaultValue(Boolean.FALSE).register();

    private Styles() {
    }

    /** Гарантирует регистрацию встроенных ключей до построения стилей. */
    static void bootstrap() {
    }
}
