package io.github.ssbaxys.lumina.style;

import io.github.ssbaxys.lumina.anim.Easing;

import java.util.Arrays;
import java.util.Objects;

/**
 * Как анимировать смену стиля при смене состояния (hover, pressed...). Общие длительность, задержка
 * и плавность плюс свои для отдельных свойств — как {@code transition} в CSS:
 *
 * <pre>{@code
 * Transition.of(0.2f)
 *         .key(Styles.BACKGROUND, 0.6f, Easing.OUT_EXPO)   // фон перетекает дольше
 *         .key(Styles.SCALE, 0.35f, Easing.OUT_BACK)      // масштаб с «отскоком»
 *         .instant(Styles.BORDER_WIDTH)                    // рамка — сразу
 * }</pre>
 */
public final class Transition {
    public static final Transition NONE = new Transition(0f, Easing.LINEAR);

    private final float duration;
    private final Easing easing;
    private final float delay;
    private final int[] keys;
    private final float[] durations;
    private final float[] delays;
    private final Easing[] easings;

    public Transition(float duration, Easing easing) {
        this(duration, easing, 0f, new int[0], new float[0], new float[0], new Easing[0]);
    }

    private Transition(float duration, Easing easing, float delay, int[] keys, float[] durations, float[] delays,
                       Easing[] easings) {
        this.duration = duration;
        this.easing = easing;
        this.delay = delay;
        this.keys = keys;
        this.durations = durations;
        this.delays = delays;
        this.easings = easings;
    }

    public static Transition of(float durationSeconds) {
        return new Transition(durationSeconds, Easing.OUT_CUBIC);
    }

    public static Transition of(float durationSeconds, Easing easing) {
        return new Transition(durationSeconds, easing);
    }

    public float duration() {
        return duration;
    }

    public Easing easing() {
        return easing;
    }

    public float delay() {
        return delay;
    }

    public Transition delay(float seconds) {
        return new Transition(duration, easing, seconds, keys, durations, delays, easings);
    }

    public Transition easing(Easing easing) {
        return new Transition(duration, easing, delay, keys, durations, delays, easings);
    }

    /** Своя длительность и плавность для одного свойства. */
    public Transition key(StyleKey<?> key, float duration, Easing easing) {
        return key(key, duration, easing, delay);
    }

    public Transition key(StyleKey<?> key, float duration, Easing easing, float delay) {
        int n = keys.length;
        int at = indexOf(key.id());
        int[] k = at < 0 ? Arrays.copyOf(keys, n + 1) : keys.clone();
        float[] d = at < 0 ? Arrays.copyOf(durations, n + 1) : durations.clone();
        float[] dl = at < 0 ? Arrays.copyOf(delays, n + 1) : delays.clone();
        Easing[] e = at < 0 ? Arrays.copyOf(easings, n + 1) : easings.clone();
        int i = at < 0 ? n : at;
        k[i] = key.id();
        d[i] = duration;
        dl[i] = delay;
        e[i] = easing;
        return new Transition(this.duration, this.easing, this.delay, k, d, dl, e);
    }

    /** Свойство меняется сразу, без перехода. */
    public Transition instant(StyleKey<?> key) {
        return key(key, 0f, Easing.LINEAR, 0f);
    }

    /** Все длительности и задержки × factor (так работает токен {@link Tokens#MOTION}). */
    public Transition scaled(float factor) {
        if (factor == 1f) {
            return this;
        }
        float[] d = durations.clone();
        float[] dl = delays.clone();
        for (int i = 0; i < d.length; i++) {
            d[i] *= factor;
            dl[i] *= factor;
        }
        return new Transition(duration * factor, easing, delay * factor, keys, d, dl, easings);
    }

    /** Id ключей стиля, для которых заданы свои длительность, задержка и плавность. */
    public int[] keyIds() {
        return keys.clone();
    }

    public float durationOf(int keyId) {
        int i = indexOf(keyId);
        return i < 0 ? duration : durations[i];
    }

    public float delayOf(int keyId) {
        int i = indexOf(keyId);
        return i < 0 ? delay : delays[i];
    }

    public Easing easingOf(int keyId) {
        int i = indexOf(keyId);
        return i < 0 ? easing : easings[i];
    }

    /** Когда закончится самый долгий из переходов. */
    public float totalDuration() {
        float total = duration + delay;
        for (int i = 0; i < keys.length; i++) {
            total = Math.max(total, durations[i] + delays[i]);
        }
        return total;
    }

    public boolean isNone() {
        return totalDuration() <= 0f;
    }

    private int indexOf(int keyId) {
        for (int i = 0; i < keys.length; i++) {
            if (keys[i] == keyId) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || (o instanceof Transition t && duration == t.duration && delay == t.delay
                && easing == t.easing && Arrays.equals(keys, t.keys) && Arrays.equals(durations, t.durations)
                && Arrays.equals(delays, t.delays) && Arrays.equals(easings, t.easings));
    }

    @Override
    public int hashCode() {
        return Objects.hash(duration, delay, easing, Arrays.hashCode(keys), Arrays.hashCode(durations));
    }

    @Override
    public String toString() {
        return "Transition[" + duration + "s" + (delay > 0f ? " +" + delay + "s" : "") + (keys.length > 0 ? ", " + keys.length + " keys" : "") + "]";
    }
}
