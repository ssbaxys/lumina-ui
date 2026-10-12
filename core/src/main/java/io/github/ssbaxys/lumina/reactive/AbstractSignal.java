package io.github.ssbaxys.lumina.reactive;

import java.util.Arrays;
import java.util.function.Consumer;

abstract class AbstractSignal<T> implements ReadableSignal<T> {
    @SuppressWarnings({"unchecked", "rawtypes"})
    private Consumer<? super T>[] listeners = new Consumer[0];

    @Override
    public Subscription subscribe(Consumer<? super T> listener) {
        listeners = Arrays.copyOf(listeners, listeners.length + 1);
        listeners[listeners.length - 1] = listener;
        return () -> unsubscribe(listener);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void unsubscribe(Consumer<? super T> listener) {
        Consumer<? super T>[] ls = listeners;
        for (int i = 0; i < ls.length; i++) {
            if (ls[i] == listener) {
                Consumer<? super T>[] next = new Consumer[ls.length - 1];
                System.arraycopy(ls, 0, next, 0, i);
                System.arraycopy(ls, i + 1, next, i, ls.length - i - 1);
                listeners = next;
                return;
            }
        }
    }

    protected void fire(T value) {
        for (Consumer<? super T> l : listeners) {
            l.accept(value);
        }
    }

    protected boolean hasListeners() {
        return listeners.length > 0;
    }

    protected void track() {
        Computed<?> current = Computed.current();
        if (current != null) {
            current.dependOn(this);
        }
    }
}
