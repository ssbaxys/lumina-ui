package io.github.ssbaxys.lumina.anim;

/**
 * Проигрыватель {@link Motion}: циклы (в том числе бесконечные), «туда-обратно», скорость, пауза,
 * обратный вызов по окончании. Живёт в {@link Animator} только пока играет.
 */
public final class Playback implements Animation {
    public static final int INFINITE = -1;

    private final Motion motion;
    private int loops = 1;
    private boolean yoyo;
    private float speed = 1f;
    private Runnable onFinish;

    private float time;
    private int iteration;
    private boolean running;
    private boolean paused;
    private boolean scheduled;

    public Playback(Motion motion) {
        this.motion = motion;
    }

    public Motion motion() {
        return motion;
    }

    /** Сколько раз проиграть ({@link #INFINITE} — бесконечно). */
    public Playback loops(int loops) {
        this.loops = loops == INFINITE ? INFINITE : Math.max(1, loops);
        return this;
    }

    public Playback loop() {
        return loops(INFINITE);
    }

    /** Чётные проходы — задом наперёд. */
    public Playback yoyo(boolean yoyo) {
        this.yoyo = yoyo;
        return this;
    }

    public Playback speed(float speed) {
        this.speed = speed;
        return this;
    }

    public Playback onFinish(Runnable onFinish) {
        this.onFinish = onFinish;
        return this;
    }

    public Playback start(Animator animator) {
        time = 0f;
        iteration = 0;
        running = true;
        paused = false;
        motion.reset();
        motion.apply(0f);
        if (!scheduled) {
            scheduled = true;
            animator.start(this);
        }
        return this;
    }

    /** Остановить там, где есть (значения остаются как в текущем кадре). */
    public void stop() {
        running = false;
    }

    /** Сразу перейти в конечное состояние и закончить. */
    public void finish() {
        if (running) {
            running = false;
            motion.apply(endTime(lastIterationIndex()));
            if (onFinish != null) {
                onFinish.run();
            }
        }
    }

    public void pause() {
        paused = true;
    }

    public void resume() {
        paused = false;
    }

    public boolean isRunning() {
        return running;
    }

    public boolean isPaused() {
        return paused;
    }

    /** Текущий номер прохода (с 0). */
    public int iteration() {
        return iteration;
    }

    @Override
    public boolean tick(float dt) {
        if (!running) {
            scheduled = false;
            return false;
        }
        if (paused) {
            return true;
        }
        float d = motion.duration();
        if (d <= 0f) {
            motion.apply(0f);
            complete();
            return false;
        }
        time += dt * speed;
        while (time >= d) {
            if (loops != INFINITE && iteration + 1 >= loops) {
                motion.apply(endTime(iteration));
                complete();
                return false;
            }
            motion.apply(endTime(iteration));
            time -= d;
            iteration++;
            motion.reset();
        }
        motion.apply(reversedPass(iteration) ? d - time : time);
        return true;
    }

    private void complete() {
        running = false;
        scheduled = false;
        if (onFinish != null) {
            onFinish.run();
        }
    }

    private boolean reversedPass(int i) {
        return yoyo && (i & 1) == 1;
    }

    private float endTime(int i) {
        return reversedPass(i) ? 0f : motion.duration();
    }

    private int lastIterationIndex() {
        return loops == INFINITE ? iteration : loops - 1;
    }
}
