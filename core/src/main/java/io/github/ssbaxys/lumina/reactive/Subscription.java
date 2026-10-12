package io.github.ssbaxys.lumina.reactive;

/** Отписка от сигнала. */
@FunctionalInterface
public interface Subscription extends AutoCloseable {
    @Override
    void close();
}
