package io.github.ssbaxys.lumina.reactive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Производное значение с автоматическим отслеживанием зависимостей: все сигналы, прочитанные
 * внутри функции, становятся зависимостями. Пересчитывается сразу при их изменении.
 * Вызовите {@link #dispose()}, когда он больше не нужен.
 */
public final class Computed<T> extends AbstractSignal<T> {
    private static Computed<?> current;

    private final Supplier<T> fn;
    private final List<Subscription> deps = new ArrayList<>();
    private final Set<AbstractSignal<?>> sources = Collections.newSetFromMap(new IdentityHashMap<>());
    private T value;
    private boolean disposed;

    public Computed(Supplier<T> fn) {
        this.fn = fn;
        this.value = evaluate();
    }

    static Computed<?> current() {
        return current;
    }

    @Override
    public T get() {
        track();
        return value;
    }

    void dependOn(AbstractSignal<?> source) {
        if (source == this || !sources.add(source)) {
            return;
        }
        deps.add(source.subscribe(v -> recompute()));
    }

    private T evaluate() {
        for (Subscription s : deps) {
            s.close();
        }
        deps.clear();
        sources.clear();
        Computed<?> prev = current;
        current = this;
        try {
            return fn.get();
        } finally {
            current = prev;
        }
    }

    private void recompute() {
        if (disposed) {
            return;
        }
        T next = evaluate();
        if (!Objects.equals(next, value)) {
            value = next;
            fire(next);
        }
    }

    public void dispose() {
        disposed = true;
        for (Subscription s : deps) {
            s.close();
        }
        deps.clear();
        sources.clear();
    }
}
