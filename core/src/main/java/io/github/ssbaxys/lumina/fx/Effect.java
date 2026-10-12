package io.github.ssbaxys.lumina.fx;

import io.github.ssbaxys.lumina.render.Canvas;

/**
 * Визуальный эффект в {@link FxLayer}: огонь, взрыв, частицы. Координаты — в системе слоя.
 * Эффект живёт, пока {@link #tick} возвращает {@code true}.
 */
public abstract class Effect {
    private boolean stopped;

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
