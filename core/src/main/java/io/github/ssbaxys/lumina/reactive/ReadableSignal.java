package io.github.ssbaxys.lumina.reactive;

import java.util.function.Consumer;
import java.util.function.Function;

/** Наблюдаемое значение. Однопоточное: используйте только из потока рендера. */
public interface ReadableSignal<T> {
    T get();

    /** Подписка на изменения (без немедленного вызова). */
    Subscription subscribe(Consumer<? super T> listener);

    default <R> Computed<R> map(Function<? super T, ? extends R> mapper) {
        return new Computed<>(() -> mapper.apply(get()));
    }
}
