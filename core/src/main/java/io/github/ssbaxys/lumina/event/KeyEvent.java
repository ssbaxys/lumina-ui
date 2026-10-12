package io.github.ssbaxys.lumina.event;

/** Клавиатура. Приходит элементу в фокусе и всплывает. */
public final class KeyEvent extends UiEvent {
    public static final EventType<KeyEvent> PRESS = EventType.create("key-press", true);
    public static final EventType<KeyEvent> RELEASE = EventType.create("key-release", true);

    private final int key;
    private final int scanCode;
    private final int modifiers;

    public KeyEvent(EventType<KeyEvent> type, int key, int scanCode, int modifiers) {
        super(type);
        this.key = key;
        this.scanCode = scanCode;
        this.modifiers = modifiers;
    }

    public int key() {
        return key;
    }

    public int scanCode() {
        return scanCode;
    }

    public int modifiers() {
        return modifiers;
    }

    public boolean hasModifier(int mod) {
        return (modifiers & mod) != 0;
    }
}
