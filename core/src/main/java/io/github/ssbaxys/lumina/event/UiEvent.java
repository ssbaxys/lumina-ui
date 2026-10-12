package io.github.ssbaxys.lumina.event;

import io.github.ssbaxys.lumina.node.UiElement;

/** Базовое событие. Проходит фазу захвата (корень → цель), затем всплытия (цель → корень). */
public abstract class UiEvent {
    private final EventType<?> type;
    private UiElement target;
    private UiElement currentTarget;
    private boolean propagationStopped;
    private boolean consumed;

    protected UiEvent(EventType<?> type) {
        this.type = type;
    }

    public EventType<?> type() {
        return type;
    }

    public UiElement target() {
        return target;
    }

    public UiElement currentTarget() {
        return currentTarget;
    }

    /** Остановить дальнейшее распространение (как stopPropagation в DOM). */
    public void stopPropagation() {
        propagationStopped = true;
    }

    public boolean isPropagationStopped() {
        return propagationStopped;
    }

    /** Пометить событие обработанным: экран не передаст его Minecraft дальше. */
    public void consume() {
        consumed = true;
    }

    public boolean isConsumed() {
        return consumed;
    }

    /** Внутреннее: заполняет диспетчер. */
    public final void route(UiElement target, UiElement currentTarget) {
        this.target = target;
        this.currentTarget = currentTarget;
    }
}
