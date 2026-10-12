package io.github.ssbaxys.lumina.anim;

import java.util.function.Consumer;

/** Движение значения по ключевым кадрам за заданное время с общей плавностью. */
public final class Tween<T> extends Motion {
    private final Keyframes<T> frames;
    private final float duration;
    private final Easing easing;
    private final Consumer<? super T> target;

    public Tween(Keyframes<T> frames, float duration, Easing easing, Consumer<? super T> target) {
        this.frames = frames;
        this.duration = Math.max(0f, duration);
        this.easing = easing != null ? easing : Easing.LINEAR;
        this.target = target;
    }

    /** От {@code from} к {@code to}. */
    public static <T> Tween<T> of(Interpolator<T> interpolator, T from, T to, float duration, Easing easing,
                                  Consumer<? super T> target) {
        return new Tween<>(Keyframes.between(interpolator, from, to), duration, easing, target);
    }

    @Override
    public float duration() {
        return duration;
    }

    @Override
    public void apply(float t) {
        float p = duration <= 0f ? 1f : Math.max(0f, Math.min(1f, t / duration));
        target.accept(frames.sample(easing.apply(p)));
    }
}
