package io.github.ssbaxys.lumina.hud;

import io.github.ssbaxys.lumina.Lumina;

/**
 * Обратный отсчёт или секундомер по реальным часам ({@code Platform.nanoTime()}): идёт, даже когда его
 * никто не рисует. Показывается виджетом {@link TimerDisplay}; окончание ловится {@link #onFinish} — колбэк
 * вызывается из {@link #poll()}, который дёргает отображающий виджет каждый кадр (или ваш код на тике).
 *
 * <pre>{@code
 * Countdown round = Countdown.seconds(90).onFinish(() -> hud.toast("Время вышло", "")).start();
 * Countdown lap = Countdown.stopwatch().start();
 * }</pre>
 */
public final class Countdown {
    private final boolean stopwatch;
    private float duration;
    /** Накоплено до последней паузы, секунд. */
    private double banked;
    /** Момент последнего запуска (нс) или -1, если стоит. */
    private long startedAt = -1L;
    private boolean finishedFired;
    private Runnable onFinish;

    private Countdown(boolean stopwatch, float duration) {
        this.stopwatch = stopwatch;
        this.duration = Math.max(0f, duration);
    }

    /** Обратный отсчёт на {@code seconds} секунд (ещё не запущен). */
    public static Countdown seconds(float seconds) {
        return new Countdown(false, seconds);
    }

    /** Секундомер: считает вверх без конца. */
    public static Countdown stopwatch() {
        return new Countdown(true, 0f);
    }

    private static long now() {
        return Lumina.isInitialized() ? Lumina.platform().nanoTime() : System.nanoTime();
    }

    // ================================================================ управление

    /** Запустить (или продолжить после паузы). */
    public Countdown start() {
        if (startedAt < 0L && !isFinished()) {
            startedAt = now();
        }
        return this;
    }

    public Countdown pause() {
        if (startedAt >= 0L) {
            banked += (now() - startedAt) / 1e9;
            startedAt = -1L;
        }
        return this;
    }

    /** Пауза ↔ ход. */
    public Countdown toggle() {
        return isRunning() ? pause() : start();
    }

    /** В начало и остановить. */
    public Countdown reset() {
        banked = 0.0;
        startedAt = -1L;
        finishedFired = false;
        return this;
    }

    /** С начала на новую длительность и сразу запустить. */
    public Countdown restart(float seconds) {
        duration = Math.max(0f, seconds);
        reset();
        return start();
    }

    /** Добавить (или отнять) время обратному отсчёту. */
    public Countdown add(float seconds) {
        duration = Math.max(0f, duration + seconds);
        if (!isFinished()) {
            finishedFired = false;
        }
        return this;
    }

    public Countdown onFinish(Runnable callback) {
        this.onFinish = callback;
        return this;
    }

    // ================================================================ состояние

    /** Прошло секунд с запуска (без пауз). */
    public float elapsed() {
        double e = banked + (startedAt >= 0L ? (now() - startedAt) / 1e9 : 0.0);
        return (float) (stopwatch ? e : Math.min(e, duration));
    }

    /** Осталось секунд (у секундомера — прошло). */
    public float remaining() {
        return stopwatch ? elapsed() : Math.max(0f, duration - elapsed());
    }

    /** Значение для показа: остаток у отсчёта, прошедшее у секундомера. */
    public float displayed() {
        return remaining();
    }

    public float duration() {
        return duration;
    }

    /** Доля прошедшего времени 0..1 (у секундомера — 0). */
    public float progress() {
        return stopwatch || duration <= 0f ? 0f : Math.min(1f, elapsed() / duration);
    }

    public boolean isStopwatch() {
        return stopwatch;
    }

    /** Идёт (запущен и не закончился). */
    public boolean isRunning() {
        return startedAt >= 0L && !isFinished();
    }

    public boolean isFinished() {
        return !stopwatch && (finishedFired || (elapsed() >= duration && (startedAt >= 0L || banked > 0.0)));
    }

    /**
     * Проверить окончание: при первом вызове после конца отсчёта останавливает его и вызывает
     * {@link #onFinish}. Возвращает, закончился ли отсчёт.
     */
    public boolean poll() {
        if (!isFinished()) {
            return false;
        }
        if (!finishedFired) {
            finishedFired = true;
            banked = duration;
            startedAt = -1L;
            if (onFinish != null) {
                onFinish.run();
            }
        }
        return true;
    }
}
