package io.github.ssbaxys.lumina.anim;

import java.util.Arrays;

/**
 * Ключевые кадры: значения в моменты 0..1 и плавность каждого отрезка, как {@code @keyframes} в CSS.
 *
 * <pre>{@code
 * Keyframes.of(Interpolator.COLOR)
 *         .at(0f, RED)
 *         .at(0.5f, YELLOW, Easing.OUT_SINE)   // плавность отрезка, который заканчивается здесь
 *         .at(1f, RED)
 * }</pre>
 */
public final class Keyframes<T> {
    private final Interpolator<T> interpolator;
    private float[] times = new float[0];
    private Object[] values = new Object[0];
    private Easing[] easings = new Easing[0];

    private Keyframes(Interpolator<T> interpolator) {
        this.interpolator = interpolator;
    }

    public static <T> Keyframes<T> of(Interpolator<T> interpolator) {
        return new Keyframes<>(interpolator != null ? interpolator : Interpolator.step());
    }

    /** Два кадра: от {@code from} к {@code to}. */
    public static <T> Keyframes<T> between(Interpolator<T> interpolator, T from, T to) {
        return Keyframes.of(interpolator).at(0f, from).at(1f, to);
    }

    public Keyframes<T> at(float time, T value) {
        return at(time, value, Easing.LINEAR);
    }

    /** Кадр в момент {@code time} (0..1); {@code easing} — плавность отрезка, ведущего к нему. */
    public Keyframes<T> at(float time, T value, Easing easing) {
        int n = times.length;
        int i = 0;
        while (i < n && times[i] <= time) {
            i++;
        }
        times = insert(times, i, time);
        values = insert(values, i, value);
        easings = insert(easings, i, easing);
        return this;
    }

    public int size() {
        return times.length;
    }

    @SuppressWarnings("unchecked")
    public T value(int i) {
        return (T) values[i];
    }

    /** Значение в момент {@code p} (0..1). */
    @SuppressWarnings("unchecked")
    public T sample(float p) {
        int n = times.length;
        if (n == 0) {
            throw new IllegalStateException("Keyframes are empty");
        }
        if (p <= times[0]) {
            return (T) values[0];
        }
        for (int i = 1; i < n; i++) {
            if (p <= times[i]) {
                float span = times[i] - times[i - 1];
                float f = span <= 0f ? 1f : (p - times[i - 1]) / span;
                return interpolator.lerp((T) values[i - 1], (T) values[i], easings[i].apply(f));
            }
        }
        return (T) values[n - 1];
    }

    private static float[] insert(float[] a, int i, float v) {
        float[] b = Arrays.copyOf(a, a.length + 1);
        System.arraycopy(a, i, b, i + 1, a.length - i);
        b[i] = v;
        return b;
    }

    private static <E> E[] insert(E[] a, int i, E v) {
        E[] b = Arrays.copyOf(a, a.length + 1);
        System.arraycopy(a, i, b, i + 1, a.length - i);
        b[i] = v;
        return b;
    }
}
