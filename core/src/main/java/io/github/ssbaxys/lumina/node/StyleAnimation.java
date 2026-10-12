package io.github.ssbaxys.lumina.node;

import io.github.ssbaxys.lumina.anim.Easing;
import io.github.ssbaxys.lumina.anim.Interpolator;
import io.github.ssbaxys.lumina.anim.Keyframes;
import io.github.ssbaxys.lumina.anim.Motion;
import io.github.ssbaxys.lumina.anim.Playback;
import io.github.ssbaxys.lumina.anim.Tween;
import io.github.ssbaxys.lumina.style.StyleKey;

/**
 * Построитель анимации одного свойства стиля элемента ({@link UiElement#animate}).
 *
 * <pre>{@code
 * card.animate(Styles.TRANSLATE_Y).from(12f).to(0f).duration(0.4f).ease(Easing.OUT_BACK).play();
 * badge.animate(Styles.BACKGROUND)
 *         .keyframe(0f, Brush.solid(RED)).keyframe(0.5f, Brush.solid(GOLD)).keyframe(1f, Brush.solid(RED))
 *         .duration(2f).loop().play();
 * icon.animate(Styles.SCALE).to(1.25f).duration(0.15f).yoyo().loops(2).play();
 * }</pre>
 *
 * <p>По умолчанию конечное значение остаётся в слое анимаций ({@link #keep}); {@code keep(false)} убирает
 * его по окончании, и элемент возвращается к значению из стиля.</p>
 */
public final class StyleAnimation<T> {
    private final UiElement element;
    private final StyleKey<T> key;
    private Keyframes<T> frames;
    private T from;
    private T to;
    private float duration = 0.3f;
    private float delay;
    private Easing easing = Easing.OUT_CUBIC;
    private int loops = 1;
    private boolean yoyo;
    private boolean keep = true;
    private Runnable onFinish;

    StyleAnimation(UiElement element, StyleKey<T> key) {
        this.element = element;
        this.key = key;
    }

    /** Начальное значение (по умолчанию — текущее). */
    public StyleAnimation<T> from(T value) {
        this.from = value;
        return this;
    }

    public StyleAnimation<T> to(T value) {
        this.to = value;
        return this;
    }

    /** Ключевой кадр в момент {@code time} (0..1); заменяет from/to. */
    public StyleAnimation<T> keyframe(float time, T value) {
        return keyframe(time, value, Easing.LINEAR);
    }

    public StyleAnimation<T> keyframe(float time, T value, Easing segmentEasing) {
        if (frames == null) {
            frames = Keyframes.of(interpolator());
        }
        frames.at(time, value, segmentEasing);
        return this;
    }

    public StyleAnimation<T> duration(float seconds) {
        this.duration = seconds;
        return this;
    }

    public StyleAnimation<T> delay(float seconds) {
        this.delay = seconds;
        return this;
    }

    public StyleAnimation<T> ease(Easing easing) {
        this.easing = easing;
        return this;
    }

    public StyleAnimation<T> loops(int loops) {
        this.loops = loops;
        return this;
    }

    public StyleAnimation<T> loop() {
        return loops(Playback.INFINITE);
    }

    public StyleAnimation<T> yoyo() {
        this.yoyo = true;
        return this;
    }

    /** Оставить последнее значение после окончания (по умолчанию да). */
    public StyleAnimation<T> keep(boolean keep) {
        this.keep = keep;
        return this;
    }

    public StyleAnimation<T> onFinish(Runnable onFinish) {
        this.onFinish = onFinish;
        return this;
    }

    /** Движение без запуска — чтобы собрать из нескольких свойств последовательность. */
    public Motion motion() {
        Keyframes<T> kf = frames;
        if (kf == null) {
            T start = from != null ? from : element.computedStyle().get(key);
            T end = to != null ? to : element.computedStyle().getBase(key);
            kf = Keyframes.between(interpolator(), start, end);
        }
        Motion m = new Tween<>(kf, duration, easing, v -> element.setAnimatedStyle(key, v));
        return delay > 0f ? m.delayed(delay) : m;
    }

    public Playback play() {
        Playback p = new Playback(motion()).loops(loops).yoyo(yoyo).onFinish(() -> {
            if (!keep) {
                element.clearAnimatedStyle(key);
            }
            if (onFinish != null) {
                onFinish.run();
            }
        });
        return element.play(p);
    }

    private Interpolator<T> interpolator() {
        Interpolator<T> i = key.interpolator();
        return i != null ? i : Interpolator.step();
    }
}
