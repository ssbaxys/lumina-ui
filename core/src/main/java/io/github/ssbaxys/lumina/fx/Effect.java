package io.github.ssbaxys.lumina.fx;

import io.github.ssbaxys.lumina.render.Canvas;

/**
 * Визуальный эффект в {@link FxLayer}: огонь, взрыв, частицы. Координаты — в системе слоя.
 * Эффект живёт, пока {@link #tick} возвращает {@code true}.
 */
public abstract class Effect {
    private boolean stopped;

    /** Шаг эффекта: {@code time} — секунд с рождения; вернуть {@code false}, чтобы исчезнуть. */
    @FunctionalInterface
    public interface Ticker {
        boolean tick(float dt, float time, FxLayer layer);
    }

    @FunctionalInterface
    public interface Painter {
        void paint(Canvas canvas, float time);
    }

    /**
     * Эффект из двух лямбд, без своего класса:
     * {@code Effect.of((dt, t, l) -> t < 1f, (c, t) -> c.fillCircle(x, y, 20 * t, Brush.solid(col)))}.
     */
    public static Effect of(Ticker ticker, Painter painter) {
        return new Effect() {
            private float time;

            @Override
            public boolean tick(float dt, FxLayer layer) {
                time += dt;
                return ticker.tick(dt, time, layer) && !isStopped();
            }

            @Override
            public void render(Canvas canvas) {
                painter.paint(canvas, time);
            }
        };
    }

    /** Несколько эффектов как один (живёт, пока жив хотя бы один; {@link #stop()} гасит все). */
    public static Effect group(Effect... effects) {
        return new Effect() {
            @Override
            public boolean tick(float dt, FxLayer layer) {
                boolean alive = false;
                for (Effect e : effects) {
                    if (isStopped() && !e.isStopped()) {
                        e.stop();
                    }
                    alive |= e.tick(dt, layer);
                }
                return alive;
            }

            @Override
            public void render(Canvas canvas) {
                for (Effect e : effects) {
                    e.render(canvas);
                }
            }
        };
    }

    /** @return продолжать ли жить */
    public abstract boolean tick(float dt, FxLayer layer);

    public abstract void render(Canvas canvas);

    /** Попросить эффект закончиться (бесконечные эффекты вроде огня гаснут). */
    public void stop() {
        stopped = true;
    }

    public boolean isStopped() {
        return stopped;
    }
}
