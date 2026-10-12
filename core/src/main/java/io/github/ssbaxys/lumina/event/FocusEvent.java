package io.github.ssbaxys.lumina.event;

public final class FocusEvent extends UiEvent {
    public static final EventType<FocusEvent> GAINED = EventType.create("focus-gained", false);
    public static final EventType<FocusEvent> LOST = EventType.create("focus-lost", false);

    public FocusEvent(EventType<FocusEvent> type) {
        super(type);
    }
}
