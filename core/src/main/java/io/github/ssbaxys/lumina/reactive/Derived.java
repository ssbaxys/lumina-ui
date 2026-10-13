package io.github.ssbaxys.lumina.reactive;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Производное значение, как {@link Computed}, но «ленивое»: подписывается на свои зависимости,
 * только пока на него самого кто-то подписан. Без подписчиков не удерживается исходными сигналами,
 * поэтому {@code dispose()} не нужен — удобно для значений, привязанных к долгоживущим сигналам
 * (например, к текущему языку).
 */
public final class Derived<T> extends AbstractSignal<T> {
    private final Supplier<T> fn;
    private Computed<T> live;
    private Subscription liveSubscription;
    private int subscribers;

    public Derived(Supplier<T> fn) {
        this.fn = fn;
    }

    @Override
    public T get() {
        if (live == null) {
            // Не активен: считаем напрямую; внешний Computed (если есть) сам отследит исходные сигналы.
            return fn.get();
        }
        track();
        return live.get();
    }

    @Override
    public Subscription subscribe(Consumer<? super T> listener) {
        Subscription inner = super.subscribe(listener);
        if (subscribers++ == 0) {
            live = new Computed<>(fn);
            liveSubscription = live.subscribe(this::fire);
        }
        boolean[] closed = {false};
        return () -> {
            if (closed[0]) {
                return;
            }
            closed[0] = true;
            inner.close();
            if (--subscribers == 0) {
                liveSubscription.close();
                live.dispose();
                liveSubscription = null;
                live = null;
            }
        };
    }
}
