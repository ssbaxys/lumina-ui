package io.github.ssbaxys.lumina.anim;

import java.util.function.Consumer;

/**
 * Описание анимации как функции времени: «в момент t значения такие». Из маленьких движений
 * собираются большие: последовательности, параллельные группы, задержки, повторы, вызовы кода.
 * Само по себе движение ничего не делает — его проигрывает {@link Playback} (циклы, «туда-обратно»,
 * скорость, пауза).
 *
 * <pre>{@code
 * Motion pop = Motion.sequence(
 *         Tween.of(Interpolator.FLOAT, 0.9f, 1.08f, 0.18f, Easing.OUT_CUBIC, setScale),
 *         Tween.of(Interpolator.FLOAT, 1.08f, 1f, 0.25f, Easing.OUT_BACK, setScale),
 *         Motion.call(() -> sound.play()));
 * new Playback(pop).loops(3).start(animator);
 * }</pre>
 */
public abstract class Motion {
    /** Длительность одного прохода в секундах (конечная). */
    public abstract float duration();

    /** Применить состояние в момент {@code t} ∈ [0, duration]. Можно вызывать в любом порядке. */
    public abstract void apply(float t);

    /** Перед новым проходом: сбросить одноразовые вещи (например, вызовы {@link #call}). */
    public void reset() {
    }

    // ================================================================ сборка

    public static Motion sequence(Motion... parts) {
        return new Sequence(parts);
    }

    public static Motion parallel(Motion... parts) {
        return new Parallel(parts);
    }

    public static Motion delay(float seconds) {
        return new Delay(seconds);
    }

    /** Вызвать код, когда проигрывание дойдёт до этого места (один раз за проход). */
    public static Motion call(Runnable action) {
        return new Call(action);
    }

    /** Произвольное движение из функции прогресса 0..1 (после плавности). */
    public static Motion of(float duration, Easing easing, Consumer<Float> progress) {
        return new Motion() {
            @Override
            public float duration() {
                return duration;
            }

            @Override
            public void apply(float t) {
                progress.accept(easing.apply(duration <= 0f ? 1f : Math.min(1f, t / duration)));
            }
        };
    }

    public Motion then(Motion next) {
        return new Sequence(this, next);
    }

    public Motion with(Motion other) {
        return new Parallel(this, other);
    }

    public Motion delayed(float seconds) {
        return new Sequence(new Delay(seconds), this);
    }

    /** Повторить {@code times} раз подряд; {@code yoyo} — каждый второй проход задом наперёд. */
    public Motion repeat(int times, boolean yoyo) {
        return new Repeat(this, Math.max(1, times), yoyo);
    }

    /** То же движение задом наперёд. */
    public Motion reversed() {
        Motion self = this;
        return new Motion() {
            @Override
            public float duration() {
                return self.duration();
            }

            @Override
            public void apply(float t) {
                self.apply(self.duration() - t);
            }

            @Override
            public void reset() {
                self.reset();
            }
        };
    }

    // ================================================================ реализации

    static final class Sequence extends Motion {
        private final Motion[] parts;
        private final float[] starts;
        private final boolean[] done;
        private final float total;

        Sequence(Motion... parts) {
            this.parts = parts.clone();
            this.starts = new float[parts.length];
            this.done = new boolean[parts.length];
            float t = 0f;
            for (int i = 0; i < parts.length; i++) {
                starts[i] = t;
                t += parts[i].duration();
            }
            this.total = t;
        }

        @Override
        public float duration() {
            return total;
        }

        @Override
        public void apply(float t) {
            for (int i = 0; i < parts.length; i++) {
                Motion m = parts[i];
                float start = starts[i];
                float end = start + m.duration();
                if (t >= end && (t > start || m.duration() == 0f)) {
                    // Пройденный кусок — один раз в конечное состояние, чтобы не спорить со следующими.
                    if (!done[i]) {
                        m.apply(m.duration());
                        done[i] = true;
                    }
                } else {
                    done[i] = false;
                    if (t >= start) {
                        m.apply(t - start);
                    }
                }
            }
        }

        @Override
        public void reset() {
            java.util.Arrays.fill(done, false);
            for (Motion m : parts) {
                m.reset();
            }
        }
    }

    static final class Parallel extends Motion {
        private final Motion[] parts;
        private final float total;

        Parallel(Motion... parts) {
            this.parts = parts.clone();
            float t = 0f;
            for (Motion m : parts) {
                t = Math.max(t, m.duration());
            }
            this.total = t;
        }

        @Override
        public float duration() {
            return total;
        }

        @Override
        public void apply(float t) {
            for (Motion m : parts) {
                m.apply(Math.min(t, m.duration()));
            }
        }

        @Override
        public void reset() {
            for (Motion m : parts) {
                m.reset();
            }
        }
    }

    static final class Delay extends Motion {
        private final float seconds;

        Delay(float seconds) {
            this.seconds = Math.max(0f, seconds);
        }

        @Override
        public float duration() {
            return seconds;
        }

        @Override
        public void apply(float t) {
        }
    }

    static final class Call extends Motion {
        private final Runnable action;
        private boolean fired;

        Call(Runnable action) {
            this.action = action;
        }

        @Override
        public float duration() {
            return 0f;
        }

        @Override
        public void apply(float t) {
            if (!fired) {
                fired = true;
                action.run();
            }
        }

        @Override
        public void reset() {
            fired = false;
        }
    }

    static final class Repeat extends Motion {
        private final Motion inner;
        private final int times;
        private final boolean yoyo;

        Repeat(Motion inner, int times, boolean yoyo) {
            this.inner = inner;
            this.times = times;
            this.yoyo = yoyo;
        }

        @Override
        public float duration() {
            return inner.duration() * times;
        }

        @Override
        public void apply(float t) {
            float d = inner.duration();
            if (d <= 0f) {
                inner.apply(0f);
                return;
            }
            int i = Math.min((int) (t / d), times - 1);
            float local = t - i * d;
            inner.apply(yoyo && (i & 1) == 1 ? d - local : local);
        }

        @Override
        public void reset() {
            inner.reset();
        }
    }
}
