package io.github.ssbaxys.lumina.event;

@FunctionalInterface
public interface EventListener<E extends UiEvent> {
    void handle(E event);
}
