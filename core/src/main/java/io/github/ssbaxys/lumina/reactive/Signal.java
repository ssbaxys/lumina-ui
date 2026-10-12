package io.github.ssbaxys.lumina.reactive;

import java.util.Objects;
import java.util.function.UnaryOperator;

/** Изменяемое наблюдаемое значение. Уведомляет только при реальном изменении. */
public final class Signal<T> extends AbstractSignal<T> {
    private T value;

    public Signal(T initial) {
        this.value = initial;
    }

    public static <T> Signal<T> of(T initial) {
        return new Signal<>(initial);
    }

    @Override
    public T get() {
        track();
        return value;
    }

    /** Значение без регистрации зависимости. */
    public T peek() {
        return value;
    }

    public void set(T next) {
        if (!Objects.equals(value, next)) {
            value = next;
            fire(next);
        }
    }

    public void update(UnaryOperator<T> fn) {
        set(fn.apply(value));
    }
}
