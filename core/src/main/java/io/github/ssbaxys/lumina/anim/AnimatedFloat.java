package io.github.ssbaxys.lumina.anim;

/**
 * Анимируемое число: твин (длительность + easing) или пружина.
 * Без аллокаций на кадр; в аниматоре находится только пока движется.
 */
public final class AnimatedFloat implements Animation {
    @FunctionalInterface
    public interface Listener {
        void onChange(float value);
    }

    private static final float SPRING_EPSILON = 1e-3f;
    private static final float MAX_SPRING_STEP = 1f / 240f;

    private final Animator animator;
    private float value;
    private float from;
    private float target;
    private float velocity;
    private float elapsed;
    private float duration = 0.2f;
    private Easing easing = Easing.OUT_CUBIC;
    private Spring spring;
    private boolean running;
    private boolean scheduled;
    private Listener listener;

    public AnimatedFloat(Animator animator, float initial) {
        this.animator = animator;
        this.value = initial;
        this.target = initial;
    }

    public AnimatedFloat tween(float durationSeconds, Easing easing) {
        this.duration = durationSeconds;
        this.easing = easing;
        this.spring = null;
        return this;
    }

    public AnimatedFloat spring(Spring spring) {
        this.spring = spring;
        return this;
    }

    public AnimatedFloat listener(Listener listener) {
        this.listener = listener;
        return this;
    }

    public float get() {
        return value;
    }

    public float target() {
        return target;
    }

    public boolean isRunning() {
        return running;
    }

    public void animateTo(float newTarget) {
        if (newTarget == target && (running || value == newTarget)) {
            return;
        }
        target = newTarget;
        from = value;
        elapsed = 0f;
        if (spring == null && duration <= 0f) {
            snapTo(newTarget);
            return;
        }
        running = true;
        if (!scheduled) {
            scheduled = true;
            animator.start(this);
        }
    }

    public void snapTo(float v) {
        running = false;
        velocity = 0f;
        target = v;
        if (value != v) {
            value = v;
            notifyListener();
        }
    }

    @Override
    public boolean tick(float dt) {
        if (!running) {
            scheduled = false;
            return false;
        }
        if (spring != null) {
            float remaining = dt;
            while (remaining > 0f) {
                float h = Math.min(remaining, MAX_SPRING_STEP);
                float force = -spring.stiffness() * (value - target) - spring.damping() * velocity;
                velocity += force / spring.mass() * h;
                value += velocity * h;
                remaining -= h;
            }
            if (Math.abs(value - target) < SPRING_EPSILON && Math.abs(velocity) < SPRING_EPSILON) {
                value = target;
                velocity = 0f;
                running = false;
            }
        } else {
            elapsed += dt;
            float p = Math.min(1f, elapsed / duration);
            value = from + (target - from) * easing.apply(p);
            if (p >= 1f) {
                value = target;
                running = false;
            }
        }
        notifyListener();
        if (!running) {
            scheduled = false;
        }
        return running;
    }

    private void notifyListener() {
        if (listener != null) {
            listener.onChange(value);
        }
    }
}
